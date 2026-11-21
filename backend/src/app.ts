import Fastify, { type FastifyError, type FastifyInstance } from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import jwt from '@fastify/jwt';
import rateLimit from '@fastify/rate-limit';
import websocket from '@fastify/websocket';
import { ZodError } from 'zod';
import { env } from './config/env.js';
import { AppError } from './lib/errors.js';
import { healthRoutes } from './modules/health/routes.js';
import { authRoutes } from './modules/auth/routes.js';
import { userRoutes } from './modules/users/routes.js';
import { contentRoutes } from './modules/content/routes.js';
import { vocabularyRoutes } from './modules/vocabulary/routes.js';
import { writingRoutes } from './modules/writing/routes.js';
import { speakingRoutes } from './modules/speaking/routes.js';
import { billingRoutes } from './modules/billing/routes.js';
import { progressRoutes } from './modules/progress/routes.js';
import { battleRoutes } from './modules/battle/routes.js';
import { placementRoutes } from './modules/placement/routes.js';
import { devRoutes } from './modules/dev/routes.js';
import { emailMode } from './lib/email.js';
import authPlugin from './plugins/auth.js';
import idempotencyPlugin from './plugins/idempotency.js';
import prismaPlugin from './plugins/prisma.js';

export async function buildApp(): Promise<FastifyInstance> {
  const app = Fastify({
    logger: env.NODE_ENV !== 'test',
    trustProxy: env.TRUST_PROXY,
  });

  if (env.NODE_ENV === 'production') {
    if (env.CORS_ORIGIN === '*') {
      app.log.warn('CORS_ORIGIN is set to * in production.');
    }
    if (!env.GEMINI_API_KEY) {
      app.log.warn('GEMINI_API_KEY is not configured. AI scoring will run in mock mode.');
    }
    if (emailMode() === 'log') {
      app.log.warn('SMTP is not configured. Emails will be logged to stdout.');
    }
  }

  await app.register(helmet, {
    contentSecurityPolicy: false,
  });

  await app.register(cors, {
    origin: env.CORS_ORIGIN === '*' ? true : env.CORS_ORIGIN.split(',').map((value) => value.trim()),
  });

  await app.register(rateLimit, {
    max: 120,
    timeWindow: '1 minute',
    errorResponseBuilder: (_request, context) => ({
      statusCode: 429,
      code: 'RATE_LIMITED',
      message: `Too many requests. Retry in ${context.after}.`,
    }),
  });

  await app.register(jwt, {
    secret: env.JWT_SECRET,
  });

  await app.register(websocket, {
    options: {
      maxPayload: 64 * 1024,
    },
  });

  await app.register(prismaPlugin);
  await app.register(authPlugin);
  await app.register(idempotencyPlugin);

  app.setErrorHandler((error: FastifyError, request, reply) => {
    if (error instanceof AppError) {
      return reply.code(error.statusCode).send({
        code: error.code,
        message: error.message,
        details: error.details,
      });
    }

    if (error instanceof ZodError) {
      return reply.code(400).send({
        code: 'VALIDATION_ERROR',
        message: 'Invalid request input',
        details: error.flatten(),
      });
    }

    // @fastify/rate-limit throws an error without statusCode when a custom
    // errorResponseBuilder is used; normalise it here instead of returning 500.
    const statusCode =
      error.statusCode ?? (error.code === 'RATE_LIMITED' ? 429 : 500);

    if (statusCode >= 500) {
      request.log.error(error);
    }

    const code =
      statusCode === 400
        ? 'VALIDATION_ERROR'
        : statusCode === 401
          ? 'UNAUTHORIZED'
          : statusCode === 403
            ? 'FORBIDDEN'
            : statusCode === 404
              ? 'NOT_FOUND'
              : statusCode === 429
                ? 'RATE_LIMITED'
                : 'ERROR';

    return reply.code(statusCode).send({
      code,
      message:
        statusCode >= 500
          ? 'Internal server error'
          : statusCode === 400
            ? 'Invalid request'
            : error.message,
    });
  });

  app.setNotFoundHandler((_request, reply) =>
    reply.code(404).send({ code: 'NOT_FOUND', message: 'Route not found' }),
  );

  await app.register(healthRoutes);
  await app.register(authRoutes, { prefix: '/v1/auth' });
  await app.register(userRoutes, { prefix: '/v1' });
  await app.register(contentRoutes, { prefix: '/v1' });
  await app.register(vocabularyRoutes, { prefix: '/v1' });
  await app.register(writingRoutes, { prefix: '/v1' });
  await app.register(speakingRoutes, { prefix: '/v1' });
  await app.register(billingRoutes, { prefix: '/v1' });
  await app.register(progressRoutes, { prefix: '/v1' });
  await app.register(battleRoutes, { prefix: '/v1' });
  await app.register(placementRoutes, { prefix: '/v1' });

  if (env.NODE_ENV !== 'production') {
    await app.register(devRoutes, { prefix: '/v1/dev' });
  }

  return app;
}
