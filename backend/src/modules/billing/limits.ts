import {
  SubmissionStatus,
  type PrismaClient,
  type SubscriptionPlan,
} from '@prisma/client';
import { AppError } from '../../lib/errors.js';

/**
 * The subset of Prisma used by billing. Both PrismaClient and the transaction
 * client satisfy it, so quota checks can run inside an advisory-locked
 * transaction.
 */
export type BillingClient = Pick<
  PrismaClient,
  'subscription' | 'speakingSubmission' | 'writingSubmission'
>;

const TASHKENT_OFFSET_MS = 5 * 60 * 60 * 1000;
const DAY_MS = 24 * 60 * 60 * 1000;

export const PLAN_LIMITS: Record<
  SubscriptionPlan,
  { speakingPerDay: number; writingPerMonth: number }
> = {
  FREE: { speakingPerDay: 3, writingPerMonth: 1 },
  PRO: { speakingPerDay: 15, writingPerMonth: 4 },
  TEACHER: { speakingPerDay: 30, writingPerMonth: 10 },
};

export type UsageBucket = {
  used: number;
  limit: number;
  remaining: number;
  resetsAt: string;
};

export type UsageSnapshot = {
  plan: SubscriptionPlan;
  speaking: UsageBucket;
  writing: UsageBucket;
};

export type UsageKind = 'speaking' | 'writing';

function shiftToLocal(now: Date): Date {
  return new Date(now.getTime() + TASHKENT_OFFSET_MS);
}

export function startOfTashkentDay(now: Date): Date {
  const local = shiftToLocal(now);
  return new Date(
    Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate()) -
      TASHKENT_OFFSET_MS,
  );
}

export function nextTashkentDay(now: Date): Date {
  return new Date(startOfTashkentDay(now).getTime() + DAY_MS);
}

export function startOfTashkentMonth(now: Date): Date {
  const local = shiftToLocal(now);
  return new Date(
    Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), 1) - TASHKENT_OFFSET_MS,
  );
}

export function nextTashkentMonth(now: Date): Date {
  const local = shiftToLocal(now);
  return new Date(
    Date.UTC(local.getUTCFullYear(), local.getUTCMonth() + 1, 1) - TASHKENT_OFFSET_MS,
  );
}

export async function getActivePlan(
  prisma: BillingClient,
  userId: string,
): Promise<SubscriptionPlan> {
  const subscription = await prisma.subscription.findUnique({ where: { userId } });

  if (
    !subscription ||
    subscription.plan === 'FREE' ||
    subscription.status === 'EXPIRED' ||
    subscription.status === 'ON_HOLD'
  ) {
    return 'FREE';
  }

  // A subscription in its grace period keeps access while Play retries payment.
  if (subscription.status === 'GRACE') {
    return subscription.plan;
  }

  // A canceled subscription stays active only until the paid period ends.
  if (subscription.status === 'CANCELED') {
    if (!subscription.expiresAt || subscription.expiresAt.getTime() <= Date.now()) {
      return 'FREE';
    }
    return subscription.plan;
  }

  if (subscription.expiresAt && subscription.expiresAt.getTime() <= Date.now()) {
    return 'FREE';
  }

  return subscription.plan;
}

export async function getUsage(
  prisma: BillingClient,
  userId: string,
  now: Date = new Date(),
): Promise<UsageSnapshot> {
  const plan = await getActivePlan(prisma, userId);
  const limits = PLAN_LIMITS[plan];

  const dayStart = startOfTashkentDay(now);
  const monthStart = startOfTashkentMonth(now);
  // A scoring request that never finished (crash/timeout) must not consume the
  // user's quota forever. Failed retries are explicitly not charged either.
  const staleScoringBefore = new Date(now.getTime() - 10 * 60 * 1000);
  const countedStatuses = {
    OR: [
      { status: SubmissionStatus.SCORED },
      {
        status: SubmissionStatus.SCORING,
        submittedAt: { gte: staleScoringBefore },
      },
    ],
  };

  const [speakingUsed, writingUsed] = await Promise.all([
    prisma.speakingSubmission.count({
      where: {
        userId,
        submittedAt: { gte: dayStart },
        ...countedStatuses,
      },
    }),
    prisma.writingSubmission.count({
      where: {
        userId,
        submittedAt: { gte: monthStart },
        ...countedStatuses,
      },
    }),
  ]);

  return {
    plan,
    speaking: {
      used: speakingUsed,
      limit: limits.speakingPerDay,
      remaining: Math.max(0, limits.speakingPerDay - speakingUsed),
      resetsAt: nextTashkentDay(now).toISOString(),
    },
    writing: {
      used: writingUsed,
      limit: limits.writingPerMonth,
      remaining: Math.max(0, limits.writingPerMonth - writingUsed),
      resetsAt: nextTashkentMonth(now).toISOString(),
    },
  };
}

export async function assertQuota(
  prisma: BillingClient,
  userId: string,
  kind: UsageKind,
): Promise<UsageSnapshot> {
  const usage = await getUsage(prisma, userId);
  const bucket = usage[kind];

  if (bucket.remaining <= 0) {
    throw new AppError(
      429,
      'QUOTA_EXCEEDED',
      `Your ${kind} limit for the ${usage.plan} plan has been reached`,
      {
        kind,
        plan: usage.plan,
        used: bucket.used,
        limit: bucket.limit,
        resetsAt: bucket.resetsAt,
      },
    );
  }

  return usage;
}
