import type { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { PlacementStatus, type Prisma } from '@prisma/client';
import { z } from 'zod';
import { AppError } from '../../lib/errors.js';
import { getAiScorer } from '../../lib/ai.js';
import { countWords } from '../../lib/text.js';
import { parseInput } from '../../lib/validation.js';
import {
  PLACEMENT_GRAMMAR,
  PLACEMENT_READING,
  PLACEMENT_SPEAKING,
  PLACEMENT_WRITING,
  emptySectionStats,
  placementCefrForBand,
  placementResult,
  roundBand,
  type PlacementQuestion,
  type PlacementSectionStats,
} from '../../lib/placement.js';

const sessionParamsSchema = z.object({
  id: z.string().uuid(),
});

const submitSchema = z.object({
  answers: z
    .array(
      z.object({
        questionId: z.string().trim().min(1).max(120),
        answerIndex: z.number().int().min(0).max(20),
      }),
    )
    .min(1)
    .max(100),
  writingText: z.string().trim().min(1).max(20_000),
  speakingTranscript: z.string().trim().min(1).max(20_000),
});

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

function asQuestions(value: Prisma.JsonValue): PlacementQuestion[] {
  if (!Array.isArray(value)) return [];

  return value.filter((item): item is PlacementQuestion => {
    if (item === null || typeof item !== 'object' || Array.isArray(item)) return false;
    const candidate = item as Record<string, unknown>;
    return (
      typeof candidate.id === 'string' &&
      typeof candidate.section === 'string' &&
      typeof candidate.prompt === 'string' &&
      Array.isArray(candidate.options) &&
      typeof candidate.correctIndex === 'number'
    );
  });
}

type WritingTaskSnapshot = {
  slug: string;
  taskType: string;
  title: string;
  prompt: string;
  minWords: number;
  timeLimitMinutes: number;
};

type SpeakingPromptSnapshot = {
  slug: string;
  part: string;
  topic: string;
  questions: string[];
  prepTimeSeconds: number;
  speakTimeSeconds: number;
};

function asRecord(value: Prisma.JsonValue): Record<string, Prisma.JsonValue> {
  if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, Prisma.JsonValue>;
  }
  return {};
}

function asWritingTask(value: Prisma.JsonValue): WritingTaskSnapshot {
  const record = asRecord(value);
  return {
    slug: typeof record.slug === 'string' ? record.slug : PLACEMENT_WRITING.slug,
    taskType: typeof record.taskType === 'string' ? record.taskType : PLACEMENT_WRITING.taskType,
    title: typeof record.title === 'string' ? record.title : PLACEMENT_WRITING.title,
    prompt: typeof record.prompt === 'string' ? record.prompt : PLACEMENT_WRITING.prompt,
    minWords: typeof record.minWords === 'number' ? record.minWords : PLACEMENT_WRITING.minWords,
    timeLimitMinutes:
      typeof record.timeLimitMinutes === 'number'
        ? record.timeLimitMinutes
        : PLACEMENT_WRITING.timeLimitMinutes,
  };
}

function asSpeakingPrompt(value: Prisma.JsonValue): SpeakingPromptSnapshot {
  const record = asRecord(value);
  const questions = Array.isArray(record.questions)
    ? record.questions.filter((item): item is string => typeof item === 'string')
    : PLACEMENT_SPEAKING.questions;

  return {
    slug: typeof record.slug === 'string' ? record.slug : PLACEMENT_SPEAKING.slug,
    part: typeof record.part === 'string' ? record.part : PLACEMENT_SPEAKING.part,
    topic: typeof record.topic === 'string' ? record.topic : PLACEMENT_SPEAKING.topic,
    questions: questions.length > 0 ? questions : PLACEMENT_SPEAKING.questions,
    prepTimeSeconds:
      typeof record.prepTimeSeconds === 'number'
        ? record.prepTimeSeconds
        : PLACEMENT_SPEAKING.prepTimeSeconds,
    speakTimeSeconds:
      typeof record.speakTimeSeconds === 'number'
        ? record.speakTimeSeconds
        : PLACEMENT_SPEAKING.speakTimeSeconds,
  };
}

