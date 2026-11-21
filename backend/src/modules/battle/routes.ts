import type { FastifyInstance, FastifyPluginAsync, FastifyRequest } from 'fastify';
import type { WebSocket } from 'ws';
import '@fastify/websocket';
import { z } from 'zod';
import { parseInput } from '../../lib/validation.js';
import {
  answerBattleRound,
  addBattleBot,
  broadcastRoom,
  buildRoomReport,
  createBattleRoom,
  createBattleRoomFromPack,
  getRoomForUser,
  joinBattleRoom,
  leaveBattleRoom,
  loadRoomRaw,
  setBattleNickname,
  setBattleReady,
  startBattleRoom,
  syncRoom,
  timeLimitIsAllowed,
  BATTLE_DEFAULT_TIME_LIMIT_MS,
} from '../../lib/battle-engine.js';
import {
  createBattlePack,
  deleteBattlePack,
  getBattlePack,
  listBattlePacks,
  updateBattlePack,
} from '../../lib/battle-packs.js';
import { AppError } from '../../lib/errors.js';
import { subscribe, unsubscribe } from '../../lib/battle-hub.js';
import { BATTLE_CODE_LENGTH } from '../../lib/battle.js';

const roomParamsSchema = z.object({
  id: z.string().uuid(),
});

const createRoomSchema = z.object({
  mode: z.enum(['BOT', 'FRIEND', 'CLASSROOM']).optional(),
  questionCount: z.number().int().min(5).max(20).optional(),
  timeLimitMs: z
    .number()
    .int()
    .refine(timeLimitIsAllowed, 'Vaqt 10, 20 yoki 30 soniya bo‘lishi kerak')
    .optional(),
  autoBot: z.boolean().optional(),
});

const joinRoomSchema = z.object({
  code: z
    .string()
    .trim()
    .regex(new RegExp(`^\\d{${BATTLE_CODE_LENGTH}}$`), 'Xona kodi 6 raqamdan iborat'),
  nickname: z.string().trim().min(2).max(20).optional(),
});

const nicknameSchema = z.object({
  nickname: z.string().trim().min(2).max(20),
});

const packQuestionSchema = z.object({
  type: z.enum(['MCQ', 'TRUE_FALSE']).optional(),
  prompt: z.string().trim().min(3).max(400),
  options: z.array(z.string().trim().min(1).max(120)).max(4).optional(),
  correctIndex: z.number().int().min(0).max(3),
  explanation: z.string().trim().max(600).optional(),
  timeLimitMs: z.number().int().optional(),
});

const packSchema = z.object({
  title: z.string().trim().min(3).max(120),
  description: z.string().trim().max(1000).optional(),
  videoUrl: z.string().trim().min(1).max(500),
  tags: z.array(z.string().trim().max(24)).max(5).optional(),
  questions: z.array(packQuestionSchema).min(1).max(50),
});

const packListQuerySchema = z.object({
  scope: z.enum(['mine', 'popular', 'new', 'all']).optional(),
  search: z.string().trim().max(80).optional(),
  tag: z.string().trim().max(24).optional(),
  offset: z.coerce.number().int().min(0).optional(),
  limit: z.coerce.number().int().min(1).max(50).optional(),
});

const packParamsSchema = z.object({
  id: z.string().uuid(),
});

const playPackSchema = z.object({
  mode: z.enum(['FRIEND', 'CLASSROOM']).optional(),
  shuffle: z.boolean().optional(),
});

const readySchema = z.object({
  ready: z.boolean(),
});

const answerSchema = z.object({
  roundId: z.string().uuid(),
  // -1 means the timer expired before the player confirmed an option.
  answerIndex: z.number().int().min(-1).max(20),
});

const wsQuerySchema = z.object({
  token: z.string().min(1),
});

function sendSocketError(socket: WebSocket, code: string, message: string): void {
  try {
    socket.send(JSON.stringify({ type: 'error', code, message }));
  } catch {
    // socket is already gone
  }
  try {
    socket.close(1008, code.slice(0, 120));
  } catch {
    // ignore
  }
}

