import { randomInt } from 'node:crypto';
import { Prisma, type BattleMode, type SubscriptionPlan } from '@prisma/client';
import type { FastifyInstance } from 'fastify';
import { getActivePlan } from '../modules/billing/limits.js';
import { AppError } from './errors.js';
import {
  BATTLE_AUTO_BOT_DELAY_MS,
  BATTLE_LOBBY_TTL_MS,
  BATTLE_MAX_PLAYERS,
  BATTLE_REVEAL_MS,
  BATTLE_TIME_LIMIT_MS,
  BATTLE_TIME_LIMITS_MS,
  battlePhase,
  botDecision,
  scoreAnswer,
  type BattlePhase,
} from './battle.js';
import {
  broadcast,
  cancelSchedule,
  closeRoomSockets,
  onlineParticipantIds,
  scheduleRoom,
  withRoomLock,
} from './battle-hub.js';
import { incrementPackPlayCount, loadPackForPlay } from './battle-packs.js';

type Candidate = {
  source: 'EXERCISE' | 'VOCABULARY';
  prompt: string;
  options: string[];
  correctIndex: number;
  explanation: string | null;
};

export type RoomWithRelations = Prisma.BattleRoomGetPayload<{
  include: {
    participants: true;
    rounds: { include: { answers: true } };
    pack: {
      select: {
        id: true;
        title: true;
        videoId: true;
        videoUrl: true;
        thumbnailUrl: true;
        creatorId: true;
      };
    };
  };
}>;

const roomInclude = {
  participants: true,
  rounds: {
    include: { answers: true },
    orderBy: { position: 'asc' },
  },
  pack: {
    select: {
      id: true,
      title: true,
      videoId: true,
      videoUrl: true,
      thumbnailUrl: true,
      creatorId: true,
    },
  },
} satisfies Prisma.BattleRoomInclude;

function asRecord(value: Prisma.JsonValue): Record<string, Prisma.JsonValue> {
  if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, Prisma.JsonValue>;
  }
  return {};
}

function asStringArray(value: Prisma.JsonValue | null): string[] {
  if (!Array.isArray(value)) return [];
  return value
    .filter((item): item is string => typeof item === 'string')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function shuffle<T>(items: T[]): T[] {
  const copy = [...items];
  for (let index = copy.length - 1; index > 0; index -= 1) {
    const swap = Math.floor(Math.random() * (index + 1));
    const current = copy[index];
    const target = copy[swap];
    if (current === undefined || target === undefined) continue;
    copy[index] = target;
    copy[swap] = current;
  }
  return copy;
}

function currentRound(room: RoomWithRelations) {
  return room.rounds.find((round) => round.position === room.currentPosition);
}

function displayNameFor(user: {
  email: string;
  profile: { displayName: string | null } | null;
}): string {
  return user.profile?.displayName?.trim() || user.email.split('@')[0] || 'Player';
}

/** Kahoot-style nickname: 2–20 characters, whitespace collapsed. */
export function sanitizeNickname(value: string | null | undefined): string | null {
  if (!value) return null;
  const trimmed = value.trim().replace(/\s+/g, ' ');
  if (trimmed.length < 2 || trimmed.length > 20) return null;
  return trimmed;
}

export function maxPlayersForPlan(plan: SubscriptionPlan): number {
  return BATTLE_MAX_PLAYERS[plan];
}

export async function generateRoomCode(app: FastifyInstance): Promise<string> {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const code = randomInt(0, 1_000_000).toString().padStart(6, '0');
    const existing = await app.prisma.battleRoom.findUnique({
      where: { code },
      select: { id: true },
    });
    if (!existing) return code;
  }
  throw AppError.conflict(
    'ROOM_CODE_EXHAUSTED',
    'Xona kodi yaratilmadi. Bir ozdan keyin qayta urinib ko‘ring.',
  );
}

let candidateCache: { data: Candidate[]; expiresAt: number } | null = null;