function serializeSession(session: {
  id: string;
  status: PlacementStatus;
  questionsJson: Prisma.JsonValue;
  answersJson: Prisma.JsonValue | null;
  writingTaskJson: Prisma.JsonValue;
  speakingPromptJson: Prisma.JsonValue;
  writingText: string | null;
  speakingTranscript: string | null;
  writingBand: number | null;
  speakingBand: number | null;
  writingFeedbackJson: Prisma.JsonValue | null;
  speakingFeedbackJson: Prisma.JsonValue | null;
  correctCount: number | null;
  totalCount: number | null;
  percentage: number | null;
  cefrLevel: string | null;
  estimatedBand: number | null;
  weakSkill: string | null;
  sectionStatsJson: Prisma.JsonValue | null;
  startedAt: Date;
  finishedAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}) {
  const questions = asQuestions(session.questionsJson);
  const writingTask = asWritingTask(session.writingTaskJson);
  const speakingPrompt = asSpeakingPrompt(session.speakingPromptJson);

  return {
    id: session.id,
    status: session.status,
    startedAt: session.startedAt.toISOString(),
    finishedAt: session.finishedAt?.toISOString() ?? null,
    createdAt: session.createdAt.toISOString(),
    updatedAt: session.updatedAt.toISOString(),
    questions: questions.map((question) => ({
      id: question.id,
      section: question.section,
      prompt: question.prompt,
      passage: question.passage ?? null,
      options: question.options,
    })),
    writing: {
      ...writingTask,
      text: session.writingText,
      band: session.writingBand,
      feedback: session.writingFeedbackJson,
    },
    speaking: {
      ...speakingPrompt,
      transcript: session.speakingTranscript,
      band: session.speakingBand,
      feedback: session.speakingFeedbackJson,
    },
    result:
      session.status === PlacementStatus.FINISHED
        ? {
            correctCount: session.correctCount,
            totalCount: session.totalCount,
            percentage: session.percentage,
            cefrLevel: session.cefrLevel,
            estimatedBand: session.estimatedBand,
            weakSkill: session.weakSkill,
            sectionStats: (session.sectionStatsJson as PlacementSectionStats | null) ?? null,
            answers: session.answersJson ?? [],
          }
        : null,
  };
}

async function buildQuestions(app: FastifyInstance): Promise<PlacementQuestion[]> {
  const vocabulary = await app.prisma.vocabularyItem.findMany({
    orderBy: { word: 'asc' },
    select: { id: true, word: true, definitionUz: true },
  });

  const definitions = [...new Set(vocabulary.map((item) => item.definitionUz.trim()).filter(Boolean))];
  const vocabularyQuestions: PlacementQuestion[] = [];

  for (const item of vocabulary.slice(0, 10)) {
    const correct = item.definitionUz.trim();
    if (correct.length === 0) continue;

    const distractors = shuffle(definitions.filter((definition) => definition !== correct)).slice(0, 3);
    if (distractors.length < 3) continue;

    const options = shuffle([correct, ...distractors]);
    vocabularyQuestions.push({
      id: `vocab-${item.id}`,
      section: 'VOCABULARY',
      prompt: `“${item.word}” so‘zining ma’nosi qaysi?`,
      options,
      correctIndex: options.indexOf(correct),
      explanation: `“${item.word}” — ${correct}.`,
    });
  }

  if (vocabularyQuestions.length < 4) {
    throw AppError.conflict('NOT_ENOUGH_QUESTIONS', 'Placement uchun yetarli lug‘at savollari topilmadi.');
  }

  return [...PLACEMENT_GRAMMAR, ...vocabularyQuestions, ...PLACEMENT_READING];
}

