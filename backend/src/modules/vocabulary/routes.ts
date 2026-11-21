import type { FastifyPluginAsync } from 'fastify';
import { ReviewState, type UserVocabulary, type VocabularyItem } from '@prisma/client';
import { z } from 'zod';
import { AppError } from '../../lib/errors.js';
import { parseInput } from '../../lib/validation.js';
import { VOCABULARY_SETS } from '../../lib/vocabulary-sets.js';
import { scheduleReview, type ReviewRating } from './scheduler.js';

const dueQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

const listQuerySchema = z.object({
  q: z.string().trim().max(80).optional(),
  tag: z.string().trim().max(60).optional(),
  limit: z.coerce.number().int().min(1).max(200).default(50),
  offset: z.coerce.number().int().min(0).default(0),
});

const reviewParamsSchema = z.object({
  id: z.string().uuid(),
});

const reviewBodySchema = z.object({
  rating: z.number().int().min(1).max(4),
});

function serializeState(state: UserVocabulary | null) {
  if (!state) {
    return {
      state: ReviewState.NEW,
      dueAt: new Date().toISOString(),
      stability: 0,
      difficulty: 0,
      reps: 0,
      lapses: 0,
    };
  }

  return {
    state: state.state,
    dueAt: state.dueAt.toISOString(),
    stability: state.stability,
    difficulty: state.difficulty,
    reps: state.reps,
    lapses: state.lapses,
  };
}

function serializeItem(item: VocabularyItem, state: UserVocabulary | null) {
  return {
    id: item.id,
    word: item.word,
    pos: item.pos,
    cefr: item.cefr,
    definitionUz: item.definitionUz,
    definitionEn: item.definitionEn,
    exampleEn: item.exampleEn,
    exampleUz: item.exampleUz,
    audioUrl: item.audioUrl,
    tags: item.tags,
    state: serializeState(state),
  };
}

export const vocabularyRoutes: FastifyPluginAsync = async (app) => {
  app.get('/vocabulary/due', { preHandler: [app.authenticate] }, async (request) => {
    const { limit = 20 } = parseInput(dueQuerySchema, request.query, 'Invalid query');
    const userId = request.user.sub;

    const dueStates = await app.prisma.userVocabulary.findMany({
      where: {
        userId,
        dueAt: { lte: new Date() },
      },
      orderBy: { dueAt: 'asc' },
      take: limit,
      include: { vocabulary: true },
    });

    const dueItems = dueStates.map((s) => serializeItem(s.vocabulary, s));
    const remaining = limit - dueItems.length;

    let newItems: Array<ReturnType<typeof serializeItem>> = [];
    if (remaining > 0) {
      const items = await app.prisma.vocabularyItem.findMany({
        where: {
          userStates: {
            none: { userId },
          },
        },
        orderBy: [{ cefr: 'asc' }, { word: 'asc' }],
        take: remaining,
      });
      newItems = items.map((item) => serializeItem(item, null));
    }

    return {
      items: [...dueItems, ...newItems],
    };
  });

  app.get('/vocabulary', { preHandler: [app.authenticate] }, async (request) => {
    const { q, tag, limit, offset } = parseInput(listQuerySchema, request.query, 'Invalid query');
    const userId = request.user.sub;

    const where = {
      ...(q
        ? {
            OR: [
              { word: { contains: q, mode: 'insensitive' as const } },
              { definitionUz: { contains: q, mode: 'insensitive' as const } },
              { definitionEn: { contains: q, mode: 'insensitive' as const } },
            ],
          }
        : {}),
      ...(tag ? { tags: { has: tag } } : {}),
    };

    const [items, total, states] = await Promise.all([
      app.prisma.vocabularyItem.findMany({
        where,
        orderBy: [{ cefr: 'asc' }, { word: 'asc' }],
        skip: offset,
        take: limit,
      }),
      app.prisma.vocabularyItem.count({ where }),
      app.prisma.userVocabulary.findMany({ where: { userId } }),
    ]);

    const stateByVocabulary = new Map(states.map((state) => [state.vocabularyId, state]));

    return {
      items: items.map((item) => serializeItem(item, stateByVocabulary.get(item.id) ?? null)),
      total,
      limit,
      offset,
    };
  });

  app.get('/vocabulary/sets', { preHandler: [app.authenticate] }, async () => {
    const counts = await Promise.all(
      VOCABULARY_SETS.map((set) =>
        app.prisma.vocabularyItem.count({ where: { tags: { has: set.tag } } }),
      ),
    );

    return {
      sets: VOCABULARY_SETS.map((set, index) => ({
        slug: set.slug,
        title: set.title,
        description: set.description,
        tag: set.tag,
        count: counts[index] ?? 0,
      })),
    };
  });

  app.get('/vocabulary/stats', { preHandler: [app.authenticate] }, async (request) => {
    const userId = request.user.sub;
    const [total, states] = await Promise.all([
      app.prisma.vocabularyItem.count(),
      app.prisma.userVocabulary.findMany({ where: { userId } }),
    ]);

    const now = Date.now();
    const newCount = Math.max(0, total - states.length);
    const due = states.filter((state) => state.dueAt.getTime() <= now).length + newCount;
    const learning = states.filter(
      (state) => state.state === ReviewState.LEARNING || state.state === ReviewState.RELEARNING,
    ).length;
    const review = states.filter((state) => state.state === ReviewState.REVIEW).length;

    return {
      stats: {
        total,
        new: newCount,
        due,
        learning,
        review,
      },
    };
  });

  app.post('/vocabulary/:id/review', { preHandler: [app.authenticate, app.idempotency] }, async (request) => {
    const { id } = parseInput(reviewParamsSchema, request.params, 'Invalid vocabulary id');
    const { rating } = parseInput(reviewBodySchema, request.body, 'Invalid review body');
    const userId = request.user.sub;

    const item = await app.prisma.vocabularyItem.findUnique({ where: { id } });
    if (!item) {
      throw AppError.notFound('Vocabulary item not found');
    }

    const existing = await app.prisma.userVocabulary.findUnique({
      where: { userId_vocabularyId: { userId, vocabularyId: id } },
    });

    const now = new Date();
    const schedule = scheduleReview({
      state: existing?.state ?? ReviewState.NEW,
      stability: existing?.stability ?? 0,
      difficulty: existing?.difficulty ?? 0,
      reps: existing?.reps ?? 0,
      lapses: existing?.lapses ?? 0,
      rating: rating as ReviewRating,
      now,
    });

    const updated = await app.prisma.userVocabulary.upsert({
      where: { userId_vocabularyId: { userId, vocabularyId: id } },
      update: {
        state: schedule.state,
        dueAt: schedule.dueAt,
        stability: schedule.stability,
        difficulty: schedule.difficulty,
        reps: schedule.reps,
        lapses: schedule.lapses,
        lastReviewedAt: now,
      },
      create: {
        userId,
        vocabularyId: id,
        state: schedule.state,
        dueAt: schedule.dueAt,
        stability: schedule.stability,
        difficulty: schedule.difficulty,
        reps: schedule.reps,
        lapses: schedule.lapses,
        lastReviewedAt: now,
      },
    });

    return {
      review: {
        vocabularyId: updated.vocabularyId,
        state: updated.state,
        dueAt: updated.dueAt.toISOString(),
        stability: updated.stability,
        difficulty: updated.difficulty,
        reps: updated.reps,
        lapses: updated.lapses,
        intervalDays: schedule.intervalDays,
      },
      item: {
        id: item.id,
        word: item.word,
        definitionUz: item.definitionUz,
      },
    };
  });
};
