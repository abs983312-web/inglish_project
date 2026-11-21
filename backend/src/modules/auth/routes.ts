import type { FastifyPluginAsync } from 'fastify';
import type { z } from 'zod';
import { env } from '../../config/env.js';
import { AppError } from '../../lib/errors.js';
import { AuthService } from './service.js';
import {
  authCodeSchema,
  forgotPasswordSchema,
  loginSchema,
  refreshSchema,
  registerSchema,
  resetPasswordSchema,
} from './schemas.js';

function parseBody<T>(schema: z.ZodType<T>, body: unknown): T {
  const result = schema.safeParse(body);
  if (!result.success) {
    throw AppError.badRequest('VALIDATION_ERROR', 'Invalid request body', result.error.flatten());
  }
  return result.data;
}

export const authRoutes: FastifyPluginAsync = async (app) => {
  const service = new AuthService(app.prisma, (payload) =>
    app.jwt.sign(payload, { expiresIn: env.ACCESS_TOKEN_TTL }),
  );

  app.post('/register', { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
    const input = parseBody(registerSchema, request.body);
    const result = await service.register(input);
    return reply.code(201).send(result);
  });

  app.post('/login', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request) => {
    const input = parseBody(loginSchema, request.body);
    return service.login(input);
  });

  app.post('/refresh', { config: { rateLimit: { max: 30, timeWindow: '1 minute' } } }, async (request) => {
    const input = parseBody(refreshSchema, request.body);
    return service.refresh(input.refreshToken);
  });

  app.post('/logout', { config: { rateLimit: { max: 30, timeWindow: '1 minute' } } }, async (request, reply) => {
    const input = parseBody(refreshSchema, request.body);
    await service.logout(input.refreshToken);
    return reply.code(204).send();
  });

  app.post(
    '/verify-email/request',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 3, timeWindow: '1 minute' } },
    },
    async (request) => {
      return service.requestEmailVerification(request.user.sub);
    },
  );

  app.post(
    '/verify-email/confirm',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request) => {
      const input = parseBody(authCodeSchema, request.body);
      return service.confirmEmailVerification(request.user.sub, input.code);
    },
  );

  app.post(
    '/password/forgot',
    { config: { rateLimit: { max: 3, timeWindow: '1 minute' } } },
    async (request, reply) => {
      const input = parseBody(forgotPasswordSchema, request.body);
      await service.requestPasswordReset(input.email);
      // Always 204 so callers cannot tell whether the email exists.
      return reply.code(204).send();
    },
  );

  app.post(
    '/password/reset',
    { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } },
    async (request, reply) => {
      const input = parseBody(resetPasswordSchema, request.body);
      await service.resetPassword(input);
      return reply.code(204).send();
    },
  );
};
