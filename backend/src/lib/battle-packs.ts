import { Prisma, type BattlePack, type BattlePackQuestion } from '@prisma/client';
import type { FastifyInstance } from 'fastify';
import { AppError } from './errors.js';

/** A pack can be created at most this many times per user per rolling day. */
export const BATTLE_PACK_DAILY_LIMIT = 5;
export const BATTLE_PACK_MAX_QUESTIONS = 50;
export const BATTLE_PACK_MIN_QUESTIONS = 1;
export const BATTLE_PACK_TITLE_MAX = 120;
export const BATTLE_PACK_DESCRIPTION_MAX = 1000;
export const BATTLE_PACK_TAG_MAX = 5;
export const BATTLE_PACK_TAG_LENGTH = 24;
export const BATTLE_PACK_OPTION_MAX = 4;
export const BATTLE_PACK_EXPLANATION_MAX = 600;
export const BATTLE_PACK_TIME_LIMITS_MS = [10_000, 20_000, 30_000] as const;

export type BattlePackQuestionInput = {
  type: 'MCQ' | 'TRUE_FALSE';
  prompt: string;
  options: string[];
  correctIndex: number;
  explanation?: string | null;
  timeLimitMs?: number;
};

export type BattlePackInput = {
  title: string;
  description?: string | null;
  videoUrl: string;
  tags?: string[];
  questions: BattlePackQuestionInput[];
};

type PackWithQuestions = BattlePack & { questions: BattlePackQuestion[] };

const packInclude = {
  questions: { orderBy: { position: 'asc' as const } },
};

/**
 * Accepts every common YouTube URL shape and returns the 11-character video id.
 * We deliberately do not accept arbitrary URLs: the client renders the video in
 * the YouTube app, so only YouTube ids are useful.
 */
export function parseYouTubeUrl(input: string): string | null {
  const value = input.trim();
  if (value.length === 0) return null;

  let url: URL;
  try {
    url = new URL(value.startsWith('http') ? value : `https://${value}`);
  } catch {
    return null;
  }

  const host = url.hostname.replace(/^www\./, '').replace(/^m\./, '');
  if (!['youtube.com', 'youtu.be', 'music.youtube.com'].includes(host)) return null;

  let id: string | null = null;
  if (host === 'youtu.be') {
    id = url.pathname.split('/').filter(Boolean)[0] ?? null;
  } else if (url.pathname === '/watch') {
    id = url.searchParams.get('v');
  } else {
    const parts = url.pathname.split('/').filter(Boolean);
    if (parts[0] === 'embed' || parts[0] === 'shorts' || parts[0] === 'live') {
      id = parts[1] ?? null;
    }
  }

  if (!id || !/^[A-Za-z0-9_-]{11}$/.test(id)) return null;
  return id;
}

export function youTubeWatchUrl(videoId: string): string {
  return `https://www.youtube.com/watch?v=${videoId}`;
}

