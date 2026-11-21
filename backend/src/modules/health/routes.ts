import type { FastifyPluginAsync } from 'fastify';

export const healthRoutes: FastifyPluginAsync = async (app) => {
  app.get('/health', async () => ({
    status: 'ok',
    service: 'inglish-api',
    version: '0.1.0',
    uptime: Math.round(process.uptime()),
    timestamp: new Date().toISOString(),
  }));

  app.get('/v1/health', async (_request, reply) => {
    try {
      await app.prisma.$queryRaw`SELECT 1`;
      return { status: 'ok', db: 'up' };
    } catch {
      // Readiness probes must see a failure status when the DB is unavailable.
      return reply.code(503).send({ status: 'degraded', db: 'down' });
    }
  });
};
