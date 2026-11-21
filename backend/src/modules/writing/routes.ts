import type { FastifyPluginAsync } from 'fastify';
import {
  Prisma,
  SubmissionStatus,
  WritingTaskType,
  type WritingSubmission,
  type WritingTask,
} from '@prisma/client';
import { z } from 'zod';
import { AppError } from '../../lib/errors.js';
import { getAiScorer, type WritingTaskTypeValue } from '../../lib/ai.js';
import { assertQuota } from '../billing/limits.js';
import { countWords } from '../../lib/text.js';
import { parseInput } from '../../lib/validation.js';

const listTasksQuerySchema = z.object({
  type: z.nativeEnum(WritingTaskType).optional(),
});

const taskParamsSchema = z.object({
  slug: z.string().trim().min(1).max(120),
});

const createSubmissionSchema = z.object({
  taskSlug: z.string().trim().min(1).max(120),
  text: z.string().min(1).max(20_000),
});

const submissionParamsSchema = z.object({
  id: z.string().uuid(),
});

const listSubmissionsQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

function serializeTask(task: WritingTask) {
  return {
    id: task.id,
    slug: task.slug,
    type: task.type,
    title: task.title,
    prompt: task.prompt,
    imageUrl: task.imageUrl,
    minWords: task.minWords,
    timeLimitMinutes: task.timeLimitMinutes,
  };
}

function serializeSubmission(submission: WritingSubmission, task?: WritingTask) {
  return {
    id: submission.id,
    taskId: submission.taskId,
    task: task ? serializeTask(task) : undefined,
    text: submission.text,
    wordCount: submission.wordCount,
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

export const writingRoutes: FastifyPluginAsync = async (app) => {
  app.get('/writing/tasks', async (request) => {
    const query = parseInput(listTasksQuerySchema, request.query, 'Invalid query');

    const tasks = await app.prisma.writingTask.findMany({
      where: {
        published: true,
        ...(query.type ? { type: query.type } : {}),
      },
      orderBy: [{ type: 'asc' }, { createdAt: 'asc' }],
    });

    return { tasks: tasks.map(serializeTask) };
  });

  app.get('/writing/tasks/:slug', async (request) => {
    const { slug } = parseInput(taskParamsSchema, request.params, 'Invalid task id');

    const task = await app.prisma.writingTask.findFirst({
      where: { slug, published: true },
    });

    if (!task) {
      throw AppError.notFound('Writing task not found');
    }

    return {
      task: {
        ...serializeTask(task),
        sampleAnswer: task.sampleAnswer,
        bandDescriptors: task.bandDescriptors,
      },
    };
  });

  app.post('/writing/submissions', { preHandler: [app.authenticate] }, async (request, reply) => {
    const input = parseInput(createSubmissionSchema, request.body, 'Invalid submission');
    const userId = request.user.sub;

    const task = await app.prisma.writingTask.findFirst({
      where: { slug: input.taskSlug, published: true },
    });

    if (!task) {
      throw AppError.notFound('Writing task not found');
    }

    const submission = await app.prisma.writingSubmission.create({
      data: {
        userId,
        taskId: task.id,
        text: input.text,
        wordCount: countWords(input.text),
        status: SubmissionStatus.DRAFT,
      },
    });

    return reply.code(201).send({ submission: serializeSubmission(submission, task) });
  });

  app.post(
    '/writing/submissions/:id/submit',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(submissionParamsSchema, request.params, 'Invalid submission id');
      const userId = request.user.sub;

      const submission = await app.prisma.writingSubmission.findUnique({
        where: { id },
        include: { task: true },
      });

      if (!submission || submission.userId !== userId) {
        throw AppError.notFound('Submission not found');
      }

      if (submission.status === SubmissionStatus.SCORED) {
        return { submission: serializeSubmission(submission, submission.task) };
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

        // A crashed scoring run must not permanently consume the monthly quota.
        await app.prisma.writingSubmission.updateMany({
          where: { id, status: SubmissionStatus.SCORING },
          data: { status: SubmissionStatus.FAILED },
        });
      }

      if (submission.wordCount < 20) {
        throw AppError.badRequest(
          'SUBMISSION_TOO_SHORT',
          'Write at least 20 words before requesting feedback',
        );
      }

      const claimed = await app.prisma.$transaction(async (tx) => {
        // Serialize quota check + claim for this user. The advisory lock is
        // released when the short transaction ends, before the AI call.
        await tx.$executeRaw`SELECT pg_advisory_xact_lock(hashtext(${userId})::bigint)`;
        await assertQuota(tx, userId, 'writing');
        return tx.writingSubmission.updateMany({
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
        const latest = await app.prisma.writingSubmission.findUnique({ where: { id } });
        if (latest?.status === SubmissionStatus.SCORED) {
          return { submission: serializeSubmission(latest, submission.task) };
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
        const feedback = await scorer.scoreWriting({
          prompt: submission.task.prompt,
          taskType: submission.task.type as WritingTaskTypeValue,
          minWords: submission.task.minWords,
          text: submission.text,
        });

        const updated = await app.prisma.writingSubmission.update({
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

        return { submission: serializeSubmission(updated, submission.task) };
      } catch (error) {
        request.log.error({ err: error, submissionId: id }, 'Writing AI scoring failed');
        await app.prisma.writingSubmission.update({
          where: { id },
          data: { status: SubmissionStatus.FAILED },
        });
        throw new AppError(502, 'AI_SCORING_FAILED', 'AI scoring failed. Please try again.');
      }
    },
  );

  app.get('/writing/submissions', { preHandler: [app.authenticate] }, async (request) => {
    const { limit } = parseInput(listSubmissionsQuerySchema, request.query, 'Invalid query');
    const userId = request.user.sub;

    const submissions = await app.prisma.writingSubmission.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: { task: { select: { title: true, slug: true, type: true } } },
    });

    return {
      submissions: submissions.map((submission) => ({
        id: submission.id,
        taskId: submission.taskId,
        taskTitle: submission.task.title,
        taskSlug: submission.task.slug,
        taskType: submission.task.type,
        wordCount: submission.wordCount,
        status: submission.status,
        bandEstimate: submission.bandEstimate === null ? null : Number(submission.bandEstimate),
        createdAt: submission.createdAt.toISOString(),
        scoredAt: submission.scoredAt?.toISOString() ?? null,
      })),
    };
  });

  app.get('/writing/submissions/:id', { preHandler: [app.authenticate] }, async (request) => {
    const { id } = parseInput(submissionParamsSchema, request.params, 'Invalid submission id');
    const userId = request.user.sub;

    const submission = await app.prisma.writingSubmission.findUnique({
      where: { id },
      include: { task: true },
    });

    if (!submission || submission.userId !== userId) {
      throw AppError.notFound('Submission not found');
    }

    return { submission: serializeSubmission(submission, submission.task) };
  });
};
