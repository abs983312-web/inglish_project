import type { FastifyPluginAsync } from 'fastify';
import {
  Prisma,
  SpeakingPart,
  SubmissionStatus,
  type SpeakingPrompt,
  type SpeakingSubmission,
} from '@prisma/client';
import { z } from 'zod';
import { AppError } from '../../lib/errors.js';
import { getAiScorer, type SpeakingPartValue } from '../../lib/ai.js';
import { assertQuota } from '../billing/limits.js';
import { parseInput } from '../../lib/validation.js';

const listPromptsQuerySchema = z.object({
  part: z.nativeEnum(SpeakingPart).optional(),
});

const promptParamsSchema = z.object({
  slug: z.string().trim().min(1).max(120),
});

const createSubmissionSchema = z.object({
  promptSlug: z.string().trim().min(1).max(120),
  transcript: z.string().min(1).max(20_000),
  durationSeconds: z.number().int().min(1).max(600).optional(),
});

const submissionParamsSchema = z.object({
  id: z.string().uuid(),
});

const listSubmissionsQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

function serializePrompt(prompt: SpeakingPrompt) {
  return {
    id: prompt.id,
    slug: prompt.slug,
    part: prompt.part,
    topic: prompt.topic,
    questions: prompt.questionsJson,
    prepTimeSeconds: prompt.prepTimeSeconds,
    speakTimeSeconds: prompt.speakTimeSeconds,
  };
}

function serializeSubmission(submission: SpeakingSubmission, prompt?: SpeakingPrompt) {
  return {
    id: submission.id,
    promptId: submission.promptId,
    prompt: prompt ? serializePrompt(prompt) : undefined,
    transcript: submission.transcript,
    durationSeconds: submission.durationSeconds,
    status: submission.status,
    bandEstimate: submission.bandEstimate === null ? null : Number(submission.bandEstimate),
    confidenceLow: submission.confidenceLow === null ? null : Number(submission.confidenceLow),
    confidenceHigh: submission.confidenceHigh === null ? null : Number(submission.confidenceHigh),
    feedback: submission.feedbackJson,
    modelVersion: submission.modelVersion,
    submittedAt: submission.submittedAt?.toISOString() ?? null,
    scoredAt: submission.scoredAt?.toISOString() ?? null,
    createdAt: submission.createdAt.toISOString(),
  };
}