async function buildCandidates(app: FastifyInstance): Promise<Candidate[]> {
  if (candidateCache && Date.now() < candidateCache.expiresAt && candidateCache.data.length > 0) {
    return candidateCache.data;
  }

  const [exercises, vocabulary] = await Promise.all([
    app.prisma.exercise.findMany({
      where: { type: 'MULTIPLE_CHOICE' },
      select: {
        prompt: true,
        optionsJson: true,
        answerJson: true,
        explanation: true,
      },
    }),
    app.prisma.vocabularyItem.findMany({
      select: { word: true, definitionUz: true, exampleEn: true },
    }),
  ]);

  const candidates: Candidate[] = [];

  for (const exercise of exercises) {
    const options = asStringArray(exercise.optionsJson);
    const answer = asRecord(exercise.answerJson);
    const correctIndex = answer.correctIndex;
    if (
      options.length < 2 ||
      typeof correctIndex !== 'number' ||
      !Number.isInteger(correctIndex) ||
      correctIndex < 0 ||
      correctIndex >= options.length
    ) {
      continue;
    }
    candidates.push({
      source: 'EXERCISE',
      prompt: exercise.prompt,
      options,
      correctIndex,
      explanation: exercise.explanation,
    });
  }

  const definitions = [...new Set(vocabulary.map((item) => item.definitionUz.trim()))];

  for (const item of vocabulary) {
    const correct = item.definitionUz.trim();
    if (correct.length === 0) continue;

    const distractors = shuffle(definitions.filter((definition) => definition !== correct)).slice(
      0,
      3,
    );
    if (distractors.length < 3) continue;

    const options = shuffle([correct, ...distractors]);
    const correctIndex = options.indexOf(correct);
    candidates.push({
      source: 'VOCABULARY',
      prompt: `What does “${item.word}” mean?`,
      options,
      correctIndex,
      explanation: item.exampleEn
        ? `“${item.word}” — ${correct}. Example: ${item.exampleEn}`
        : `“${item.word}” — ${correct}.`,
    });
  }

  candidateCache = { data: candidates, expiresAt: Date.now() + 15 * 60 * 1000 };
  return candidates;
}

export function serializeRoom(room: RoomWithRelations, now: Date = new Date()) {
  const online = onlineParticipantIds(room.id);
  const phase: BattlePhase = battlePhase(room.status, room.revealEndsAt, now);
  const round = currentRound(room);
  const roundAnswers = round?.answers ?? [];
  const answeredIds = new Set(roundAnswers.map((answer) => answer.participantId));

  const ranked = room.participants
    .filter((participant) => !participant.leftAt)
    .map((participant) => ({
      id: participant.id,
      name: participant.name,
      isBot: participant.isBot,
      isHost: participant.userId === room.hostId,
      isReady: participant.isReady || participant.isBot,
      isOnline: participant.isBot ? true : online.has(participant.id),
      score: participant.score,
      correctCount: participant.correctCount,
      streak: participant.streak,
      bestStreak: participant.bestStreak,
      answeredCurrent: answeredIds.has(participant.id),
      rank: participant.rank,
    }))
    .sort(
      (left, right) =>
        right.score - left.score ||
        right.correctCount - left.correctCount ||
        left.name.localeCompare(right.name),
    )
    .map((participant, index) => ({
      ...participant,
      rank: room.status === 'FINISHED' ? participant.rank ?? index + 1 : index + 1,
    }));

  const currentQuestion =
    room.status === 'ACTIVE' && round
      ? {
          id: round.id,
          position: round.position,
          source: round.source,
          prompt: round.prompt,
          options: asStringArray(round.optionsJson),
          endsAt: room.roundEndsAt?.toISOString() ?? null,
        }
      : null;

  // The answer key is only serialised during the reveal phase. Before that the
  // client receives prompts and options, never correctIndex or explanation.
  const reveal =
    phase === 'REVEAL' && round
      ? (() => {
          const optionCount = asStringArray(round.optionsJson).length;
          const distribution = Array.from({ length: optionCount }, () => 0);
          for (const answer of roundAnswers) {
            if (answer.answerIndex >= 0 && answer.answerIndex < optionCount) {
              distribution[answer.answerIndex] = (distribution[answer.answerIndex] ?? 0) + 1;
            }
          }
          return {
            roundId: round.id,
            position: round.position,
            correctIndex: round.correctIndex,
            explanation: round.explanation,
            endsAt: room.revealEndsAt?.toISOString() ?? null,
            distribution,
            answers: roundAnswers.map((answer) => ({
              participantId: answer.participantId,
              answerIndex: answer.answerIndex,
              isCorrect: answer.isCorrect,
              responseMs: answer.responseMs,
              scoreGained: answer.scoreGained,
            })),
          };
        })()
      : null;

  const hostParticipant = room.participants.find(
    (participant) => participant.userId === room.hostId,
  );

  return {
    id: room.id,
    code: room.code,
    mode: room.mode,
    status: room.status,
    phase,
    questionCount: room.questionCount,
    currentPosition: room.currentPosition,
    timeLimitMs: room.timeLimitMs,
    revealMs: room.revealMs,
    maxPlayers: room.maxPlayers,
    hostParticipantId: hostParticipant?.id ?? null,
    serverNow: now.getTime(),
    startedAt: room.startedAt?.toISOString() ?? null,
    pack: room.pack
      ? {
          id: room.pack.id,
          title: room.pack.title,
          videoId: room.pack.videoId,
          videoUrl: room.pack.videoUrl,
          thumbnailUrl: room.pack.thumbnailUrl,
        }
      : null,
    scores: ranked,
    currentQuestion,
    reveal,
    finished: room.status === 'FINISHED',
    results: room.status === 'FINISHED' ? ranked : null,
  };
}

export type SerializedRoom = ReturnType<typeof serializeRoom>;

/**
 * Host-only post-game report. Includes the answer key because the game is over;
 * it is never exposed while a room is live.
 */
