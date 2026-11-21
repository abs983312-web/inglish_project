import { ReviewState } from '@prisma/client';
import { describe, expect, it } from 'vitest';
import { scheduleReview } from '../src/modules/vocabulary/scheduler.js';
import {
  nextTashkentDay,
  nextTashkentMonth,
  startOfTashkentDay,
  startOfTashkentMonth,
} from '../src/modules/billing/limits.js';

describe('scheduleReview', () => {
  it('schedules a new word for review after a good rating', () => {
    const now = new Date('2026-01-01T00:00:00.000Z');
    const result = scheduleReview({
      state: ReviewState.NEW,
      stability: 0,
      difficulty: 0,
      reps: 0,
      lapses: 0,
      rating: 3,
      now,
    });

    expect(result.state).toBe(ReviewState.REVIEW);
    expect(result.reps).toBe(1);
    expect(result.stability).toBe(2);
    expect(result.intervalDays).toBe(2);
    expect(result.dueAt.toISOString()).toBe('2026-01-03T00:00:00.000Z');
  });

  it('returns a forgotten word to relearning after ten minutes', () => {
    const now = new Date('2026-01-01T00:00:00.000Z');
    const result = scheduleReview({
      state: ReviewState.REVIEW,
      stability: 10,
      difficulty: 5,
      reps: 3,
      lapses: 0,
      rating: 1,
      now,
    });

    expect(result.state).toBe(ReviewState.RELEARNING);
    expect(result.lapses).toBe(1);
    expect(result.intervalDays).toBeCloseTo(10 / (24 * 60), 4);
    expect(result.dueAt.toISOString()).toBe('2026-01-01T00:10:00.000Z');
  });

  it('increases stability more for easy than for good', () => {
    const base = {
      state: ReviewState.REVIEW,
      stability: 4,
      difficulty: 5,
      reps: 2,
      lapses: 0,
      now: new Date('2026-01-01T00:00:00.000Z'),
    } as const;

    const good = scheduleReview({ ...base, rating: 3 });
    const easy = scheduleReview({ ...base, rating: 4 });

    expect(easy.stability).toBeGreaterThan(good.stability);
    expect(easy.difficulty).toBeLessThan(good.difficulty);
  });
});

describe('Tashkent quota periods', () => {
  it('starts the day at midnight Tashkent time', () => {
    // 20:00 UTC is 01:00 on the next day in Tashkent (UTC+5).
    const now = new Date('2026-01-01T20:00:00.000Z');

    expect(startOfTashkentDay(now).toISOString()).toBe('2026-01-01T19:00:00.000Z');
    expect(nextTashkentDay(now).toISOString()).toBe('2026-01-02T19:00:00.000Z');
  });

  it('starts the month at midnight Tashkent time', () => {
    const now = new Date('2026-01-15T12:00:00.000Z');

    expect(startOfTashkentMonth(now).toISOString()).toBe('2025-12-31T19:00:00.000Z');
    expect(nextTashkentMonth(now).toISOString()).toBe('2026-01-31T19:00:00.000Z');
  });
});