export const placementRoutes: FastifyPluginAsync = async (app) => {
  app.post(
    '/placement/sessions',
    {
      preHandler: [app.authenticate],
      config: { rateLimit: { max: 10, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      const questions = await buildQuestions(app);

      const session = await app.prisma.placementSession.create({
        data: {
          userId: request.user.sub,
          status: PlacementStatus.ACTIVE,
          questionsJson: questions as unknown as Prisma.InputJsonValue,
          writingTaskJson: PLACEMENT_WRITING as unknown as Prisma.InputJsonValue,
          speakingPromptJson: PLACEMENT_SPEAKING as unknown as Prisma.InputJsonValue,
        },
      });

      return reply.code(201).send({ session: serializeSession(session) });
    },
  );

  app.get('/placement/latest', { preHandler: [app.authenticate] }, async (request) => {
    const session = await app.prisma.placementSession.findFirst({
      where: { userId: request.user.sub, status: PlacementStatus.FINISHED },
      orderBy: { createdAt: 'desc' },
    });

    return { session: session ? serializeSession(session) : null };
  });

  app.get(
    '/placement/sessions/:id',
    { preHandler: [app.authenticate] },
    async (request) => {
      const { id } = parseInput(sessionParamsSchema, request.params, 'Invalid session id');

      const session = await app.prisma.placementSession.findFirst({
        where: { id, userId: request.user.sub },
      });

      if (!session) {
        throw AppError.notFound('Placement session not found');
      }

      return { session: serializeSession(session) };
    },
  );

  app.post(
    '/placement/sessions/:id/submit',
    {
      preHandler: [app.authenticate, app.idempotency],
      config: { rateLimit: { max: 60, timeWindow: '1 minute' } },
    },
    async (request) => {
      const { id } = parseInput(sessionParamsSchema, request.params, 'Invalid session id');
      const { answers, writingText, speakingTranscript } = parseInput(
        submitSchema,
        request.body,
        'Invalid placement answers',
      );

      const session = await app.prisma.placementSession.findFirst({
        where: { id, userId: request.user.sub },
      });

      if (!session) {
        throw AppError.notFound('Placement session not found');
      }

      if (session.status === PlacementStatus.FINISHED) {
        throw AppError.conflict('PLACEMENT_FINISHED', 'Bu placement allaqachon yakunlangan.');
      }

      const questions = asQuestions(session.questionsJson);
      const byId = new Map(questions.map((question) => [question.id, question]));
      const seen = new Set<string>();

      for (const answer of answers) {
        if (seen.has(answer.questionId)) {
          throw AppError.badRequest('DUPLICATE_ANSWER', 'Bir savolga faqat bitta javob berish mumkin.');
        }

        seen.add(answer.questionId);
        const question = byId.get(answer.questionId);

        if (!question || answer.answerIndex >= question.options.length) {
          throw AppError.badRequest('INVALID_ANSWER', 'Javob varianti mavjud emas.');
        }
      }

      if (seen.size !== questions.length) {
        throw AppError.badRequest('INCOMPLETE_PLACEMENT', 'Barcha placement savollariga javob berish kerak.');
      }

      const writingTask = asWritingTask(session.writingTaskJson);
      const speakingPrompt = asSpeakingPrompt(session.speakingPromptJson);

      if (countWords(writingText) < writingTask.minWords) {
        throw AppError.badRequest(
          'SUBMISSION_TOO_SHORT',
          `Yozuv kamida ${writingTask.minWords} so'z bo'lishi kerak.`,
        );
      }

      if (countWords(speakingTranscript) < 20) {
        throw AppError.badRequest(
          'SUBMISSION_TOO_SHORT',
          'Gapirish transkripti kamida 20 so\'z bo\'lishi kerak.',
        );
      }

      const stats = emptySectionStats();
      let correctCount = 0;

      for (const answer of answers) {
        const question = byId.get(answer.questionId);
        if (!question) continue;

        const isCorrect = answer.answerIndex === question.correctIndex;
        stats[question.section].total += 1;
        if (isCorrect) {
          correctCount += 1;
          stats[question.section].correct += 1;
        }
      }

      const baseResult = placementResult(correctCount, questions.length, stats);

      const scorer = getAiScorer();
      let writingFeedback;
      let speakingFeedback;

      try {
        [writingFeedback, speakingFeedback] = await Promise.all([
          scorer.scoreWriting({
            prompt: writingTask.prompt,
            taskType: writingTask.taskType === 'TASK_1' ? 'TASK_1' : 'TASK_2',
            minWords: writingTask.minWords,
            text: writingText,
          }),
          scorer.scoreSpeaking({
            topic: speakingPrompt.topic,
            part: speakingPrompt.part === 'PART_2' || speakingPrompt.part === 'PART_3' ? speakingPrompt.part : 'PART_1',
            questions: speakingPrompt.questions,
            transcript: speakingTranscript,
            durationSeconds: speakingPrompt.speakTimeSeconds,
          }),
        ]);
      } catch (error) {
        request.log.error({ err: error, sessionId: id }, 'Placement AI scoring failed');
        throw new AppError(502, 'AI_SCORING_FAILED', 'AI scoring failed. Please try again.');
      }

      const finalBand = roundBand(
        (baseResult.estimatedBand + writingFeedback.overallBand + speakingFeedback.overallBand) / 3,
      );
      const finalCefr = placementCefrForBand(finalBand);
      const result = {
        ...baseResult,
        estimatedBand: finalBand,
        cefrLevel: finalCefr,
      };
      const finishedAt = new Date();

      const updated = await app.prisma.placementSession.update({
        where: { id: session.id },
        data: {
          status: PlacementStatus.FINISHED,
          answersJson: answers as unknown as Prisma.InputJsonValue,
          writingText,
          speakingTranscript,
          writingBand: writingFeedback.overallBand,
          speakingBand: speakingFeedback.overallBand,
          writingFeedbackJson: writingFeedback as unknown as Prisma.InputJsonValue,
          speakingFeedbackJson: speakingFeedback as unknown as Prisma.InputJsonValue,
          correctCount: result.correct,
          totalCount: result.total,
          percentage: result.percentage,
          cefrLevel: result.cefrLevel,
          estimatedBand: result.estimatedBand,
          weakSkill: result.weakSkill,
          sectionStatsJson: result.sectionStats as unknown as Prisma.InputJsonValue,
          finishedAt,
        },
      });

      await app.prisma.profile.upsert({
        where: { userId: request.user.sub },
        create: {
          userId: request.user.sub,
          displayName: 'Learner',
          cefrLevel: result.cefrLevel,
          onboardingDone: true,
        },
        update: {
          cefrLevel: result.cefrLevel,
          onboardingDone: true,
        },
      });

      return { session: serializeSession(updated) };
    },
  );
};
