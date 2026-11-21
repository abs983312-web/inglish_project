import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';
import { AppError } from '../../lib/errors.js';
import { parseInput } from '../../lib/validation.js';
import { toPublicUser } from '../auth/service.js';

const examDateSchema = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/u, 'examDate must use YYYY-MM-DD')
  .refine((value) => {
    const date = new Date(`${value}T00:00:00.000Z`);
    return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
  }, 'examDate is not a real calendar date');

const updateProfileSchema = z
  .object({
    displayName: z.string().trim().min(2).max(60).optional(),
    uiLang: z.enum(['uz', 'ru', 'en']).optional(),
    sourceLang: z.string().trim().min(2).max(8).optional(),
    targetLang: z.string().trim().min(2).max(8).optional(),
    cefrLevel: z.enum(['A2', 'B1', 'B2', 'C1', 'C2']).nullable().optional(),
    targetBand: z.number().min(4).max(9).multipleOf(0.5).nullable().optional(),
    examDate: examDateSchema.nullable().optional(),
    onboardingDone: z.boolean().optional(),
  })
  .refine((value) => Object.keys(value).length > 0, {
    message: 'At least one profile field is required',
  });

export const userRoutes: FastifyPluginAsync = async (app) => {
  app.get('/me', { preHandler: [app.authenticate] }, async (request) => {
    const user = await app.prisma.user.findUnique({
      where: { id: request.user.sub },
      include: { profile: true },
    });

    if (!user || user.deletedAt) {
      throw AppError.unauthorized();
    }

    return { user: toPublicUser(user) };
  });

  app.put('/me/profile', { preHandler: [app.authenticate] }, async (request) => {
    const input = parseInput(updateProfileSchema, request.body, 'Invalid profile');
    const userId = request.user.sub;

    const user = await app.prisma.user.findUnique({
      where: { id: userId },
      include: { profile: true },
    });

    if (!user || user.deletedAt) {
      throw AppError.unauthorized();
    }

    const data = {
      ...(input.displayName !== undefined ? { displayName: input.displayName } : {}),
      ...(input.uiLang !== undefined ? { uiLang: input.uiLang } : {}),
      ...(input.sourceLang !== undefined ? { sourceLang: input.sourceLang } : {}),
      ...(input.targetLang !== undefined ? { targetLang: input.targetLang } : {}),
      ...(input.cefrLevel !== undefined ? { cefrLevel: input.cefrLevel } : {}),
      ...(input.targetBand !== undefined ? { targetBand: input.targetBand } : {}),
      ...(input.examDate !== undefined
        ? {
            examDate:
              input.examDate === null ? null : new Date(`${input.examDate}T00:00:00.000Z`),
          }
        : {}),
      ...(input.onboardingDone !== undefined ? { onboardingDone: input.onboardingDone } : {}),
    };

    const profile = await app.prisma.profile.upsert({
      where: { userId },
      update: data,
      create: {
        userId,
        displayName: input.displayName ?? user.email.split('@')[0] ?? 'Learner',
        ...data,
      },
    });

    return { user: toPublicUser({ ...user, profile }) };
  });

  app.delete('/me', { preHandler: [app.authenticate] }, async (request, reply) => {
    const userId = request.user.sub;

    const user = await app.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, deletedAt: true },
    });

    if (!user || user.deletedAt) {
      throw AppError.unauthorized();
    }

    // ZRU-1125 compliance: anonymize PII, revoke tokens, and release original email
    const anonymizedEmail = `deleted_${userId.replace(/-/g, '')}_${Date.now()}@deleted.inglish.uz`;
    const randomHash = `$argon2id$v=19$m=65536,t=3,p=4$deleted$${userId}`;

    await app.prisma.$transaction([
      app.prisma.user.update({
        where: { id: userId },
        data: {
          email: anonymizedEmail,
          passwordHash: randomHash,
          emailVerified: false,
          deletedAt: new Date(),
        },
      }),
      app.prisma.profile.updateMany({
        where: { userId },
        data: {
          displayName: 'O‘chirilgan hisob',
        },
      }),
      app.prisma.refreshToken.updateMany({
        where: { userId, revokedAt: null },
        data: { revokedAt: new Date() },
      }),
      app.prisma.authToken.deleteMany({
        where: { userId },
      }),
    ]);

    return reply.code(204).send();
  });
};
