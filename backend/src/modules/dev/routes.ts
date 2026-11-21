import type { FastifyPluginAsync } from 'fastify';
import { AppError } from '../../lib/errors.js';
import { getOutboxFor } from '../../lib/email.js';

/**
 * Development-only helpers. Registered only when NODE_ENV !== 'production'.
 * The outbox is scoped to the authenticated user's own email address, so even
 * on a shared dev network a caller cannot read another account's codes.
 */
export const devRoutes: FastifyPluginAsync = async (app) => {
  app.get('/outbox', { preHandler: [app.authenticate] }, async (request) => {
    const user = await app.prisma.user.findUnique({
      where: { id: request.user.sub },
      select: { email: true },
    });

    if (!user) {
      throw AppError.unauthorized();
    }

    return { messages: getOutboxFor(user.email) };
  });
};