export function buildRoomReport(room: RoomWithRelations) {
  const rounds = room.rounds
    .slice()
    .sort((left, right) => left.position - right.position)
    .map((round) => {
      const options = asStringArray(round.optionsJson);
      const distribution = Array.from({ length: options.length }, () => 0);
      for (const answer of round.answers) {
        if (answer.answerIndex >= 0 && answer.answerIndex < options.length) {
          distribution[answer.answerIndex] =
            (distribution[answer.answerIndex] ?? 0) + 1;
        }
      }

      return {
        position: round.position,
        prompt: round.prompt,
        options,
        correctIndex: round.correctIndex,
        explanation: round.explanation,
        answeredCount: round.answers.filter((answer) => answer.answerIndex >= 0).length,
        correctCount: round.answers.filter((answer) => answer.isCorrect).length,
        distribution,
        answers: round.answers.map((answer) => ({
          participantId: answer.participantId,
          answerIndex: answer.answerIndex,
          isCorrect: answer.isCorrect,
          responseMs: answer.responseMs,
          scoreGained: answer.scoreGained,
        })),
      };
    });

  const participants = room.participants
    .filter((participant) => !participant.leftAt)
    .map((participant) => ({
      id: participant.id,
      name: participant.name,
      isBot: participant.isBot,
      isHost: participant.userId === room.hostId,
      score: participant.score,
      correctCount: participant.correctCount,
      bestStreak: participant.bestStreak,
      rank: participant.rank,
    }))
    .sort((left, right) => right.score - left.score);

  return {
    roomId: room.id,
    code: room.code,
    mode: room.mode,
    status: room.status,
    questionCount: room.questionCount,
    pack: room.pack
      ? {
          id: room.pack.id,
          title: room.pack.title,
          videoUrl: room.pack.videoUrl,
        }
      : null,
    participants,
    rounds,
  };
}

export async function loadRoomRaw(
  app: FastifyInstance,
  roomId: string,
): Promise<RoomWithRelations | null> {
  return app.prisma.battleRoom.findUnique({
    where: { id: roomId },
    include: roomInclude,
  });
}

async function loadRoomOrThrow(app: FastifyInstance, roomId: string): Promise<RoomWithRelations> {
  const room = await loadRoomRaw(app, roomId);
  if (!room) throw AppError.notFound('Battle room not found');
  return room;
}

function participantFor(room: RoomWithRelations, userId: string) {
  return room.participants.find((participant) => participant.userId === userId);
}

function activeParticipants(room: RoomWithRelations) {
  return room.participants.filter((participant) => !participant.leftAt);
}

function humanParticipants(room: RoomWithRelations) {
  return activeParticipants(room).filter((participant) => !participant.isBot);
}

export function broadcastRoom(room: RoomWithRelations): void {
  broadcast(room.id, { type: 'room', room: serializeRoom(room) });
}

function scheduleTransition(app: FastifyInstance, room: RoomWithRelations): void {
  if (room.status !== 'ACTIVE') {
    cancelSchedule(room.id);
    return;
  }

  const at = room.revealEndsAt ?? room.roundEndsAt;
  if (!at) {
    cancelSchedule(room.id);
    return;
  }

  scheduleRoom(room.id, at, async () => {
    const updated = await syncRoom(app, room.id);
    if (updated) broadcastRoom(updated);
  });
}

async function finalizeRoundLocked(
  app: FastifyInstance,
  room: RoomWithRelations,
): Promise<RoomWithRelations> {
  const round = currentRound(room);
  if (!round) return room;

  const options = asStringArray(round.optionsJson);
  const answeredIds = new Set(round.answers.map((answer) => answer.participantId));
  const now = new Date();
  const missing = activeParticipants(room).filter(
    (participant) => !answeredIds.has(participant.id),
  );

  const createdAnswers: Array<{
    participantId: string;
    answerIndex: number;
    isCorrect: boolean;
    responseMs: number;
    scoreGained: number;
    streak: number;
    bestStreak: number;
  }> = [];

  for (const participant of missing) {
    let isCorrect = false;
    let answerIndex = -1;
    let responseMs = room.timeLimitMs;

    if (participant.isBot) {
      const decision = botDecision(
        room.id,
        round.position,
        options.length,
        round.correctIndex,
      );
      isCorrect = decision.isCorrect;
      answerIndex = decision.answerIndex;
      responseMs = Math.min(decision.responseMs, room.timeLimitMs);
    }

    const streak = isCorrect ? participant.streak + 1 : 0;
    const scoreGained = scoreAnswer({
      isCorrect,
      responseMs,
      streak,
      timeLimitMs: room.timeLimitMs,
    });

    createdAnswers.push({
      participantId: participant.id,
      answerIndex,
      isCorrect,
      responseMs,
      scoreGained,
      streak,
      bestStreak: Math.max(participant.bestStreak, streak),
    });
  }

  const revealEndsAt = new Date(now.getTime() + room.revealMs);

  await app.prisma.$transaction(async (tx) => {
    if (createdAnswers.length > 0) {
      await tx.battleAnswer.createMany({
        data: createdAnswers.map((answer) => ({
          roundId: round.id,
          participantId: answer.participantId,
          answerIndex: answer.answerIndex,
          isCorrect: answer.isCorrect,
          responseMs: answer.responseMs,
          scoreGained: answer.scoreGained,
        })),
        skipDuplicates: true,
      });

      for (const answer of createdAnswers) {
        await tx.battleParticipant.update({
          where: { id: answer.participantId },
          data: {
            score: { increment: answer.scoreGained },
            correctCount: { increment: answer.isCorrect ? 1 : 0 },
            streak: answer.streak,
            bestStreak: answer.bestStreak,
          },
        });
      }
    }

    await tx.battleRoom.update({
      where: { id: room.id },
      data: { revealEndsAt },
    });
  });

  return loadRoomOrThrow(app, room.id);
}

