import type { FastifyInstance, FastifyRequest } from 'fastify';
import { google, type androidpublisher_v3 } from 'googleapis';
import { OAuth2Client } from 'google-auth-library';
import type { SubscriptionStatus } from '@prisma/client';
import { env } from '../../config/env.js';
import { AppError } from '../../lib/errors.js';
import { findProduct, normaliseProductId, planForProduct } from './products.js';

export type PlaySubscriptionState =
  | 'SUBSCRIPTION_STATE_UNSPECIFIED'
  | 'SUBSCRIPTION_STATE_PENDING'
  | 'SUBSCRIPTION_STATE_ACTIVE'
  | 'SUBSCRIPTION_STATE_PAUSED'
  | 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD'
  | 'SUBSCRIPTION_STATE_ON_HOLD'
  | 'SUBSCRIPTION_STATE_CANCELED'
  | 'SUBSCRIPTION_STATE_EXPIRED';

export interface VerifiedPurchase {
  productId: string;
  purchaseToken: string;
  orderId: string | null;
  packageName: string;
  startTime: Date;
  expiryTime: Date | null;
  autoRenewing: boolean;
  acknowledged: boolean;
  state: PlaySubscriptionState;
  mock: boolean;
  raw: unknown;
}

export interface ApplyResult {
  isNew: boolean;
  purchaseId: string;
  plan: 'PRO' | 'TEACHER';
  status: SubscriptionStatus;
  startedAt: Date;
  expiresAt: Date | null;
}

const DAY_MS = 24 * 60 * 60 * 1000;

let androidPublisher: androidpublisher_v3.Androidpublisher | null = null;
let oidcClient: OAuth2Client | null = null;

export function billingMode(): 'mock' | 'google' {
  if (env.BILLING_MODE === 'mock') return 'mock';
  if (env.BILLING_MODE === 'google') return 'google';
  return env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON || env.GOOGLE_PLAY_SERVICE_ACCOUNT_FILE
    ? 'google'
    : 'mock';
}

function getAndroidPublisher(): androidpublisher_v3.Androidpublisher {
  if (androidPublisher) return androidPublisher;

  const credentials = env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
    ? JSON.parse(env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON)
    : undefined;
  const keyFile = env.GOOGLE_PLAY_SERVICE_ACCOUNT_FILE;

  if (!credentials && !keyFile) {
    throw new AppError(
      503,
      'BILLING_NOT_CONFIGURED',
      'Google Play service account is not configured',
    );
  }

  const auth = new google.auth.GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    ...(credentials ? { credentials } : {}),
    ...(keyFile ? { keyFile } : {}),
  });

  androidPublisher = google.androidpublisher({ version: 'v3', auth });
  return androidPublisher;
}

