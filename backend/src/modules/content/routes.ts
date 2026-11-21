import type { FastifyPluginAsync } from 'fastify';
import { Skill } from '@prisma/client';
import { z } from 'zod';
import { checkExerciseAnswer, type ExerciseAnswer } from '../../lib/exercise.js';
import { AppError } from '../../lib/errors.js';
import { parseInput } from '../../lib/validation.js';

const listUnitsQuerySchema = z.object({
  skill: z.nativeEnum(Skill).optional(),
  level: z.string().trim().min(1).max(10).optional(),
});

const unitParamsSchema = z.object({
  slug: z.string().trim().min(1).max(120),
});

const exerciseParamsSchema = z.object({
  id: z.string().uuid(),
});

const exerciseAnswerSchema = z.object({
  // MCQ sends the option index, gap-fill / short-answer send text.
  answer: z.union([
    z.number().int().min(0).max(50),
    z.string().trim().min(1).max(2_000),
  ]),
});

export const contentRoutes: FastifyPluginAsync = async (app) => {
  app.get('/content/units', async (request) => {
    const query = parseInput(listUnitsQuerySchema, request.query, 'Invalid query');

    const units = await app.prisma.unit.findMany({
      where: {
        published: true,
        ...(query.skill ? { skill: query.skill } : {}),
        ...(query.level ? { cefrLevel: query.level.toUpperCase() } : {}),
      },
      orderBy: [{ skill: 'asc' }, { order: 'asc' }],
      include: {
        _count: { select: { lessons: true } },
      },
    });

    return {
      units: units.map((unit) => ({
        id: unit.id,
        slug: unit.slug,
        title: unit.title,
        description: unit.description,
        skill: unit.skill,
        cefrLevel: unit.cefrLevel,
        order: unit.order,
        lessonCount: unit._count.lessons,
      })),
    };
  });

  app.get('/content/units/:slug', async (request) => {
    const { slug } = parseInput(unitParamsSchema, request.params, 'Invalid unit id');

    const unit = await app.prisma.unit.findFirst({
      where: { slug, published: true },
      include: {
        lessons: {
          where: { published: true },
          orderBy: { order: 'asc' },
          include: {
            exercises: {
              orderBy: { order: 'asc' },
            },
          },
        },
      },
    });

    if (!unit) {
      throw AppError.notFound('Unit not found');
    }

    return {
      unit: {
        id: unit.id,
        slug: unit.slug,
        title: unit.title,
        description: unit.description,
        skill: unit.skill,
        cefrLevel: unit.cefrLevel,
        order: unit.order,
        lessons: unit.lessons.map((lesson) => ({
          id: lesson.id,
          slug: lesson.slug,
          title: lesson.title,
          type: lesson.type,
          order: lesson.order,
          estimatedMinutes: lesson.estimatedMinutes,
          contentJson: lesson.contentJson,
          // Answer keys and explanations stay server-side until the learner
          // submits an answer through POST /content/exercises/:id/answer.
          exercises: lesson.exercises.map((exercise) => ({
            id: exercise.id,
            type: exercise.type,
            order: exercise.order,
            prompt: exercise.prompt,
            optionsJson: exercise.optionsJson,
          })),
        })),
      },
    };
  });

  app.post(
    '/content/exercises/:id/answer',
    { preHandler: [app.authenticate] },
    async (request) => {
      const { id } = parseInput(exerciseParamsSchema, request.params, 'Invalid exercise id');
      const { answer } = parseInput(exerciseAnswerSchema, request.body, 'Invalid answer');

      const exercise = await app.prisma.exercise.findFirst({
        where: {
          id,
          lesson: { published: true, unit: { published: true } },
        },
      });

      if (!exercise) {
        throw AppError.notFound('Exercise not found');
      }

      const result = checkExerciseAnswer(
        exercise.type,
        exercise.answerJson,
        answer as ExerciseAnswer,
      );

      // Persist auto-gradable attempts so /v1/progress can report real accuracy.
      // Open short answers return null and are excluded from the accuracy stat.
      if (result.isCorrect !== null) {
        await app.prisma.exerciseAttempt.create({
          data: {
            userId: request.user.sub,
            exerciseId: exercise.id,
            isCorrect: result.isCorrect,
            answerJson: answer,
          },
        });
      }

      return {
        result: {
          isCorrect: result.isCorrect,
          correctAnswer: result.correctAnswer,
          explanation: exercise.explanation,
        },
      };
    },
  );
};