async function advanceAfterRevealLocked(
  app: FastifyInstance,
  room: RoomWithRelations,
): Promise<RoomWithRelations> {
  const now = new Date();

  if (room.currentPosition >= room.questionCount) {
    const ranked = [...activeParticipants(room)].sort(
      (left, right) =>
        right.score - left.score ||
        right.correctCount - left.correctCount ||
        left.name.localeCompare(right.name),
    );

    await app.prisma.$transaction(async (tx) => {
      for (const [index, participant] of ranked.entries()) {
        await tx.battleParticipant.update({
          where: { id: participant.id },
          data: { rank: index + 1 },
        });
      }
      await tx.battleRoom.update({
        where: { id: room.id },
        data: {
          status: 'FINISHED',
          currentPosition: room.questionCount,
          finishedAt: now,
          roundEndsAt: null,
          revealEndsAt: null,
        },
      });
    });

    return loadRoomOrThrow(app, room.id);
  }

  const nextPosition = room.currentPosition + 1;
  const nextRound = room.rounds.find((round) => round.position === nextPosition);

  await app.prisma.$transaction(async (tx) => {
    await tx.battleRoom.update({
      where: { id: room.id },
      data: {
        currentPosition: nextPosition,
        roundEndsAt: new Date(now.getTime() + room.timeLimitMs),
        revealEndsAt: null,
      },
    });
    if (nextRound) {
      await tx.battleRound.update({
        where: { id: nextRound.id },
        data: { startedAt: now },
      });
    }
  });

  return loadRoomOrThrow(app, room.id);
}

/**
 * Applies every timer that is already due. Called before any read/write so a
 * restarted server (or a sleeping phone) catches up without a background cron.
 */
async function advanceDueLocked(
  app: FastifyInstance,
  roomId: string,
): Promise<{ room: RoomWithRelations | null; changed: boolean }> {
  let changed = false;

  for (let step = 0; step < 4; step += 1) {
    const room = await loadRoomRaw(app, roomId);
    if (!room || room.status !== 'ACTIVE') return { room, changed };

    const now = new Date();

    if (room.revealEndsAt && now.getTime() >= room.revealEndsAt.getTime()) {
      await advanceAfterRevealLocked(app, room);
      changed = true;
      continue;
    }

    if (!room.revealEndsAt && room.roundEndsAt && now.getTime() >= room.roundEndsAt.getTime()) {
      await finalizeRoundLocked(app, room);
      changed = true;
      continue;
    }

    return { room, changed };
  }

  return { room: await loadRoomRaw(app, roomId), changed };
}

export async function syncRoom(
  app: FastifyInstance,
  roomId: string,
): Promise<RoomWithRelations | null> {
  const room = await withRoomLock(roomId, async () => {
    const result = await advanceDueLocked(app, roomId);
    return result.room;
  });

  if (room?.status === 'ACTIVE') {
    scheduleTransition(app, room);
  } else if (room?.status === 'FINISHED') {
    cancelSchedule(room.id);
  }
  // Lobby rooms may still own an auto-bot timer; leave it alone.

  return room;
}

export async function getRoomForUser(
  app: FastifyInstance,
  roomId: string,
  userId: string,
): Promise<{ room: SerializedRoom; participantId: string }> {
  const synced = await syncRoom(app, roomId);
  const room = synced ?? (await loadRoomRaw(app, roomId));
  if (!room) throw AppError.notFound('Battle room not found');

  const participant = participantFor(room, userId);
  if (!participant) throw AppError.notFound('Battle room not found');

  return {
    room: serializeRoom(room),
    participantId: participant.id,
  };
}

export type CreateBattleRoomInput = {
  mode: BattleMode;
  questionCount: number;
  timeLimitMs: number;
  autoBot: boolean;
};