export const speakingRoutes: FastifyPluginAsync = async (app) => {
  app.get('/speaking/prompts', async (request) => {
    const query = parseInput(listPromptsQuerySchema, request.query, 'Invalid query');

    const prompts = await app.prisma.speakingPrompt.findMany({
      where: {
        published: true,
        ...(query.part ? { part: query.part } : {}),
      },
      orderBy: [{ part: 'asc' }, { createdAt: 'asc' }],
    });

    return { prompts: prompts.map(serializePrompt) };
  });

  app.get('/speaking/prompts/:slug', async (request) => {
    const { slug } = parseInput(promptParamsSchema, request.params, 'Invalid prompt id');

    const prompt = await app.prisma.speakingPrompt.findFirst({
      where: { slug, published: true },
    });

    if (!prompt) {
      throw AppError.notFound('Speaking prompt not found');
    }

    return { prompt: serializePrompt(prompt) };
  });

  app.post('/speaking/submissions', { preHandler: [app.authenticate] }, async (request, reply) => {
    const input = parseInput(createSubmissionSchema, request.body, 'Invalid submission');
    const userId = request.user.sub;

    const prompt = await app.prisma.speakingPrompt.findFirst({
      where: { slug: input.promptSlug, published: true },
    });

    if (!prompt) {
      throw AppError.notFound('Speaking prompt not found');
    }

    const submission = await app.prisma.speakingSubmission.create({
      data: {
        userId,
        promptId: prompt.id,
        transcript: input.transcript,
        durationSeconds: input.durationSeconds,
        status: SubmissionStatus.DRAFT,
      },
    });

    return reply.code(201).send({ submission: serializeSubmission(submission, prompt) });
  });

  app.post(
    '/speaking/submissions/:id/submit',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(submissionParamsSchema, request.params, 'Invalid submission id');
      const userId = request.user.sub;

      const submission = await app.prisma.speakingSubmission.findUnique({
        where: { id },
        include: { prompt: true },
      });

      if (!submission || submission.userId !== userId) {
        throw AppError.notFound('Submission not found');
      }

      if (submission.status === SubmissionStatus.SCORED) {
        return { submission: serializeSubmission(submission, submission.prompt) };
      }

      if (submission.status === SubmissionStatus.SCORING) {
        const isRecent =
          submission.submittedAt &&
          Date.now() - submission.submittedAt.getTime() < 2 * 60 * 1000;

        if (isRecent) {
          throw new AppError(
            409,
            'SCORING_IN_PROGRESS',
            'This submission is already being scored. Please wait and try again.',
          );
        }

        // A crashed scoring run must not permanently consume the daily quota.
        await app.prisma.speakingSubmission.updateMany({
          where: { id, status: SubmissionStatus.SCORING },
          data: { status: SubmissionStatus.FAILED },
        });
      }

      if (!submission.transcript || submission.transcript.trim().length < 20) {
        throw AppError.badRequest(
          'SUBMISSION_TOO_SHORT',
          'Record a longer answer before requesting feedback',
        );
      }

      const claimed = await app.prisma.$transaction(async (tx) => {
        // Serialize quota check + claim for this user. The advisory lock is
        // released when the short transaction ends, before the AI call.
        await tx.$executeRaw`SELECT pg_advisory_xact_lock(hashtext(${userId})::bigint)`;
        await assertQuota(tx, userId, 'speaking');
        return tx.speakingSubmission.updateMany({
          where: {
            id,
            userId,
            status: {
              in: [SubmissionStatus.DRAFT, SubmissionStatus.SUBMITTED, SubmissionStatus.FAILED],
            },
          },
          data: { status: SubmissionStatus.SCORING, submittedAt: new Date() },
        });
      });

      if (claimed.count === 0) {
        const latest = await app.prisma.speakingSubmission.findUnique({ where: { id } });
        if (latest?.status === SubmissionStatus.SCORED) {
          return { submission: serializeSubmission(latest, submission.prompt) };
        }
        throw new AppError(
          409,
          'SCORING_IN_PROGRESS',
          'This submission is already being scored. Please wait and try again.',
        );
      }

      const scorer = getAiScorer();
      const now = new Date();

      try {
        const feedback = await scorer.scoreSpeaking({
          topic: submission.prompt.topic,
          part: submission.prompt.part as SpeakingPartValue,
          questions: submission.prompt.questionsJson,
          transcript: submission.transcript,
          durationSeconds: submission.durationSeconds ?? undefined,
        });

        const updated = await app.prisma.speakingSubmission.update({
          where: { id },
          data: {
            status: SubmissionStatus.SCORED,
            bandEstimate: feedback.overallBand,
            confidenceLow: feedback.confidenceLow,
            confidenceHigh: feedback.confidenceHigh,
            feedbackJson: feedback as unknown as Prisma.InputJsonValue,
            modelVersion: scorer.provider,
            scoredAt: now,
          },
        });

        return { submission: serializeSubmission(updated, submission.prompt) };
      } catch (error) {
        request.log.error({ err: error, submissionId: id }, 'Speaking AI scoring failed');
        await app.prisma.speakingSubmission.update({
          where: { id },
          data: { status: SubmissionStatus.FAILED },
        });
        throw new AppError(502, 'AI_SCORING_FAILED', 'AI scoring failed. Please try again.');
      }
    },
  );

  app.get('/speaking/submissions', { preHandler: [app.authenticate] }, async (request) => {
    const { limit } = parseInput(listSubmissionsQuerySchema, request.query, 'Invalid query');
    const userId = request.user.sub;

    const submissions = await app.prisma.speakingSubmission.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: {
        prompt: { select: { topic: true, slug: true, part: true } },
      },
    });

    return {
      submissions: submissions.map((submission) => ({
        id: submission.id,
        promptId: submission.promptId,
        topic: submission.prompt.topic,
        promptSlug: submission.prompt.slug,
        part: submission.prompt.part,
        durationSeconds: submission.durationSeconds,
        status: submission.status,
        bandEstimate: submission.bandEstimate === null ? null : Number(submission.bandEstimate),
        createdAt: submission.createdAt.toISOString(),
        scoredAt: submission.scoredAt?.toISOString() ?? null,
      })),
    };
  });

  app.get('/speaking/submissions/:id', { preHandler: [app.authenticate] }, async (request) => {
    const { id } = parseInput(submissionParamsSchema, request.params, 'Invalid submission id');
    const userId = request.user.sub;

    const submission = await app.prisma.speakingSubmission.findUnique({
      where: { id },
      include: { prompt: true },
    });

    if (!submission || submission.userId !== userId) {
      throw AppError.notFound('Submission not found');
    }

    return { submission: serializeSubmission(submission, submission.prompt) };
  });
};
