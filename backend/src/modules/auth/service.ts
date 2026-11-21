import {
  Prisma,
  type AuthTokenPurpose,
  type PrismaClient,
  type Profile,
  type User,
} from '@prisma/client';
import { OAuth2Client } from 'google-auth-library';
import crypto from 'node:crypto';
import { env } from '../../config/env.js';
import { AppError } from '../../lib/errors.js';
import { normalizeEmail, sendEmail } from '../../lib/email.js';
import { authCodeMatches, generateAuthCode, hashAuthCode } from '../../lib/auth-codes.js';
import { hashPassword, verifyPassword } from '../../lib/password.js';
import { addDays, generateRefreshToken, hashToken, ttlToSeconds } from '../../lib/tokens.js';
import type { LoginInput, RegisterInput, ResetPasswordInput } from './schemas.js';

// Used to keep login timing similar when the email does not exist.
const DUMMY_PASSWORD_HASH = await hashPassword('dummy-password-for-timing-only');

export type PublicUser = {
  id: string;
  email: string;
  role: string;
  emailVerified: boolean;
  createdAt: string;
  profile: {
    displayName: string;
    uiLang: string;
    sourceLang: string;
    targetLang: string;
    cefrLevel: string | null;
    targetBand: number | null;
    examDate: string | null;
    onboardingDone: boolean;
  } | null;
};

export type TokenPair = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

type SignAccess = (payload: { sub: string; role: string }) => string;

export function toPublicUser(user: User & { profile: Profile | null }): PublicUser {
  return {
    id: user.id,
    email: user.email,
    role: user.role,
    emailVerified: user.emailVerified,
    createdAt: user.createdAt.toISOString(),
    profile: user.profile
      ? {
          displayName: user.profile.displayName,
          uiLang: user.profile.uiLang,
          sourceLang: user.profile.sourceLang,
          targetLang: user.profile.targetLang,
          cefrLevel: user.profile.cefrLevel,
          targetBand: user.profile.targetBand === null ? null : Number(user.profile.targetBand),
          examDate: user.profile.examDate ? user.profile.examDate.toISOString().slice(0, 10) : null,
          onboardingDone: user.profile.onboardingDone,
        }
      : null,
  };
}

export class AuthService {
  private readonly googleClient = new OAuth2Client();

  constructor(
    private readonly prisma: PrismaClient,
    private readonly signAccess: SignAccess,
  ) {}

  async register(input: RegisterInput): Promise<{ user: PublicUser } & TokenPair> {
    const email = normalizeEmail(input.email);

    const existing = await this.prisma.user.findUnique({
      where: { email },
      select: { id: true },
    });

    if (existing) {
      throw AppError.conflict('EMAIL_IN_USE', 'This email is already registered');
    }

    const passwordHash = await hashPassword(input.password);

    let user: User & { profile: Profile | null };

    try {
      user = await this.prisma.user.create({
        data: {
          email,
          passwordHash,
          profile: {
            create: { displayName: input.displayName },
          },
          subscription: {
            create: { plan: 'FREE', status: 'ACTIVE' },
          },
        },
        include: { profile: true },
      });
    } catch (error) {
      if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
        throw AppError.conflict('EMAIL_IN_USE', 'This email is already registered');
      }
      throw error;
    }

    const tokens = await this.issueTokens(user);

    // A failed email must not block registration; the user can request a new
    // code from the app.
    try {
      await this.sendVerificationCode(user);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.warn('[auth] could not send the verification email', error);
    }

