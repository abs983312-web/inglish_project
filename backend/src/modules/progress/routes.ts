import type { FastifyPluginAsync } from 'fastify';
import { ReviewState } from '@prisma/client';

const STREAK_LOOKBACK_DAYS = 400;
const RECENT_BAND_LIMIT = 50;

function dayKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function shiftDay(key: string, delta: number): string {
  const date = new Date(`${key}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() + delta);
  return dayKey(date);
}

/**
 * Counts consecutive days with any recorded activity. A streak stays alive
 * while the most recent activity is today or yesterday, so learners do not
 * lose it before the current day ends.
 */
function countStreak(activeDays: Set<string>, today: string): number {
  let cursor = activeDays.has(today) ? today : shiftDay(today, -1);
  let streak = 0;

  while (activeDays.has(cursor)) {
    streak += 1;
    cursor = shiftDay(cursor, -1);
  }

  return streak;
}

function roundBand(value: number): number {
  return Math.round(value * 2) / 2;
}

function roundTo(value: number, digits: number): number {
  const factor = 10 ** digits;
  return Math.round(value * factor) / factor;
}

type BandRow = {
  id: string;
  bandEstimate: unknown;
  scoredAt: Date | null;
};

function serializeBandHistory(rows: BandRow[], scoredCount: number) {
  const scored = rows.filter((row) => row.bandEstimate !== null && row.scoredAt !== null);
  const bands = scored.map((row) => Number(row.bandEstimate));

  return {
    scoredCount,
    latestBand: bands.length > 0 ? bands[0] : null,
    bestBand: bands.length > 0 ? Math.max(...bands) : null,
    recent: scored.slice(0, 10).map((row) => ({
      id: row.id,
      band: Number(row.bandEstimate),
      scoredAt: (row.scoredAt as Date).toISOString(),
    })),
  };
}

export const progressRoutes: FastifyPluginAsync = async (app) => {
  app.get('/progress', { preHandler: [app.authenticate] }, async (request) => {
    const userId = request.user.sub;
    const now = new Date();
    const since = new Date(now.getTime() - STREAK_LOOKBACK_DAYS * 24 * 60 * 60 * 1000);

    const [
      user,
      attemptDays,
      writingRows,
      speakingRows,
      vocabularyStates,
      vocabularyTotal,
      exercisesAttempted,
      exercisesCorrect,
      writingScoredCount,
      speakingScoredCount,
    ] = await Promise.all([
      app.prisma.user.findUnique({
        where: { id: userId },
        include: { profile: true },
      }),
      app.prisma.exerciseAttempt.findMany({
        where: { userId, createdAt: { gte: since } },
        select: { createdAt: true },
      }),
      app.prisma.writingSubmission.findMany({
        where: { userId, status: 'SCORED', scoredAt: { not: null }, bandEstimate: { not: null } },
        orderBy: { scoredAt: 'desc' },
        take: RECENT_BAND_LIMIT,
        select: { id: true, bandEstimate: true, scoredAt: true },
      }),
      app.prisma.speakingSubmission.findMany({
        where: { userId, status: 'SCORED', scoredAt: { not: null }, bandEstimate: { not: null } },
        orderBy: { scoredAt: 'desc' },
        take: RECENT_BAND_LIMIT,
        select: { id: true, bandEstimate: true, scoredAt: true },
      }),
      app.prisma.userVocabulary.findMany({
        where: { userId },
        select: { state: true, dueAt: true, lastReviewedAt: true },
      }),
      app.prisma.vocabularyItem.count(),
      app.prisma.exerciseAttempt.count({ where: { userId } }),
      app.prisma.exerciseAttempt.count({ where: { userId, isCorrect: true } }),
      app.prisma.writingSubmission.count({
        where: { userId, status: 'SCORED', scoredAt: { not: null }, bandEstimate: { not: null } },
      }),
      app.prisma.speakingSubmission.count({
        where: { userId, status: 'SCORED', scoredAt: { not: null }, bandEstimate: { not: null } },
      }),
    ]);

    const writing = serializeBandHistory(writingRows, writingScoredCount);
    const speaking = serializeBandHistory(speakingRows, speakingScoredCount);

    const latestBands = [writing.latestBand, speaking.latestBand].filter(
      (band): band is number => band !== null,
    );
    const currentBand =
      latestBands.length > 0
        ? roundBand(latestBands.reduce((sum, band) => sum + band, 0) / latestBands.length)
        : null;

    const targetBand = user?.profile?.targetBand
      ? Number(user.profile.targetBand)
      : null;

    const activeDays = new Set<string>();
    for (const attempt of attemptDays) activeDays.add(dayKey(attempt.createdAt));
    for (const row of writingRows) {
      if (row.scoredAt) activeDays.add(dayKey(row.scoredAt));
    }
    for (const row of speakingRows) {
      if (row.scoredAt) activeDays.add(dayKey(row.scoredAt));
    }
    for (const state of vocabularyStates) {
      if (state.lastReviewedAt) activeDays.add(dayKey(state.lastReviewedAt));
    }

    const reviewedWords = vocabularyStates.length;
    const dueWords =
      vocabularyStates.filter((state) => state.dueAt.getTime() <= now.getTime()).length +
      Math.max(0, vocabularyTotal - reviewedWords);

    const daysToExam = user?.profile?.examDate
      ? Math.ceil(
          (user.profile.examDate.getTime() - now.getTime()) / (24 * 60 * 60 * 1000),
        )
      : null;

    return {
      progress: {
        examDate: user?.profile?.examDate
          ? user.profile.examDate.toISOString().slice(0, 10)
          : null,
        daysToExam,
        targetBand,
        currentBand,
        bandGap:
          targetBand !== null && currentBand !== null
            ? roundTo(targetBand - currentBand, 1)
            : null,
        streakDays: countStreak(activeDays, dayKey(now)),
        writing,
        speaking,
        vocabulary: {
          total: vocabularyTotal,
          reviewed: reviewedWords,
          due: dueWords,
          learned: vocabularyStates.filter((state) => state.state === ReviewState.REVIEW).length,
        },
        exercises: {
          attempted: exercisesAttempted,
          correct: exercisesCorrect,
          accuracy:
            exercisesAttempted > 0
              ? roundTo(exercisesCorrect / exercisesAttempted, 3)
              : null,
        },
      },
    };
  });
};