function parseDate(value: string | null | undefined): Date | null {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function stateFromGoogle(raw: string | null | undefined): PlaySubscriptionState {
  switch (raw) {
    case 'SUBSCRIPTION_STATE_PENDING':
    case 'SUBSCRIPTION_STATE_ACTIVE':
    case 'SUBSCRIPTION_STATE_PAUSED':
    case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
    case 'SUBSCRIPTION_STATE_ON_HOLD':
    case 'SUBSCRIPTION_STATE_CANCELED':
    case 'SUBSCRIPTION_STATE_EXPIRED':
      return raw;
    default:
      return 'SUBSCRIPTION_STATE_UNSPECIFIED';
  }
}

export function statusFromState(state: PlaySubscriptionState): SubscriptionStatus {
  switch (state) {
    case 'SUBSCRIPTION_STATE_ACTIVE':
      return 'ACTIVE';
    case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
      return 'GRACE';
    case 'SUBSCRIPTION_STATE_ON_HOLD':
      return 'ON_HOLD';
    case 'SUBSCRIPTION_STATE_CANCELED':
    case 'SUBSCRIPTION_STATE_PAUSED':
    case 'SUBSCRIPTION_STATE_PENDING':
      return 'CANCELED';
    case 'SUBSCRIPTION_STATE_EXPIRED':
      return 'EXPIRED';
    default:
      return 'CANCELED';
  }
}

/**
 * Development-only purchase tokens. Format:
 *   mock:<productId>:<days>[:<nonce>]
 * Example: `mock:inglish_pro_monthly:30:9f3a`
 * The optional nonce makes each mock purchase unique, mirroring real Play
 * tokens (which are unique per purchase).
 */
export function parseMockToken(purchaseToken: string): VerifiedPurchase | null {
  if (!purchaseToken.startsWith('mock:')) return null;
  const [, productId, daysRaw, nonce] = purchaseToken.split(':');
  if (!productId || !findProduct(productId)) return null;

  const days = Number(daysRaw);
  const safeDays = Number.isFinite(days) && days > 0 ? Math.min(days, 3650) : 30;
  const now = new Date();

  return {
    productId: normaliseProductId(productId),
    purchaseToken,
    orderId: `mock-${nonce || now.getTime()}`,
    packageName: env.PLAY_PACKAGE_NAME,
    startTime: now,
    expiryTime: new Date(now.getTime() + safeDays * DAY_MS),
    autoRenewing: true,
    acknowledged: true,
    state: 'SUBSCRIPTION_STATE_ACTIVE',
    mock: true,
    raw: { mock: true, days: safeDays, nonce: nonce ?? null },
  };
}

export async function verifyPurchaseWithGoogle(
  packageName: string,
  purchaseToken: string,
): Promise<VerifiedPurchase> {
  const publisher = getAndroidPublisher();

  let data: androidpublisher_v3.Schema$SubscriptionPurchaseV2;
  try {
    const response = await publisher.purchases.subscriptionsv2.get({
      packageName,
      token: purchaseToken,
    });
    data = response.data;
  } catch (error) {
    const status = (error as { code?: number; response?: { status?: number } }).code ??
      (error as { response?: { status?: number } }).response?.status;
    if (status === 400 || status === 404 || status === 410) {
      throw AppError.badRequest('PURCHASE_NOT_FOUND', 'Google Play does not recognise this purchase');
    }
    throw new AppError(
      502,
      'BILLING_VERIFY_FAILED',
      'Could not verify the purchase with Google Play',
    );
  }

  if (!data) {
    throw AppError.badRequest('PURCHASE_NOT_FOUND', 'Google Play returned an empty purchase');
  }

  const lineItem = data.lineItems?.[0];
  if (!lineItem?.productId) {
    throw AppError.badRequest('PURCHASE_INVALID', 'Purchase has no product line item');
  }

  const product = findProduct(lineItem.productId);
  if (!product) {
    throw AppError.badRequest(
      'PURCHASE_INVALID',
      `Unknown product: ${normaliseProductId(lineItem.productId)}`,
    );
  }

  const startTime = parseDate(data.startTime) ?? new Date();
  const expiryTime = parseDate(lineItem.expiryTime);

  return {
    productId: normaliseProductId(lineItem.productId),
    purchaseToken,
    orderId: lineItem.latestSuccessfulOrderId ?? null,
    packageName,
    startTime,
    expiryTime,
    autoRenewing: lineItem.autoRenewingPlan?.autoRenewEnabled === true,
    acknowledged: data.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',
    state: stateFromGoogle(data.subscriptionState),
    mock: false,
    raw: data,
  };
}

export async function verifyPurchase(
  packageName: string,
  purchaseToken: string,
): Promise<VerifiedPurchase> {
  const mock = parseMockToken(purchaseToken);
  if (mock) {
    if (env.NODE_ENV === 'production') {
      throw AppError.badRequest('PURCHASE_INVALID', 'Mock purchases are disabled');
    }
    return mock;
  }

  if (billingMode() === 'mock') {
    throw AppError.badRequest(
      'PURCHASE_INVALID',
      'Billing is running in mock mode; use a mock:<productId>:<days> token',
    );
  }

  return verifyPurchaseWithGoogle(packageName, purchaseToken);
}

export async function acknowledgePurchase(purchase: VerifiedPurchase): Promise<void> {
  if (purchase.mock) return;
  const publisher = getAndroidPublisher();
  try {
    await publisher.purchases.subscriptions.acknowledge({
      packageName: purchase.packageName,
      subscriptionId: purchase.productId,
      token: purchase.purchaseToken,
    });
  } catch (error) {
    // Acknowledge is idempotent-ish: if Play already acknowledged the token it
    // returns 400/409. Treat those as success so the entitlement still lands.
    const status = (error as { code?: number }).code;
    if (status !== 400 && status !== 409) {
      throw new AppError(
        502,
        'BILLING_ACKNOWLEDGE_FAILED',
        'Could not acknowledge the purchase with Google Play',
      );
    }
  }
}

function fallbackExpiry(productId: string, start: Date): Date {
  const product = findProduct(productId);
  const days = product?.periodDays ?? 30;
  return new Date(start.getTime() + days * DAY_MS);
}

export async function applyVerifiedPurchase(
  app: FastifyInstance,
  userId: string,
  verified: VerifiedPurchase,
): Promise<ApplyResult> {
  const existing = await app.prisma.purchase.findUnique({
    where: { purchaseToken: verified.purchaseToken },
  });

  if (existing && existing.userId !== userId) {
    throw AppError.conflict(
      'PURCHASE_ALREADY_CLAIMED',
      'This purchase token is already linked to another account',
    );
  }

  if (existing && (existing.status === 'REFUNDED' || existing.status === 'EXPIRED')) {
    throw AppError.conflict('PURCHASE_INVALID', 'This purchase was refunded or expired');
  }

  const plan = planForProduct(verified.productId);
  const status = statusFromState(verified.state);
  const startedAt = verified.startTime;
  const expiresAt =
    verified.expiryTime ?? (status === 'ACTIVE' ? fallbackExpiry(verified.productId, startedAt) : null);

  const purchase = existing
    ? await app.prisma.purchase.update({
        where: { id: existing.id },
        data: {
          status: status === 'ACTIVE' ? 'VERIFIED' : status,
          acknowledged: verified.acknowledged || existing.acknowledged,
          expiryTime: expiresAt,
          rawJson: verified.raw as object,
        },
      })
    : await app.prisma.purchase.create({
        data: {
          userId,
          productId: verified.productId,
          purchaseToken: verified.purchaseToken,
          orderId: verified.orderId,
          packageName: verified.packageName,
          purchaseTime: verified.startTime,
          expiryTime: expiresAt,
          status: status === 'ACTIVE' ? 'VERIFIED' : status,
          acknowledged: verified.acknowledged,
          rawJson: verified.raw as object,
        },
      });

  const subscription = await app.prisma.subscription.upsert({
    where: { userId },
    update: {
      plan,
      status,
      startedAt,
      expiresAt,
      provider: verified.mock ? 'MOCK' : 'GOOGLE_PLAY',
      providerRef: verified.purchaseToken,
    },
    create: {
      userId,
      plan,
      status,
      startedAt,
      expiresAt,
      provider: verified.mock ? 'MOCK' : 'GOOGLE_PLAY',
      providerRef: verified.purchaseToken,
    },
  });

  return {
    isNew: !existing,
    purchaseId: purchase.id,
    plan,
    status: subscription.status,
    startedAt: subscription.startedAt,
    expiresAt: subscription.expiresAt,
  };
}

export async function revokeSubscription(
  app: FastifyInstance,
  userId: string,
  reason: string,
): Promise<void> {
  await app.prisma.subscription.upsert({
    where: { userId },
    update: {
      plan: 'FREE',
      status: 'EXPIRED',
      expiresAt: new Date(),
    },
    create: {
      userId,
      plan: 'FREE',
      status: 'EXPIRED',
      expiresAt: new Date(),
    },
  });
  app.log.info({ userId, reason }, 'Subscription revoked');
}

// ---------------------------------------------------------------------------
// RTDN (Real-time Developer Notifications)
// ---------------------------------------------------------------------------

export interface RtdnSubscriptionNotification {
  notificationType: number;
  purchaseToken: string;
  subscriptionId: string;
}

export interface RtdnVoidedPurchaseNotification {
  purchaseToken: string;
  orderId?: string;
  productType?: number;
  refundType?: number;
}

export interface DeveloperNotification {
  packageName?: string;
  eventTimeMillis?: string;
  subscriptionNotification?: RtdnSubscriptionNotification;
  voidedPurchaseNotification?: RtdnVoidedPurchaseNotification;
  testNotification?: { version?: string };
}

export interface PubSubPushBody {
  message?: {
    data?: string;
    messageId?: string;
    publishTime?: string;
  };
  subscription?: string;
}

export function decodeRtdn(body: PubSubPushBody): DeveloperNotification | null {
  const data = body.message?.data;
  if (!data) return null;
  try {
    return JSON.parse(Buffer.from(data, 'base64').toString('utf8')) as DeveloperNotification;
  } catch {
    return null;
  }
}

const RTDN_STATE: Record<number, PlaySubscriptionState> = {
  1: 'SUBSCRIPTION_STATE_ACTIVE', // RECOVERED
  2: 'SUBSCRIPTION_STATE_ACTIVE', // RENEWED
  3: 'SUBSCRIPTION_STATE_CANCELED', // CANCELED
  4: 'SUBSCRIPTION_STATE_ACTIVE', // PURCHASED
  5: 'SUBSCRIPTION_STATE_ON_HOLD', // ON_HOLD
  6: 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD', // IN_GRACE_PERIOD
  7: 'SUBSCRIPTION_STATE_ACTIVE', // RESTARTED
  8: 'SUBSCRIPTION_STATE_ACTIVE', // PRICE_CHANGE_CONFIRMED
  9: 'SUBSCRIPTION_STATE_ACTIVE', // DEFERRED
  10: 'SUBSCRIPTION_STATE_PAUSED', // PAUSED
  11: 'SUBSCRIPTION_STATE_PAUSED', // PAUSE_SCHEDULE_CHANGED
  12: 'SUBSCRIPTION_STATE_EXPIRED', // REVOKED
  13: 'SUBSCRIPTION_STATE_EXPIRED', // EXPIRED
  20: 'SUBSCRIPTION_STATE_CANCELED', // PENDING_PURCHASE_CANCELED
};

export async function handleDeveloperNotification(
  app: FastifyInstance,
  notification: DeveloperNotification,
): Promise<void> {
  if (notification.testNotification) {
    app.log.info('RTDN test notification received');
    return;
  }

  if (notification.voidedPurchaseNotification) {
    const { purchaseToken } = notification.voidedPurchaseNotification;
    const purchase = await app.prisma.purchase.findUnique({ where: { purchaseToken } });
    if (!purchase) {
      app.log.warn({ purchaseToken }, 'RTDN voided purchase not found');
      return;
    }
    await app.prisma.purchase.update({
      where: { id: purchase.id },
      data: { status: 'REFUNDED' },
    });
    await revokeSubscription(app, purchase.userId, 'RTDN voided purchase / refund');
    return;
  }

  const subscriptionNotification = notification.subscriptionNotification;
  if (!subscriptionNotification) {
    app.log.warn('RTDN notification has no supported payload');
    return;
  }

  const { purchaseToken, notificationType, subscriptionId } = subscriptionNotification;
  const purchase = await app.prisma.purchase.findUnique({ where: { purchaseToken } });
  if (!purchase) {
    app.log.warn({ purchaseToken, subscriptionId }, 'RTDN purchase not found');
    return;
  }

  const expectedState = RTDN_STATE[notificationType];

  if (billingMode() === 'google') {
    try {
      const verified = await verifyPurchaseWithGoogle(
        notification.packageName ?? purchase.packageName,
        purchaseToken,
      );
      await applyVerifiedPurchase(app, purchase.userId, verified);
      return;
    } catch (error) {
      app.log.warn(
        { error, purchaseToken, notificationType },
        'RTDN live verification failed; falling back to notification state',
      );
    }
  }

  if (!expectedState) {
    app.log.info({ notificationType }, 'RTDN notification type ignored');
    return;
  }

  const status = statusFromState(expectedState);
  await app.prisma.purchase.update({
    where: { id: purchase.id },
    data: { status: status === 'ACTIVE' ? 'VERIFIED' : status },
  });

  if (status === 'EXPIRED' || status === 'CANCELED') {
    await revokeSubscription(app, purchase.userId, `RTDN notification ${notificationType}`);
  } else {
    await app.prisma.subscription.updateMany({
      where: { userId: purchase.userId },
      data: { status },
    });
  }
}

export async function assertRtdnAuthorised(request: FastifyRequest): Promise<void> {
  const header = request.headers.authorization;
  const bearer = header?.startsWith('Bearer ') ? header.slice('Bearer '.length).trim() : null;

  if (env.PLAY_RTDN_AUDIENCE) {
    if (!bearer) {
      throw AppError.unauthorized('RTDN request is missing an OIDC token');
    }
    oidcClient ??= new OAuth2Client();
    try {
      await oidcClient.verifyIdToken({
        idToken: bearer,
        audience: env.PLAY_RTDN_AUDIENCE,
      });
      return;
    } catch {
      throw AppError.unauthorized('RTDN OIDC token is invalid');
    }
  }

  if (env.PLAY_RTDN_TOKEN) {
    const provided = bearer ?? (request.headers['x-rtdn-token'] as string | undefined);
    if (provided !== env.PLAY_RTDN_TOKEN) {
      throw AppError.unauthorized('RTDN token is invalid');
    }
    return;
  }

  if (env.NODE_ENV === 'production') {
    throw new AppError(
      503,
      'RTDN_NOT_CONFIGURED',
      'RTDN authentication is not configured',
    );
  }
}