    return { user: toPublicUser(user), ...tokens };
  }

  async login(input: LoginInput): Promise<{ user: PublicUser } & TokenPair> {
    const email = normalizeEmail(input.email);

    const user = await this.prisma.user.findUnique({
      where: { email },
      include: { profile: true },
    });

    const valid = await verifyPassword(user?.passwordHash ?? DUMMY_PASSWORD_HASH, input.password);

    if (!user || user.deletedAt || !valid) {
      throw AppError.unauthorized('Invalid email or password');
    }

    const tokens = await this.issueTokens(user);
    return { user: toPublicUser(user), ...tokens };
  }

  async loginWithGoogle(idToken: string): Promise<{ user: PublicUser } & TokenPair> {
    let payload;
    try {
      const audiences = [env.GOOGLE_CLIENT_ID, env.GOOGLE_ANDROID_CLIENT_ID].filter(Boolean) as string[];
      const ticket = await this.googleClient.verifyIdToken({
        idToken,
        audience: audiences.length > 0 ? audiences : undefined,
      });
      payload = ticket.getPayload();
    } catch {
      throw AppError.unauthorized('Invalid Google ID token');
    }

    if (!payload || !payload.email) {
      throw AppError.badRequest('INVALID_GOOGLE_TOKEN', 'Google token is invalid or missing email');
    }

    const email = normalizeEmail(payload.email);
    let user = await this.prisma.user.findUnique({
      where: { email },
      include: { profile: true },
    });

    if (user) {
      if (user.deletedAt) {
        throw AppError.unauthorized('Account has been deactivated');
      }
      if (!user.emailVerified) {
        user = await this.prisma.user.update({
          where: { id: user.id },
          data: { emailVerified: true },
          include: { profile: true },
        });
      }
    } else {
      const randomPassword = crypto.randomUUID() + '-GAuth-' + Date.now();
      const passwordHash = await hashPassword(randomPassword);
      const displayName = (payload.name || payload.given_name || email.split('@')[0] || 'User').trim();

      user = await this.prisma.user.create({
        data: {
          email,
          passwordHash,
          emailVerified: true,
          profile: {
            create: {
              displayName: displayName.slice(0, 60),
            },
          },
          subscription: {
            create: { plan: 'FREE', status: 'ACTIVE' },
          },
        },
        include: { profile: true },
      });
    }

    const tokens = await this.issueTokens(user);
    return { user: toPublicUser(user), ...tokens };
  }

  async refresh(rawToken: string): Promise<TokenPair> {
    const tokenHash = hashToken(rawToken);

    const record = await this.prisma.refreshToken.findUnique({
      where: { tokenHash },
      include: { user: true },
    });

    if (
      !record ||
      record.expiresAt.getTime() <= Date.now() ||
      record.user.deletedAt !== null
    ) {
      throw AppError.unauthorized('Invalid refresh token');
    }

    // A rotated token must never be usable twice. If it is replayed, revoke the
    // whole token family so a stolen token cannot keep a session alive.
    if (record.revokedAt !== null) {
      await this.prisma.refreshToken.updateMany({
        where: { userId: record.userId, revokedAt: null },
        data: { revokedAt: new Date() },
      });
      throw AppError.unauthorized('Refresh token reuse detected');
    }

    const nextToken = generateRefreshToken();
    const expiresAt = addDays(new Date(), env.REFRESH_TOKEN_TTL_DAYS);

    const rotated = await this.prisma.$transaction(async (tx) => {
      const claimed = await tx.refreshToken.updateMany({
        where: { id: record.id, revokedAt: null },
        data: { revokedAt: new Date() },
      });

      if (claimed.count !== 1) {
        return false;
      }

      await tx.refreshToken.create({
        data: {
          userId: record.userId,
          tokenHash: hashToken(nextToken),
          expiresAt,
        },
      });

      return true;
    });

    if (!rotated) {
      await this.prisma.refreshToken.updateMany({
        where: { userId: record.userId, revokedAt: null },
        data: { revokedAt: new Date() },
      });
      throw AppError.unauthorized('Refresh token reuse detected');
    }

    return {
      accessToken: this.signAccess({ sub: record.userId, role: record.user.role }),
      refreshToken: nextToken,
      expiresIn: ttlToSeconds(env.ACCESS_TOKEN_TTL),
    };
  }

  async logout(rawToken: string): Promise<void> {
    await this.prisma.refreshToken.updateMany({
      where: { tokenHash: hashToken(rawToken), revokedAt: null },
      data: { revokedAt: new Date() },
    });
  }

  async me(userId: string): Promise<PublicUser> {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      include: { profile: true },
    });

    if (!user || user.deletedAt) {
      throw AppError.unauthorized();
    }

    return toPublicUser(user);
  }

  async requestEmailVerification(
    userId: string,
  ): Promise<{ alreadyVerified: boolean; expiresAt: string | null }> {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user || user.deletedAt) {
      throw AppError.unauthorized();
    }
    if (user.emailVerified) {
      return { alreadyVerified: true, expiresAt: null };
    }

    const { expiresAt } = await this.sendVerificationCode(user);
    return { alreadyVerified: false, expiresAt: expiresAt.toISOString() };
  }

  async confirmEmailVerification(
    userId: string,
    code: string,
  ): Promise<{ emailVerified: boolean }> {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user || user.deletedAt) {
      throw AppError.unauthorized();
    }
    if (user.emailVerified) {
      return { emailVerified: true };
    }

    const record = await this.verifyCode(user.id, 'EMAIL_VERIFY', code);

    await this.prisma.$transaction([
      this.prisma.authToken.update({
        where: { id: record.id },
        data: { consumedAt: new Date() },
      }),
      this.prisma.user.update({
        where: { id: user.id },
        data: { emailVerified: true },
      }),
    ]);

    return { emailVerified: true };
  }

  /**
   * Always resolves: an unknown email must look identical to a known one so the
   * endpoint cannot be used to enumerate accounts.
   */
  async requestPasswordReset(emailInput: string): Promise<void> {
    const email = normalizeEmail(emailInput);
    const user = await this.prisma.user.findUnique({ where: { email } });
    if (!user || user.deletedAt) {
      return;
    }

    const { code } = await this.createCode(user.id, 'PASSWORD_RESET');
    await sendEmail({
      to: email,
      subject: 'Inglish: parolni tiklash',
      text:
        `Parolni tiklash kodi: ${code}\n\n` +
        `Kod ${env.AUTH_CODE_TTL_MINUTES} daqiqa amal qiladi. ` +
        `Agar bu so'rovni siz yubormagan bo'lsangiz, xatni e'tiborsiz qoldiring.`,
    });
  }

  async resetPassword(input: ResetPasswordInput): Promise<void> {
    const email = normalizeEmail(input.email);
    const user = await this.prisma.user.findUnique({ where: { email } });
    if (!user || user.deletedAt) {
      throw AppError.badRequest('CODE_INVALID', "Kod noto'g'ri yoki muddati tugagan");
    }

    const record = await this.verifyCode(user.id, 'PASSWORD_RESET', input.code);
    const passwordHash = await hashPassword(input.password);

    await this.prisma.$transaction([
      this.prisma.authToken.update({
        where: { id: record.id },
        data: { consumedAt: new Date() },
      }),
      this.prisma.user.update({
        where: { id: user.id },
        data: { passwordHash },
      }),
      // A password change invalidates every existing session.
      this.prisma.refreshToken.updateMany({
        where: { userId: user.id, revokedAt: null },
        data: { revokedAt: new Date() },
      }),
    ]);
  }

  private async sendVerificationCode(user: User): Promise<{ expiresAt: Date }> {
    const { code, expiresAt } = await this.createCode(user.id, 'EMAIL_VERIFY');
    await sendEmail({
      to: user.email,
      subject: 'Inglish: emailni tasdiqlang',
      text:
        `Tasdiqlash kodi: ${code}\n\n` +
        `Kod ${env.AUTH_CODE_TTL_MINUTES} daqiqa amal qiladi.`,
    });
    return { expiresAt };
  }

  private async createCode(
    userId: string,
    purpose: AuthTokenPurpose,
  ): Promise<{ code: string; expiresAt: Date }> {
    const code = generateAuthCode();
    const expiresAt = new Date(Date.now() + env.AUTH_CODE_TTL_MINUTES * 60_000);

    // Only one code per purpose may be active, so requesting a new one
    // invalidates the previous code.
    await this.prisma.$transaction([
      this.prisma.authToken.updateMany({
        where: { userId, purpose, consumedAt: null },
        data: { consumedAt: new Date() },
      }),
      this.prisma.authToken.create({
        data: {
          userId,
          purpose,
          codeHash: hashAuthCode(userId, purpose, code),
          expiresAt,
        },
      }),
    ]);

    return { code, expiresAt };
  }

  private async verifyCode(userId: string, purpose: AuthTokenPurpose, code: string) {
    const record = await this.prisma.authToken.findFirst({
      where: { userId, purpose, consumedAt: null },
      orderBy: { createdAt: 'desc' },
    });

    if (!record) {
      throw AppError.badRequest('CODE_INVALID', "Kod topilmadi. Yangi kod so'rang.");
    }
    if (record.expiresAt.getTime() <= Date.now()) {
      throw AppError.badRequest('CODE_EXPIRED', "Kod muddati tugagan. Yangi kod so'rang.");
    }
    if (record.attempts >= env.AUTH_CODE_MAX_ATTEMPTS) {
      await this.prisma.authToken.update({
        where: { id: record.id },
        data: { consumedAt: new Date() },
      });
      throw AppError.badRequest('CODE_LOCKED', "Juda ko'p urinish. Yangi kod so'rang.");
    }

    if (!authCodeMatches(record.codeHash, hashAuthCode(userId, purpose, code))) {
      await this.prisma.authToken.update({
        where: { id: record.id },
        data: { attempts: { increment: 1 } },
      });
      throw AppError.badRequest('CODE_INVALID', "Kod noto'g'ri");
    }

    return record;
  }

  private async issueTokens(user: User): Promise<TokenPair> {
    const refreshToken = generateRefreshToken();
    const expiresAt = addDays(new Date(), env.REFRESH_TOKEN_TTL_DAYS);

    await this.prisma.refreshToken.create({
      data: {
        userId: user.id,
        tokenHash: hashToken(refreshToken),
        expiresAt,
      },
    });

    return {
      accessToken: this.signAccess({ sub: user.id, role: user.role }),
      refreshToken,
      expiresIn: ttlToSeconds(env.ACCESS_TOKEN_TTL),
    };
  }
}
