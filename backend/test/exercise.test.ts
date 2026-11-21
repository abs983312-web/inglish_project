import { ExerciseType } from '@prisma/client';
import { describe, expect, it } from 'vitest';
import { checkExerciseAnswer, normalizeAnswerText } from '../src/lib/exercise.js';

describe('checkExerciseAnswer', () => {
  it('grades a multiple-choice answer by index', () => {
    const answerJson = { correctIndex: 2 };

    expect(checkExerciseAnswer(ExerciseType.MULTIPLE_CHOICE, answerJson, 2)).toEqual({
      isCorrect: true,
      correctAnswer: 2,
    });
    expect(checkExerciseAnswer(ExerciseType.MULTIPLE_CHOICE, answerJson, 0).isCorrect).toBe(false);
    expect(checkExerciseAnswer(ExerciseType.MULTIPLE_CHOICE, answerJson, '2').isCorrect).toBe(
      false,
    );
  });

  it('accepts gap-fill answers regardless of case, spacing, and curly quotes', () => {
    const answerJson = { accepted: ['Nevertheless', 'However'] };

    expect(checkExerciseAnswer(ExerciseType.GAP_FILL, answerJson, '  however ').isCorrect).toBe(
      true,
    );
    expect(
      checkExerciseAnswer(ExerciseType.GAP_FILL, answerJson, 'NEVERTHELESS').isCorrect,
    ).toBe(true);
    expect(checkExerciseAnswer(ExerciseType.GAP_FILL, answerJson, 'therefore').isCorrect).toBe(
      false,
    );
  });

  it('normalizes curly apostrophes in gap-fill answers', () => {
    const answerJson = { accepted: ["it's"] };
    expect(checkExerciseAnswer(ExerciseType.GAP_FILL, answerJson, 'it’s').isCorrect).toBe(true);
    expect(normalizeAnswerText('  It’s   fine ')).toBe("it's fine");
  });

  it('returns the model answer for open short answers without grading them', () => {
    const answerJson = { sample: 'A clear thesis statement.' };

    expect(checkExerciseAnswer(ExerciseType.SHORT_ANSWER, answerJson, 'anything')).toEqual({
      isCorrect: null,
      correctAnswer: 'A clear thesis statement.',
    });
  });

  it('degrades gracefully when an answer key is malformed', () => {
    expect(checkExerciseAnswer(ExerciseType.MULTIPLE_CHOICE, {}, 1)).toEqual({
      isCorrect: null,
      correctAnswer: null,
    });
    expect(checkExerciseAnswer(ExerciseType.GAP_FILL, { accepted: [] }, 'x')).toEqual({
      isCorrect: null,
      correctAnswer: null,
    });
  });
});
