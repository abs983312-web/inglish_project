import type { FastifyPluginAsync } from 'fastify';
import { z } from 'zod';
import { getActivePlan, getUsage, PLAN_LIMITS } from './limits.js';
import {
  acknowledgePurchase,
  applyVerifiedPurchase,
  assertRtdnAuthorised,
  decodeRtdn,
  handleDeveloperNotification,
  verifyPurchase,
  type ApplyResult,
  type PubSubPushBody,
  type VerifiedPurchase,
} from './verify.js';
import { productCatalog } from './products.js';
import { env } from '../../config/env.js';
import { AppError } from '../../lib/errors.js';
import { parseInput } from '../../lib/validation.js';

const verifySchema = z.object({
  packageName: z.string().min(1).optional(),
  productId: z.string().min(1).optional(),
  purchaseToken: z.string().min(1).max(4096),
});

const restoreSchema = z.object({
  purchases: z
    .array(
      z.object({
        productId: z.string().min(1).optional(),
        purchaseToken: z.string().min(1).max(4096),
      }),
    )
    .min(1)
    .max(50),
});

const pubSubSchema = z.object({
  message: z.object({
    data: z.string().min(1),
    messageId: z.string().optional(),
    publishTime: z.string().optional(),
  }),
  subscription: z.string().optional(),
});

function entitlementFrom(
  plan: 'FREE' | 'PRO' | 'TEACHER',
  subscription: {
    status: string;
    startedAt: Date;
    expiresAt: Date | null;
    provider: string | null;
  } | null,
  autoRenewing: boolean,
) {
  return {
    plan,
    status: subscription?.status ?? 'ACTIVE',
    startedAt: subscription?.startedAt.toISOString() ?? null,
    expiresAt: subscription?.expiresAt?.toISOString() ?? null,
    provider: subscription?.provider ?? null,
    autoRenewing,
    limits: PLAN_LIMITS[plan],
  };
}

export const billingRoutes: FastifyPluginAsync = async (app) => {
  app.get('/billing/products', async () => ({
    products: productCatalog(),
    packageName: env.PLAY_PACKAGE_NAME,
  }));

  app.get('/usage', { preHandler: [app.authenticate] }, async (request) => {
    return { usage: await getUsage(app.prisma, request.user.sub) };
  });

  app.get('/subscription', { preHandler: [app.authenticate] }, async (request) => {
    const userId = request.user.sub;
    const [subscription, plan] = await Promise.all([
      app.prisma.subscription.findUnique({ where: { userId } }),
      getActivePlan(app.prisma, userId),
    ]);

    return {
      subscription: {
        plan,
        status:
          subscription && subscription.expiresAt && subscription.expiresAt.getTime() <= Date.now()
            ? 'EXPIRED'
            : subscription?.status ?? 'ACTIVE',
        startedAt: subscription?.startedAt.toISOString() ?? null,
        expiresAt: subscription?.expiresAt?.toISOString() ?? null,
        provider: subscription?.provider ?? null,
      },
    };
  });

  app.get('/billing/entitlement', { preHandler: [app.authenticate] }, async (request) => {
    const userId = request.user.sub;
    const [subscription, plan, latestPurchase] = await Promise.all([
      app.prisma.subscription.findUnique({ where: { userId } }),
      getActivePlan(app.prisma, userId),
      app.prisma.purchase.findFirst({
        where: { userId },
        orderBy: { createdAt: 'desc' },
      }),
    ]);

    const autoRenewing =
      latestPurchase?.status === 'VERIFIED' &&
      (subscription?.status === 'ACTIVE' || subscription?.status === 'GRACE');

    return {
      entitlement: entitlementFrom(plan, subscription, autoRenewing),
    };
  });

  app.post(
    '/billing/verify',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 20, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      const input = parseInput(verifySchema, request.body, 'Invalid verify request');
      const userId = request.user.sub;
      const packageName = input.packageName ?? env.PLAY_PACKAGE_NAME;

      const verified = await verifyPurchase(packageName, input.purchaseToken);

      if (!verified.acknowledged) {
        await acknowledgePurchase(verified);
        verified.acknowledged = true;
      }

      const result = await applyVerifiedPurchase(app, userId, verified);

      return reply.code(result.isNew ? 201 : 200).send({
        verified: true,
        purchase: {
          productId: verified.productId,
          purchaseToken: verified.purchaseToken,
          orderId: verified.orderId,
          purchaseTime: verified.startTime.toISOString(),
          expiryTime: verified.expiryTime?.toISOString() ?? null,
          autoRenewing: verified.autoRenewing,
          mock: verified.mock,
        },
        entitlement: entitlementFrom(
          result.plan,
          {
            status: result.status,
            startedAt: result.startedAt,
            expiresAt: result.expiresAt,
            provider: verified.mock ? 'MOCK' : 'GOOGLE_PLAY',
          },
          verified.autoRenewing,
        ),
      });
    },
  );

  app.post(
    '/billing/restore',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request) => {
      const input = parseInput(restoreSchema, request.body, 'Invalid restore request');
      const userId = request.user.sub;

      const restored: ApplyResult[] = [];
      const skipped: Array<{ purchaseToken: string; reason: string }> = [];

      for (const item of input.purchases) {
        try {
          const verified: VerifiedPurchase = await verifyPurchase(
            env.PLAY_PACKAGE_NAME,
            item.purchaseToken,
          );
          const result = await applyVerifiedPurchase(app, userId, verified);
          restored.push(result);
        } catch (error) {
          skipped.push({
            purchaseToken: item.purchaseToken.slice(0, 12),
            reason: error instanceof AppError ? error.code : 'VERIFY_FAILED',
          });
        }
      }

      const plan = await getActivePlan(app.prisma, userId);
      const subscription = await app.prisma.subscription.findUnique({ where: { userId } });

      return {
        restored: restored.map((item) => ({
          purchaseId: item.purchaseId,
          plan: item.plan,
          status: item.status,
          expiresAt: item.expiresAt?.toISOString() ?? null,
        })),
        skipped,
        entitlement: entitlementFrom(plan, subscription, plan !== 'FREE'),
      };
    },
  );

  app.post(
    '/billing/rtdn',
    {
      config: { rateLimit: { max: 600, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      await assertRtdnAuthorised(request);

      const parsed = pubSubSchema.safeParse(request.body);
      if (!parsed.success) {
        app.log.warn({ errors: parsed.error.flatten() }, 'Invalid RTDN push body');
        return reply.code(200).send({ ok: true });
      }

      const notification = decodeRtdn(parsed.data as PubSubPushBody);
      if (!notification) {
        app.log.warn('Could not decode RTDN payload');
        return reply.code(200).send({ ok: true });
      }

      await handleDeveloperNotification(app, notification);
      // Always acknowledge: Pub/Sub retries non-2xx responses, and our handler is
      // idempotent, so transient verification errors are logged, not retried.
      return reply.code(200).send({ ok: true });
    },
  );
};
