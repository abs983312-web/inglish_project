import fp from 'fastify-plugin';
import type { FastifyRequest } from 'fastify';
import { AppError } from '../lib/errors.js';

declare module 'fastify' {
  interface FastifyInstance {
    authenticate: (request: FastifyRequest) => Promise<void>;
  }
}

declare module '@fastify/jwt' {
  interface FastifyJWT {
    payload: { sub: string; role: string };
    user: { sub: string; role: string };
  }
}

export default fp(async (app) => {
  app.decorate('authenticate', async (request: FastifyRequest) => {
    try {
      await request.jwtVerify();
    } catch {
      throw AppError.unauthorized('Invalid or missing access token');
    }

    // A soft-deleted account must lose access immediately, not when its
    // access token expires. Refresh tokens are already revoked on delete.
    const user = await app.prisma.user.findUnique({
      where: { id: request.user.sub },
      select: { deletedAt: true },
    });

    if (!user || user.deletedAt) {
      throw AppError.unauthorized('Account is no longer active');
    }
  });
}, {
  name: 'auth',
  dependencies: ['@fastify/jwt', 'prisma'],
});