export async function createBattleRoom(
  app: FastifyInstance,
  userId: string,
  input: CreateBattleRoomInput,
): Promise<{ room: SerializedRoom; participantId: string }> {
  const user = await app.prisma.user.findUnique({
    where: { id: userId },
    include: { profile: true },
  });
  if (!user) throw AppError.unauthorized();

  const [candidates, plan] = await Promise.all([
    buildCandidates(app),
    getActivePlan(app.prisma, userId),
  ]);
  const selected = shuffle(candidates).slice(
    0,
    Math.min(input.questionCount, candidates.length),
  );

  if (selected.length < 5) {
    throw AppError.conflict(
      'NOT_ENOUGH_QUESTIONS',
      'Battle uchun savollar yetarli emas. Keyinroq qayta urinib ko‘ring.',
    );
  }

  const isBotMode = input.mode === 'BOT';
  const code = isBotMode ? null : await generateRoomCode(app);
  const now = new Date();
  const timeLimitMs = input.timeLimitMs;

  const roomId = await app.prisma.$transaction(async (tx) => {
    const room = await tx.battleRoom.create({
      data: {
        hostId: userId,
        code,
        mode: input.mode,
        status: isBotMode ? 'ACTIVE' : 'LOBBY',
        questionCount: selected.length,
        timeLimitMs,
        revealMs: BATTLE_REVEAL_MS,
        maxPlayers: maxPlayersForPlan(plan),
        roundEndsAt: isBotMode ? new Date(now.getTime() + timeLimitMs) : null,
        startedAt: isBotMode ? now : null,
      },
    });

    const host = await tx.battleParticipant.create({
      data: {
        roomId: room.id,
        userId,
        name: displayNameFor(user),
        isReady: true,
        lastSeenAt: now,
      },
    });

    if (isBotMode) {
      await tx.battleParticipant.create({
        data: {
          roomId: room.id,
          name: 'AI Bot',
          isBot: true,
          isReady: true,
          lastSeenAt: now,
        },
      });
    }

    await tx.battleRound.createMany({
      data: selected.map((question, index) => ({
        roomId: room.id,
        position: index + 1,
        source: question.source,
        prompt: question.prompt,
        optionsJson: question.options,
        correctIndex: question.correctIndex,
        explanation: question.explanation,
        startedAt: isBotMode && index === 0 ? now : now,
      })),
    });

    return { roomId: room.id, participantId: host.id };
  });

  const room = await loadRoomOrThrow(app, roomId.roomId);

  if (isBotMode) {
    scheduleTransition(app, room);
  } else if (input.autoBot && input.mode === 'FRIEND') {
    scheduleRoom(room.id, new Date(now.getTime() + BATTLE_AUTO_BOT_DELAY_MS), async () => {
      await autoFillBot(app, room.id);
    });
  }

  return {
    room: serializeRoom(room),
    participantId: roomId.participantId,
  };
}

export type PlayPackInput = {
  mode: 'FRIEND' | 'CLASSROOM';
  shuffle: boolean;
};

/**
 * Creates a Kahoot-style lobby from a user-generated pack. Questions are copied
 * into BattleRound rows so the room is a snapshot: editing or deleting the pack
 * never mutates a running game.
 */
export async function createBattleRoomFromPack(
  app: FastifyInstance,
  userId: string,
  packId: string,
  input: PlayPackInput,
): Promise<{ room: SerializedRoom; participantId: string }> {
  const user = await app.prisma.user.findUnique({
    where: { id: userId },
    include: { profile: true },
  });
  if (!user) throw AppError.unauthorized();

  const pack = await loadPackForPlay(app, packId, userId);
  const questions = input.shuffle ? shuffle(pack.questions) : pack.questions;
  if (questions.length === 0) {
    throw AppError.conflict('PACK_EMPTY', 'Bu packda savollar yo‘q.');
  }

  const code = await generateRoomCode(app);
  const now = new Date();
  const mode: BattleMode = input.mode === 'CLASSROOM' ? 'CLASSROOM' : 'FRIEND';
  const timeLimitMs = questions[0]?.timeLimitMs ?? BATTLE_TIME_LIMIT_MS;

  const created = await app.prisma.$transaction(async (tx) => {
    const room = await tx.battleRoom.create({
      data: {
        hostId: userId,
        packId,
        code,
        mode,
        status: 'LOBBY',
        questionCount: questions.length,
        timeLimitMs,
        revealMs: BATTLE_REVEAL_MS,
        // User-generated packs are free for everyone, so every room gets the
        // classroom ceiling instead of a subscription-gated player limit.
        maxPlayers: 30,
        roundEndsAt: null,
        startedAt: null,
      },
    });

    const host = await tx.battleParticipant.create({
      data: {
        roomId: room.id,
        userId,
        name: displayNameFor(user),
        isReady: true,
        lastSeenAt: now,
      },
    });

    await tx.battleRound.createMany({
      data: questions.map((question, index) => ({
        roomId: room.id,
        position: index + 1,
        source: 'PACK',
        prompt: question.prompt,
        optionsJson: question.optionsJson as Prisma.InputJsonValue,
        correctIndex: question.correctIndex,
        explanation: question.explanation,
        startedAt: now,
      })),
    });

    return { roomId: room.id, participantId: host.id };
  });

  await incrementPackPlayCount(app, packId);

  const room = await loadRoomOrThrow(app, created.roomId);
  broadcastRoom(room);

  return {
    room: serializeRoom(room),
    participantId: created.participantId,
  };
}