async function handleBattleSocket(
  app: FastifyInstance,
  socket: WebSocket,
  request: FastifyRequest,
): Promise<void> {
  const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');

  let token: string;
  try {
    token = parseInput(wsQuerySchema, request.query ?? {}, 'Missing socket token').token;
  } catch {
    sendSocketError(socket, 'UNAUTHORIZED', 'Token topilmadi.');
    return;
  }

  let userId: string;
  try {
    const payload = app.jwt.verify<{ sub: string; role: string }>(token);
    userId = payload.sub;
  } catch {
    sendSocketError(socket, 'UNAUTHORIZED', 'Token yaroqsiz.');
    return;
  }

  const user = await app.prisma.user.findUnique({
    where: { id: userId },
    select: { deletedAt: true },
  });
  if (!user || user.deletedAt) {
    sendSocketError(socket, 'UNAUTHORIZED', 'Hisob faol emas.');
    return;
  }

  await syncRoom(app, id);
  const room = await loadRoomRaw(app, id);
  const participant = room?.participants.find((entry) => entry.userId === userId);
  if (!room || !participant) {
    sendSocketError(socket, 'NOT_FOUND', 'Battle xonasi topilmadi.');
    return;
  }

  const client = {
    socket,
    roomId: id,
    participantId: participant.id,
    userId,
  };

  subscribe(client);

  await app.prisma.battleParticipant.update({
    where: { id: participant.id },
    data: { lastSeenAt: new Date() },
  });

  // Broadcast to everyone (including the new socket) so all clients see the
  // fresh online state immediately.
  const fresh = await loadRoomRaw(app, id);
  if (fresh) broadcastRoom(fresh);

  socket.on('close', () => {
    unsubscribe(client);
    void (async () => {
      const updated = await loadRoomRaw(app, id);
      if (updated) broadcastRoom(updated);
    })();
  });

  socket.on('error', () => {
    unsubscribe(client);
  });

  socket.on('message', (raw) => {
    try {
      const message = JSON.parse(raw.toString()) as { type?: string };
      if (message.type === 'ping') {
        socket.send(JSON.stringify({ type: 'pong' }));
      }
    } catch {
      // Ignore malformed client frames; all game actions go through REST.
    }
  });
}

