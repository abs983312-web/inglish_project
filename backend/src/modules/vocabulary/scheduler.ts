import { ReviewState } from '@prisma/client';

export type ReviewRating = 1 | 2 | 3 | 4;

export type ScheduleInput = {
  state: ReviewState;
  stability: number;
  difficulty: number;
  reps: number;
  lapses: number;
  rating: ReviewRating;
  now?: Date;
};

export type ScheduleResult = {
  state: ReviewState;
  stability: number;
  difficulty: number;
  reps: number;
  lapses: number;
  dueAt: Date;
  intervalDays: number;
};

const MINUTE = 60 * 1000;
const DAY = 24 * 60 * MINUTE;
const MAX_INTERVAL_DAYS = 180;

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

/**
 * A small FSRS-inspired scheduler.
 *
 * It is intentionally simple and deterministic so it can be unit-tested and
 * replaced later with a calibrated FSRS implementation. Ratings:
 *   1 = Again, 2 = Hard, 3 = Good, 4 = Easy.
 */
export function scheduleReview(input: ScheduleInput): ScheduleResult {
  const now = input.now ?? new Date();
  const rating = input.rating;

  let state = input.state;
  let stability = input.stability;
  let difficulty = input.difficulty;
  let reps = input.reps;
  let lapses = input.lapses;
  let intervalDays: number;

  if (rating === 1) {
    state = ReviewState.RELEARNING;
    lapses += 1;
    stability = Math.max(0.2, stability > 0 ? stability * 0.4 : 0.2);
    difficulty = clamp(difficulty + 1.2, 1, 10);
    intervalDays = 10 / (24 * 60);
  } else {
    const isFirstReview = reps === 0 || state === ReviewState.NEW;

    if (isFirstReview) {
      stability = rating === 2 ? 0.6 : rating === 3 ? 2 : 4;
      difficulty = clamp(5 + (rating === 2 ? 1 : rating === 4 ? -1 : 0), 1, 10);
    } else {
      const multiplier = rating === 2 ? 1.2 : rating === 3 ? 2.3 : 3.2;
      stability = Math.max(0.5, stability * multiplier);
      difficulty = clamp(
        difficulty + (rating === 2 ? 0.4 : rating === 4 ? -0.3 : 0),
        1,
        10,
      );
    }

    state = rating === 2 ? ReviewState.LEARNING : ReviewState.REVIEW;
    intervalDays = Math.min(MAX_INTERVAL_DAYS, Math.max(rating === 2 ? 1 : 0.5, stability));
  }

  reps += 1;

  return {
    state,
    stability: Math.round(stability * 1000) / 1000,
    difficulty: Math.round(difficulty * 1000) / 1000,
    reps,
    lapses,
    dueAt: new Date(now.getTime() + Math.round(intervalDays * DAY)),
    // Keep enough precision for short relearning intervals (10 minutes).
    intervalDays: Math.round(intervalDays * 10000) / 10000,
  };
}