async function autoFillBot(app: FastifyInstance, roomId: string): Promise<void> {
  const updated = await withRoomLock(roomId, async () => {
    const room = await loadRoomRaw(app, roomId);
    if (!room || room.status !== 'LOBBY') return null;
    if (humanParticipants(room).length !== 1) return null;
    if (room.participants.some((participant) => participant.isBot)) return null;
    if (activeParticipants(room).length >= room.maxPlayers) return null;

    await app.prisma.battleParticipant.create({
      data: {
        roomId,
        name: 'AI Bot',
        isBot: true,
        isReady: true,
        lastSeenAt: new Date(),
      },
    });

    return loadRoomRaw(app, roomId);
  });

  if (updated) broadcastRoom(updated);
}

export async function joinBattleRoom(
  app: FastifyInstance,
  userId: string,
  code: string,
  nickname?: string | null,
): Promise<{ room: SerializedRoom; participantId: string }> {
  const user = await app.prisma.user.findUnique({
    where: { id: userId },
    include: { profile: true },
  });
  if (!user) throw AppError.unauthorized();

  const safeNickname = sanitizeNickname(nickname);

  // Resolve the code outside the lock, then serialize with start/leave/bot
  // changes on the room id. A code-only lock would race with startBattleRoom.
  const lookup = await app.prisma.battleRoom.findUnique({
    where: { code },
    select: { id: true, mode: true },
  });
  if (!lookup || lookup.mode === 'BOT') {
    throw AppError.notFound('Bunday kodli xona topilmadi.');
  }

  const joined = await withRoomLock(lookup.id, async () => {
    const room = await app.prisma.battleRoom.findUnique({
      where: { id: lookup.id },
      include: roomInclude,
    });

    if (!room || room.code !== code || room.mode === 'BOT') {
      throw AppError.notFound('Bunday kodli xona topilmadi.');
    }

    if (room.status === 'FINISHED') {
      throw AppError.conflict('ROOM_FINISHED', 'Bu xonadagi o‘yin allaqachon yakunlangan.');
    }

    const existing = participantFor(room, userId);
    if (existing) {
      if (existing.leftAt || (safeNickname && existing.name !== safeNickname)) {
        await app.prisma.battleParticipant.update({
          where: { id: existing.id },
          data: {
            leftAt: null,
            lastSeenAt: new Date(),
            ...(safeNickname ? { name: safeNickname } : {}),
          },
        });
      }
      const refreshed = await loadRoomOrThrow(app, room.id);
      return { room: refreshed, participantId: existing.id };
    }

    if (room.status !== 'LOBBY') {
      throw AppError.conflict(
        'ROOM_ALREADY_STARTED',
        'Bu xonada o‘yin allaqachon boshlangan.',
      );
    }

    if (Date.now() - room.createdAt.getTime() > BATTLE_LOBBY_TTL_MS) {
      throw AppError.conflict('ROOM_EXPIRED', 'Xona muddati tugagan. Yangi xona yarating.');
    }

    if (activeParticipants(room).length >= room.maxPlayers) {
      throw AppError.conflict(
        'ROOM_FULL',
        `Xona to‘lgan (maksimum ${room.maxPlayers} o‘yinchi).`,
      );
    }

    const participant = await app.prisma.battleParticipant.create({
      data: {
        roomId: room.id,
        userId,
        name: safeNickname ?? displayNameFor(user),
        isReady: false,
        lastSeenAt: new Date(),
      },
    });

    return {
      room: await loadRoomOrThrow(app, room.id),
      participantId: participant.id,
    };
  });

  broadcastRoom(joined.room);
  return { room: serializeRoom(joined.room), participantId: joined.participantId };
}

export async function setBattleNickname(
  app: FastifyInstance,
  roomId: string,
  userId: string,
  nickname: string,
): Promise<SerializedRoom> {
  const safe = sanitizeNickname(nickname);
  if (!safe) {
    throw AppError.badRequest(
      'NICKNAME_INVALID',
      'Taxallus 2–20 belgidan iborat bo‘lishi kerak.',
    );
  }

  const room = await withRoomLock(roomId, async () => {
    const current = await loadRoomOrThrow(app, roomId);
    const participant = participantFor(current, userId);
    if (!participant) throw AppError.notFound('Battle room not found');
    if (current.status !== 'LOBBY') {
      throw AppError.conflict('ROOM_ALREADY_STARTED', 'O‘yin allaqachon boshlangan.');
    }

    await app.prisma.battleParticipant.update({
      where: { id: participant.id },
      data: { name: safe, lastSeenAt: new Date() },
    });

    return loadRoomOrThrow(app, roomId);
  });

  broadcastRoom(room);
  return serializeRoom(room);
}