export function youTubeThumbnailUrl(videoId: string): string {
  return `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
}

export type YouTubeMeta = {
  title: string | null;
  thumbnailUrl: string | null;
};

/**
 * oEmbed needs no API key and gives us the video title + a validity check.
 * Network failures must not block pack creation, so the caller falls back to
 * the user-provided title.
 */
export async function fetchYouTubeMeta(videoId: string): Promise<YouTubeMeta> {
  const fallback: YouTubeMeta = {
    title: null,
    thumbnailUrl: youTubeThumbnailUrl(videoId),
  };

  try {
    const endpoint = `https://www.youtube.com/oembed?format=json&url=${encodeURIComponent(
      youTubeWatchUrl(videoId),
    )}`;
    const response = await fetch(endpoint, {
      signal: AbortSignal.timeout(5_000),
      headers: { accept: 'application/json' },
    });
    if (!response.ok) return fallback;
    const body = (await response.json()) as {
      title?: unknown;
      thumbnail_url?: unknown;
    };
    return {
      title: typeof body.title === 'string' ? body.title.slice(0, 300) : null,
      thumbnailUrl:
        typeof body.thumbnail_url === 'string'
          ? body.thumbnail_url.slice(0, 500)
          : fallback.thumbnailUrl,
    };
  } catch {
    return fallback;
  }
}

export function normalizeTags(tags: string[] | undefined): string[] {
  const seen = new Set<string>();
  const result: string[] = [];
  for (const raw of tags ?? []) {
    const tag = raw.trim().replace(/\s+/g, ' ').slice(0, BATTLE_PACK_TAG_LENGTH);
    if (tag.length < 2) continue;
    const key = tag.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(tag);
    if (result.length >= BATTLE_PACK_TAG_MAX) break;
  }
  return result;
}

function normalizeQuestions(
  questions: BattlePackQuestionInput[],
): Array<{
  type: 'MCQ' | 'TRUE_FALSE';
  prompt: string;
  options: string[];
  correctIndex: number;
  explanation: string | null;
  timeLimitMs: number;
}> {
  return questions.map((question, index) => {
    const position = index + 1;
    const prompt = question.prompt.trim();
    if (prompt.length < 3) {
      throw AppError.badRequest(
        'PACK_QUESTION_INVALID',
        `${position}-savol matni juda qisqa.`,
      );
    }

    const isTrueFalse = question.type === 'TRUE_FALSE';
    const options = isTrueFalse
      ? ['True', 'False']
      : question.options.map((option) => option.trim()).filter((option) => option.length > 0);

    if (options.length < 2 || options.length > BATTLE_PACK_OPTION_MAX) {
      throw AppError.badRequest(
        'PACK_QUESTION_INVALID',
        `${position}-savolda 2 dan ${BATTLE_PACK_OPTION_MAX} tagacha variant bo‘lishi kerak.`,
      );
    }

    const unique = new Set(options.map((option) => option.toLowerCase()));
    if (unique.size !== options.length) {
      throw AppError.badRequest(
        'PACK_QUESTION_INVALID',
        `${position}-savolda bir xil variantlar bor.`,
      );
    }

    if (
      !Number.isInteger(question.correctIndex) ||
      question.correctIndex < 0 ||
      question.correctIndex >= options.length
    ) {
      throw AppError.badRequest(
        'PACK_QUESTION_INVALID',
        `${position}-savolda to‘g‘ri javob belgilanmagan.`,
      );
    }

    const timeLimitMs =
      question.timeLimitMs &&
      (BATTLE_PACK_TIME_LIMITS_MS as readonly number[]).includes(question.timeLimitMs)
        ? question.timeLimitMs
        : 20_000;

    return {
      type: isTrueFalse ? ('TRUE_FALSE' as const) : ('MCQ' as const),
      prompt: prompt.slice(0, 400),
      options,
      correctIndex: question.correctIndex,
      explanation: question.explanation?.trim().slice(0, BATTLE_PACK_EXPLANATION_MAX) || null,
      timeLimitMs,
    };
  });
}

export async function createBattlePack(
  app: FastifyInstance,
  userId: string,
  input: BattlePackInput,
): Promise<ReturnType<typeof serializePackDetail>> {
  const title = input.title.trim();
  if (title.length < 3) {
    throw AppError.badRequest('PACK_INVALID', 'Sarlavha kamida 3 belgidan iborat bo‘lishi kerak.');
  }

  if (input.questions.length < BATTLE_PACK_MIN_QUESTIONS) {
    throw AppError.badRequest('PACK_INVALID', 'Kamida 1 ta savol qo‘shing.');
  }
  if (input.questions.length > BATTLE_PACK_MAX_QUESTIONS) {
    throw AppError.badRequest(
      'PACK_INVALID',
      `Bitta packda maksimum ${BATTLE_PACK_MAX_QUESTIONS} ta savol bo‘lishi mumkin.`,
    );
  }

  const videoId = parseYouTubeUrl(input.videoUrl);
  if (!videoId) {
    throw AppError.badRequest(
      'PACK_VIDEO_INVALID',
      'YouTube havolasini kiriting (masalan https://youtu.be/...).',
    );
  }

  const since = new Date(Date.now() - 24 * 60 * 60 * 1000);
  const createdToday = await app.prisma.battlePack.count({
    where: { creatorId: userId, createdAt: { gte: since } },
  });
  if (createdToday >= BATTLE_PACK_DAILY_LIMIT) {
    throw new AppError(
      429,
      'PACK_DAILY_LIMIT',
      `Kuniga ${BATTLE_PACK_DAILY_LIMIT} ta pack yaratish mumkin. Ertaga qayta urinib ko‘ring.`,
    );
  }

  const questions = normalizeQuestions(input.questions);
  const meta = await fetchYouTubeMeta(videoId);

  const pack = await app.prisma.$transaction(async (tx) => {
    const created = await tx.battlePack.create({
      data: {
        creatorId: userId,
        title: title.slice(0, BATTLE_PACK_TITLE_MAX),
        description:
          input.description?.trim().slice(0, BATTLE_PACK_DESCRIPTION_MAX) || null,
        videoProvider: 'YOUTUBE',
        videoId,
        videoUrl: youTubeWatchUrl(videoId),
        videoTitle: meta.title,
        thumbnailUrl: meta.thumbnailUrl,
        tags: normalizeTags(input.tags),
        questionCount: questions.length,
      },
    });

    await tx.battlePackQuestion.createMany({
      data: questions.map((question, index) => ({
        packId: created.id,
        position: index + 1,
        type: question.type,
        prompt: question.prompt,
        optionsJson: question.options,
        correctIndex: question.correctIndex,
        explanation: question.explanation,
        timeLimitMs: question.timeLimitMs,
      })),
    });

    return tx.battlePack.findUniqueOrThrow({
      where: { id: created.id },
      include: packInclude,
    });
  });

  return serializePackDetail(pack, userId);
}

export async function listBattlePacks(
  app: FastifyInstance,
  userId: string,
  query: {
    scope?: 'mine' | 'popular' | 'new' | 'all';
    search?: string;
    tag?: string;
    offset?: number;
    limit?: number;
  },
): Promise<{ items: unknown[]; total: number; limit: number; offset: number }> {
  const limit = Math.min(Math.max(query.limit ?? 20, 1), 50);
  const offset = Math.max(query.offset ?? 0, 0);
  const scope = query.scope ?? 'popular';

  const where: Prisma.BattlePackWhereInput = {};
  if (scope === 'mine') {
    where.creatorId = userId;
  } else {
    where.OR = [{ isPublic: true }, { creatorId: userId }];
  }
  if (query.search && query.search.trim().length > 0) {
    const search = query.search.trim();
    where.AND = [
      {
        OR: [
          { title: { contains: search, mode: 'insensitive' } },
          { description: { contains: search, mode: 'insensitive' } },
          { tags: { has: search } },
        ],
      },
    ];
  }
  if (query.tag && query.tag.trim().length > 0) {
    where.tags = { has: query.tag.trim() };
  }

  const orderBy: Prisma.BattlePackOrderByWithRelationInput[] =
    scope === 'new'
      ? [{ createdAt: 'desc' }]
      : scope === 'mine'
        ? [{ updatedAt: 'desc' }]
        : [{ playCount: 'desc' }, { createdAt: 'desc' }];

  const [items, total] = await Promise.all([
    app.prisma.battlePack.findMany({
      where,
      orderBy,
      skip: offset,
      take: limit,
      include: { creator: { include: { profile: true } } },
    }),
    app.prisma.battlePack.count({ where }),
  ]);

  return {
    items: items.map((pack) => serializePackSummary(pack, userId)),
    total,
    limit,
    offset,
  };
}

export async function getBattlePack(
  app: FastifyInstance,
  packId: string,
  userId: string,
): Promise<ReturnType<typeof serializePackDetail>> {
  const pack = await app.prisma.battlePack.findUnique({
    where: { id: packId },
    include: packInclude,
  });
  if (!pack) throw AppError.notFound('Pack topilmadi.');
  if (!pack.isPublic && pack.creatorId !== userId) {
    throw AppError.notFound('Pack topilmadi.');
  }
  return serializePackDetail(pack, userId);
}

export async function updateBattlePack(
  app: FastifyInstance,
  packId: string,
  userId: string,
  input: BattlePackInput,
): Promise<ReturnType<typeof serializePackDetail>> {
  const existing = await app.prisma.battlePack.findUnique({
    where: { id: packId },
    select: { creatorId: true },
  });
  if (!existing) throw AppError.notFound('Pack topilmadi.');
  if (existing.creatorId !== userId) {
    throw AppError.forbidden('Faqat pack egasi uni tahrirlay oladi.');
  }

  const title = input.title.trim();
  if (title.length < 3) {
    throw AppError.badRequest('PACK_INVALID', 'Sarlavha kamida 3 belgidan iborat bo‘lishi kerak.');
  }
  if (input.questions.length < BATTLE_PACK_MIN_QUESTIONS) {
    throw AppError.badRequest('PACK_INVALID', 'Kamida 1 ta savol qo‘shing.');
  }
  if (input.questions.length > BATTLE_PACK_MAX_QUESTIONS) {
    throw AppError.badRequest(
      'PACK_INVALID',
      `Bitta packda maksimum ${BATTLE_PACK_MAX_QUESTIONS} ta savol bo‘lishi mumkin.`,
    );
  }

  const videoId = parseYouTubeUrl(input.videoUrl);
  if (!videoId) {
    throw AppError.badRequest('PACK_VIDEO_INVALID', 'YouTube havolasini kiriting.');
  }

  const questions = normalizeQuestions(input.questions);
  const meta = await fetchYouTubeMeta(videoId);

  const pack = await app.prisma.$transaction(async (tx) => {
    await tx.battlePackQuestion.deleteMany({ where: { packId } });
    await tx.battlePack.update({
      where: { id: packId },
      data: {
        title: title.slice(0, BATTLE_PACK_TITLE_MAX),
        description:
          input.description?.trim().slice(0, BATTLE_PACK_DESCRIPTION_MAX) || null,
        videoProvider: 'YOUTUBE',
        videoId,
        videoUrl: youTubeWatchUrl(videoId),
        videoTitle: meta.title,
        thumbnailUrl: meta.thumbnailUrl,
        tags: normalizeTags(input.tags),
        questionCount: questions.length,
      },
    });
    await tx.battlePackQuestion.createMany({
      data: questions.map((question, index) => ({
        packId,
        position: index + 1,
        type: question.type,
        prompt: question.prompt,
        optionsJson: question.options,
        correctIndex: question.correctIndex,
        explanation: question.explanation,
        timeLimitMs: question.timeLimitMs,
      })),
    });
    return tx.battlePack.findUniqueOrThrow({
      where: { id: packId },
      include: packInclude,
    });
  });

  return serializePackDetail(pack, userId);
}

export async function deleteBattlePack(
  app: FastifyInstance,
  packId: string,
  userId: string,
): Promise<{ deleted: boolean }> {
  const existing = await app.prisma.battlePack.findUnique({
    where: { id: packId },
    select: { creatorId: true },
  });
  if (!existing) throw AppError.notFound('Pack topilmadi.');
  if (existing.creatorId !== userId) {
    throw AppError.forbidden('Faqat pack egasi uni o‘chira oladi.');
  }
  await app.prisma.battlePack.delete({ where: { id: packId } });
  return { deleted: true };
}

export async function loadPackForPlay(
  app: FastifyInstance,
  packId: string,
  userId: string,
): Promise<PackWithQuestions> {
  const pack = await app.prisma.battlePack.findUnique({
    where: { id: packId },
    include: packInclude,
  });
  if (!pack) throw AppError.notFound('Pack topilmadi.');
  if (!pack.isPublic && pack.creatorId !== userId) {
    throw AppError.notFound('Pack topilmadi.');
  }
  if (pack.questions.length === 0) {
    throw AppError.conflict('PACK_EMPTY', 'Bu packda savollar yo‘q.');
  }
  return pack;
}

export function serializePackSummary(
  pack: BattlePack & { creator?: { email: string; profile: { displayName: string | null } | null } },
  viewerId: string,
) {
  return {
    id: pack.id,
    title: pack.title,
    description: pack.description,
    videoId: pack.videoId,
    videoUrl: pack.videoUrl,
    videoTitle: pack.videoTitle,
    thumbnailUrl: pack.thumbnailUrl,
    durationSec: pack.durationSec,
    tags: pack.tags,
    questionCount: pack.questionCount,
    playCount: pack.playCount,
    likeCount: pack.likeCount,
    isPublic: pack.isPublic,
    isOwner: pack.creatorId === viewerId,
    creatorName:
      pack.creator?.profile?.displayName?.trim() ||
      pack.creator?.email.split('@')[0] ||
      'Inglish',
    createdAt: pack.createdAt.toISOString(),
    updatedAt: pack.updatedAt.toISOString(),
  };
}

export function serializePackDetail(pack: PackWithQuestions, viewerId: string) {
  const isOwner = pack.creatorId === viewerId;
  return {
    ...serializePackSummary(pack, viewerId),
    questions: pack.questions.map((question) => ({
      id: question.id,
      position: question.position,
      type: question.type,
      prompt: question.prompt,
      options: Array.isArray(question.optionsJson)
        ? (question.optionsJson as Prisma.JsonArray)
            .filter((value): value is string => typeof value === 'string')
        : [],
      // Answer keys are only returned to the pack owner. Players never receive
      // them: the server keeps them in BattleRound and reveals them after the
      // question timer closes.
      correctIndex: isOwner ? question.correctIndex : null,
      explanation: isOwner ? question.explanation : null,
      timeLimitMs: question.timeLimitMs,
    })),
  };
}

export async function incrementPackPlayCount(
  app: FastifyInstance,
  packId: string,
): Promise<void> {
  await app.prisma.battlePack
    .update({ where: { id: packId }, data: { playCount: { increment: 1 } } })
    .catch(() => {
      // A deleted pack must not fail room creation after the transaction.
    });
}
