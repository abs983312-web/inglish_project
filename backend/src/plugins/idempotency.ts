import fp from 'fastify-plugin';
import type { FastifyReply, FastifyRequest } from 'fastify';
import { AppError } from '../lib/errors.js';
import { hashIdempotentRequest, isValidIdempotencyKey } from '../lib/idempotency.js';

const TTL_MS = 24 * 60 * 60 * 1000;
const CLEANUP_INTERVAL_MS = 60 * 60 * 1000;

declare module 'fastify' {
  interface FastifyInstance {
    /**
     * Route-level preHandler that must run after `app.authenticate`. When the
     * client sends an `Idempotency-Key`, a successful response is stored and
     * replayed for 24 hours.
     */
    idempotency: (request: FastifyRequest, reply: FastifyReply) => Promise<void>;
  }

  interface FastifyRequest {
    idempotency?: { key: string; requestHash: string };
  }
}

export default fp(async (app) => {
  app.decorate('idempotency', async (request, reply) => {
    const header = request.headers['idempotency-key'];
    const key = Array.isArray(header) ? header[0] : header;
    if (!key) return;

    if (!isValidIdempotencyKey(key)) {
      throw AppError.badRequest(
        'IDEMPOTENCY_KEY_INVALID',
        'Idempotency-Key must be 8-128 URL-safe characters',
      );
    }

    // `idempotency` is registered after `authenticate`, so the user is always
    // present here; the guard keeps the decorator safe if it is ever reused.
    const userId = request.user?.sub;
    if (!userId) return;

    const requestHash = hashIdempotentRequest(request.body);
    const existing = await app.prisma.idempotencyKey.findUnique({
      where: { userId_key: { userId, key } },
    });

    if (existing) {
      if (existing.expiresAt.getTime() <= Date.now()) {
        await app.prisma.idempotencyKey.delete({ where: { id: existing.id } });
      } else if (existing.requestHash !== requestHash) {
        throw AppError.conflict(
          'IDEMPOTENCY_KEY_REUSED',
          'This Idempotency-Key was already used with a different request body',
        );
      } else {
        reply
          .code(existing.statusCode)
          .header('idempotency-replayed', 'true')
          .type(existing.contentType)
          .send(existing.responseBody);
        return;
      }
    }

    request.idempotency = { key, requestHash };
  });

  app.addHook('onSend', async (request, reply, payload) => {
    const pending = request.idempotency;
    const userId = request.user?.sub;
    if (!pending || !userId) return payload;

    // Only successful responses are replayable. Caching 4xx/5xx would make a
    // transient failure permanent for the lifetime of the key.
    if (reply.statusCode < 200 || reply.statusCode >= 300) return payload;

    const body = typeof payload === 'string' ? payload : JSON.stringify(payload);
    const contentType = reply.getHeader('content-type');

    try {
      await app.prisma.idempotencyKey.upsert({
        where: { userId_key: { userId, key: pending.key } },
        create: {
          userId,
          key: pending.key,
          method: request.method,
          path: request.url,
          requestHash: pending.requestHash,
          statusCode: reply.statusCode,
          contentType:
            typeof contentType === 'string' ? contentType : 'application/json',
          responseBody: body,
          expiresAt: new Date(Date.now() + TTL_MS),
        },
        // A concurrent duplicate already stored its response; keep the first.
        update: {},
      });
    } catch (error) {
      // Failing to persist the replay copy must never fail the actual request.
      request.log.warn({ err: error }, 'failed to store idempotency response');
    }

    return payload;
  });

  // Expired keys are also deleted lazily on reuse, but unused keys would
  // otherwise accumulate forever. One hourly sweep keeps the table small.
  const cleanup = setInterval(async () => {
    try {
      await app.prisma.idempotencyKey.deleteMany({
        where: { expiresAt: { lte: new Date() } },
      });
    } catch (error) {
      app.log.warn({ err: error }, 'failed to clean up expired idempotency keys');
    }
  }, CLEANUP_INTERVAL_MS);
  cleanup.unref();

  app.addHook('onClose', async () => {
    clearInterval(cleanup);
  });
}, {
  name: 'idempotency',
  dependencies: ['prisma', 'auth'],
});