export async function setBattleReady(
  app: FastifyInstance,
  roomId: string,
  userId: string,
  ready: boolean,
): Promise<SerializedRoom> {
  const room = await withRoomLock(roomId, async () => {
    const current = await loadRoomOrThrow(app, roomId);
    const participant = participantFor(current, userId);
    if (!participant) throw AppError.notFound('Battle room not found');
    if (current.status !== 'LOBBY') {
      throw AppError.conflict('ROOM_ALREADY_STARTED', 'O‘yin allaqachon boshlangan.');
    }

    await app.prisma.battleParticipant.update({
      where: { id: participant.id },
      data: { isReady: ready, lastSeenAt: new Date() },
    });

    return loadRoomOrThrow(app, roomId);
  });

  broadcastRoom(room);
  return serializeRoom(room);
}

export async function addBattleBot(
  app: FastifyInstance,
  roomId: string,
  userId: string,
): Promise<SerializedRoom> {
  const room = await withRoomLock(roomId, async () => {
    const current = await loadRoomOrThrow(app, roomId);
    if (current.hostId !== userId) {
      throw AppError.forbidden('Faqat xona egasi bot qo‘sha oladi.');
    }
    if (current.status !== 'LOBBY') {
      throw AppError.conflict('ROOM_ALREADY_STARTED', 'O‘yin allaqachon boshlangan.');
    }
    if (activeParticipants(current).length >= current.maxPlayers) {
      throw AppError.conflict('ROOM_FULL', 'Xonada bo‘sh joy yo‘q.');
    }

    const botCount = current.participants.filter((participant) => participant.isBot).length;
    await app.prisma.battleParticipant.create({
      data: {
        roomId,
        name: botCount === 0 ? 'AI Bot' : `AI Bot ${botCount + 1}`,
        isBot: true,
        isReady: true,
        lastSeenAt: new Date(),
      },
    });

    return loadRoomOrThrow(app, roomId);
  });

  broadcastRoom(room);
  return serializeRoom(room);
}

export async function startBattleRoom(
  app: FastifyInstance,
  roomId: string,
  userId: string,
): Promise<SerializedRoom> {
  const room = await withRoomLock(roomId, async () => {
    const current = await loadRoomOrThrow(app, roomId);
    if (current.hostId !== userId) {
      throw AppError.forbidden('Faqat xona egasi o‘yinni boshlay oladi.');
    }
    if (current.status !== 'LOBBY') {
      throw AppError.conflict('ROOM_ALREADY_STARTED', 'O‘yin allaqachon boshlangan.');
    }

    // A user-generated pack room is also a solo practice mode: the host may
    // start alone. Friend/classroom quick-play rooms still need two players.
    const isPackRoom = current.packId !== null;
    const minimumPlayers = isPackRoom ? 1 : 2;
    if (activeParticipants(current).length < minimumPlayers) {
      throw AppError.conflict(
        'NOT_ENOUGH_PLAYERS',
        `O‘yinni boshlash uchun kamida ${minimumPlayers} o‘yinchi kerak.`,
      );
    }

    if (!isPackRoom) {
      const notReady = humanParticipants(current).filter(
        (participant) => participant.userId !== current.hostId && !participant.isReady,
      );
      if (notReady.length > 0) {
        throw AppError.conflict(
          'PLAYERS_NOT_READY',
          'Barcha o‘yinchilar tayyor bo‘lishini kuting.',
        );
      }
    }

    const now = new Date();
    const firstRound = current.rounds.find((round) => round.position === 1);

    await app.prisma.$transaction(async (tx) => {
      await tx.battleRoom.update({
        where: { id: roomId },
        data: {
          status: 'ACTIVE',
          currentPosition: 1,
          startedAt: now,
          roundEndsAt: new Date(now.getTime() + current.timeLimitMs),
          revealEndsAt: null,
        },
      });
      if (firstRound) {
        await tx.battleRound.update({
          where: { id: firstRound.id },
          data: { startedAt: now },
        });
      }
    });

    return loadRoomOrThrow(app, roomId);
  });

  scheduleTransition(app, room);
  broadcastRoom(room);
  return serializeRoom(room);
}

export type AnswerOutcome = {
  room: SerializedRoom;
  participantId: string;
  result: {
    roundId: string;
    isCorrect: boolean;
    correctIndex: number;
    explanation: string | null;
    scoreGained: number;
    responseMs: number;
    answerIndex: number;
  } | null;
};