export const battleRoutes: FastifyPluginAsync = async (app) => {
  // ---- User-generated video lesson packs (Kahoot-style) ----

  app.get('/battle/packs', { preHandler: [app.authenticate] }, async (request) => {
    const query = parseInput(packListQuerySchema, request.query ?? {}, 'Invalid pack query');
    return listBattlePacks(app, request.user.sub, query);
  });

  app.post(
    '/battle/packs',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      const input = parseInput(packSchema, request.body ?? {}, 'Invalid pack');
      const pack = await createBattlePack(app, request.user.sub, {
        title: input.title,
        description: input.description,
        videoUrl: input.videoUrl,
        tags: input.tags,
        questions: input.questions.map((question) => ({
          type: question.type ?? 'MCQ',
          prompt: question.prompt,
          options: question.options ?? [],
          correctIndex: question.correctIndex,
          explanation: question.explanation,
          timeLimitMs: question.timeLimitMs,
        })),
      });
      return reply.code(201).send({ pack });
    },
  );

  app.get('/battle/packs/:id', { preHandler: [app.authenticate] }, async (request) => {
    const { id } = parseInput(packParamsSchema, request.params, 'Invalid pack id');
    return { pack: await getBattlePack(app, id, request.user.sub) };
  });

  app.put(
    '/battle/packs/:id',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 20, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(packParamsSchema, request.params, 'Invalid pack id');
      const input = parseInput(packSchema, request.body ?? {}, 'Invalid pack');
      const pack = await updateBattlePack(app, id, request.user.sub, {
        title: input.title,
        description: input.description,
        videoUrl: input.videoUrl,
        tags: input.tags,
        questions: input.questions.map((question) => ({
          type: question.type ?? 'MCQ',
          prompt: question.prompt,
          options: question.options ?? [],
          correctIndex: question.correctIndex,
          explanation: question.explanation,
          timeLimitMs: question.timeLimitMs,
        })),
      });
      return { pack };
    },
  );

  app.delete(
    '/battle/packs/:id',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 20, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(packParamsSchema, request.params, 'Invalid pack id');
      return deleteBattlePack(app, id, request.user.sub);
    },
  );

  app.post(
    '/battle/packs/:id/play',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 20, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      const { id } = parseInput(packParamsSchema, request.params, 'Invalid pack id');
      const input = parseInput(playPackSchema, request.body ?? {}, 'Invalid play settings');
      const created = await createBattleRoomFromPack(app, request.user.sub, id, {
        mode: input.mode ?? 'FRIEND',
        shuffle: input.shuffle ?? false,
      });
      return reply.code(201).send(created);
    },
  );

  // ---- Battle rooms ----

  app.post(
    '/battle/rooms',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      const input = parseInput(createRoomSchema, request.body ?? {}, 'Invalid room settings');
      const created = await createBattleRoom(app, request.user.sub, {
        mode: input.mode ?? 'BOT',
        questionCount: input.questionCount ?? 10,
        timeLimitMs: input.timeLimitMs ?? BATTLE_DEFAULT_TIME_LIMIT_MS,
        autoBot: input.autoBot ?? true,
      });

      return reply.code(201).send(created);
    },
  );

  app.post(
    '/battle/rooms/join',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 30, timeWindow: '1 minute' } },
    },
    async (request) => {
      const input = parseInput(joinRoomSchema, request.body ?? {}, 'Invalid room code');
      return joinBattleRoom(app, request.user.sub, input.code, input.nickname);
    },
  );

  app.get('/battle/rooms/:id', { preHandler: [app.authenticate] }, async (request) => {
    const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
    return getRoomForUser(app, id, request.user.sub);
  });

  app.get('/battle/rooms/:id/report', { preHandler: [app.authenticate] }, async (request) => {
    const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
    const synced = await syncRoom(app, id);
    const room = synced ?? (await loadRoomRaw(app, id));
    if (!room) throw AppError.notFound('Battle room not found');
    if (room.hostId !== request.user.sub) {
      throw AppError.forbidden('Hisobot faqat xona egasi uchun.');
    }
    return { report: buildRoomReport(room) };
  });

  app.post(
    '/battle/rooms/:id/nickname',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 30, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
      const input = parseInput(nicknameSchema, request.body ?? {}, 'Invalid nickname');
      const room = await setBattleNickname(app, id, request.user.sub, input.nickname);
      return { room };
    },
  );

  app.post(
    '/battle/rooms/:id/ready',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 60, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
      const input = parseInput(readySchema, request.body ?? {}, 'Invalid ready state');
      const room = await setBattleReady(app, id, request.user.sub, input.ready);
      return { room };
    },
  );

  app.post(
    '/battle/rooms/:id/bots',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 30, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
      const room = await addBattleBot(app, id, request.user.sub);
      return { room };
    },
  );

  app.post(
    '/battle/rooms/:id/start',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 30, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
      const room = await startBattleRoom(app, id, request.user.sub);
      return { room };
    },
  );

  app.post(
    '/battle/rooms/:id/answer',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 120, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
      const input = parseInput(answerSchema, request.body ?? {}, 'Invalid answer');
      return answerBattleRound(
        app,
        id,
        request.user.sub,
        input.roundId,
        input.answerIndex,
      );
    },
  );

  app.post(
    '/battle/rooms/:id/leave',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 30, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(roomParamsSchema, request.params, 'Invalid room id');
      return leaveBattleRoom(app, id, request.user.sub);
    },
  );

  app.get(
    '/battle/rooms/:id/ws',
    { websocket: true },
    (socket, request) => {
      void handleBattleSocket(app, socket, request).catch((error) => {
        request.log.error(error);
        sendSocketError(socket, 'INTERNAL_ERROR', 'Battle ulanishida xatolik.');
      });
    },
  );
};