export async function answerBattleRound(
  app: FastifyInstance,
  roomId: string,
  userId: string,
  roundId: string,
  answerIndex: number,
): Promise<AnswerOutcome> {
  const outcome = await withRoomLock(roomId, async () => {
    await advanceDueLocked(app, roomId);

    const room = await loadRoomOrThrow(app, roomId);
    const participant = participantFor(room, userId);
    if (!participant || participant.leftAt) {
      throw AppError.notFound('Battle room not found');
    }

    if (room.status !== 'ACTIVE') {
      throw AppError.conflict('BATTLE_FINISHED', 'Bu battle allaqachon tugagan.');
    }

    const round = currentRound(room);
    if (!round || round.id !== roundId || round.position !== room.currentPosition) {
      throw AppError.conflict(
        'BATTLE_ROUND_MISMATCH',
        'Bu savol joriy savol emas.',
      );
    }

    if (room.revealEndsAt) {
      throw AppError.conflict(
        'BATTLE_ROUND_CLOSED',
        'Bu savol uchun vaqt tugagan.',
      );
    }

    const options = asStringArray(round.optionsJson);
    if (answerIndex >= options.length) {
      throw AppError.badRequest('INVALID_ANSWER', 'Javob varianti mavjud emas.');
    }

    const alreadyAnswered = await app.prisma.battleAnswer.findUnique({
      where: {
        roundId_participantId: {
          roundId: round.id,
          participantId: participant.id,
        },
      },
    });
    if (alreadyAnswered) {
      throw AppError.conflict(
        'BATTLE_ALREADY_ANSWERED',
        'Bu savolga allaqachon javob berilgan.',
      );
    }

    const isCorrect = answerIndex === round.correctIndex;
    const responseMs = Math.min(
      Math.max(Date.now() - round.startedAt.getTime(), 0),
      room.timeLimitMs,
    );
    const streak = isCorrect ? participant.streak + 1 : 0;
    const scoreGained = scoreAnswer({
      isCorrect,
      responseMs,
      streak,
      timeLimitMs: room.timeLimitMs,
    });

    await app.prisma.$transaction(async (tx) => {
      await tx.battleAnswer.create({
        data: {
          roundId: round.id,
          participantId: participant.id,
          answerIndex,
          isCorrect,
          responseMs,
          scoreGained,
        },
      });
      await tx.battleParticipant.update({
        where: { id: participant.id },
        data: {
          score: { increment: scoreGained },
          correctCount: { increment: isCorrect ? 1 : 0 },
          streak,
          bestStreak: Math.max(participant.bestStreak, streak),
          lastSeenAt: new Date(),
        },
      });
    });

    let updated = await loadRoomOrThrow(app, roomId);
    const updatedRound = currentRound(updated);
    const humans = humanParticipants(updated);
    const answeredIds = new Set(
      (updatedRound?.answers ?? []).map((answer) => answer.participantId),
    );

    // Finish the round immediately when every human has answered; bots are
    // resolved inside finalizeRoundLocked so they cannot stall the game.
    if (humans.length > 0 && humans.every((human) => answeredIds.has(human.id))) {
      updated = await finalizeRoundLocked(app, updated);
    }

    const finalRound = currentRound(updated);
    const finalAnswer = finalRound?.answers.find(
      (answer) => answer.participantId === participant.id,
    );
    const revealed = battlePhase(updated.status, updated.revealEndsAt) === 'REVEAL';

    return {
      room: updated,
      participantId: participant.id,
      result:
        revealed && finalRound && finalAnswer
          ? {
              roundId: finalRound.id,
              isCorrect: finalAnswer.isCorrect,
              correctIndex: finalRound.correctIndex,
              explanation: finalRound.explanation,
              scoreGained: finalAnswer.scoreGained,
              responseMs: finalAnswer.responseMs,
              answerIndex: finalAnswer.answerIndex,
            }
          : null,
    };
  });

  scheduleTransition(app, outcome.room);
  broadcastRoom(outcome.room);

  return {
    room: serializeRoom(outcome.room),
    participantId: outcome.participantId,
    result: outcome.result,
  };
}

export async function leaveBattleRoom(
  app: FastifyInstance,
  roomId: string,
  userId: string,
): Promise<{ closed: boolean }> {
  const result = await withRoomLock(roomId, async () => {
    const room = await loadRoomOrThrow(app, roomId);
    const participant = participantFor(room, userId);
    if (!participant) return { closed: false, room: null as RoomWithRelations | null };

    if (room.status === 'LOBBY') {
      if (room.hostId === userId) {
        await app.prisma.battleRoom.delete({ where: { id: roomId } });
        return { closed: true, room: null as RoomWithRelations | null };
      }

      await app.prisma.battleParticipant.delete({ where: { id: participant.id } });
      return { closed: false, room: await loadRoomOrThrow(app, roomId) };
    }

    if (room.status === 'ACTIVE') {
      await app.prisma.battleParticipant.update({
        where: { id: participant.id },
        data: { leftAt: new Date(), isReady: false },
      });
      return { closed: false, room: await loadRoomOrThrow(app, roomId) };
    }

    return { closed: false, room };
  });

  if (result.closed) {
    cancelSchedule(roomId);
    closeRoomSockets(roomId);
  } else if (result.room) {
    broadcastRoom(result.room);
  }

  return { closed: result.closed };
}

export function timeLimitIsAllowed(value: number): boolean {
  return BATTLE_TIME_LIMITS_MS.includes(value as (typeof BATTLE_TIME_LIMITS_MS)[number]);
}

export const BATTLE_DEFAULT_TIME_LIMIT_MS = BATTLE_TIME_LIMIT_MS;
