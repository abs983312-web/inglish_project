import type { ExerciseType, Prisma } from '@prisma/client';

export type ExerciseAnswer = string | number;

export type ExerciseCheckResult = {
  /**
   * `true` / `false` for auto-checkable exercises, `null` when the answer
   * cannot be graded without a teacher or AI (open short answers).
   */
  isCorrect: boolean | null;
  correctAnswer: Prisma.JsonValue;
};

function asRecord(value: Prisma.JsonValue): Record<string, Prisma.JsonValue> {
  if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, Prisma.JsonValue>;
  }
  return {};
}

/** Normalizes gap-fill answers so casing, spacing, and curly quotes do not matter. */
export function normalizeAnswerText(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[’‘]/gu, "'")
    .replace(/\s+/gu, ' ');
}

export function checkExerciseAnswer(
  type: ExerciseType,
  answerJson: Prisma.JsonValue,
  answer: ExerciseAnswer,
): ExerciseCheckResult {
  const data = asRecord(answerJson);

  if (type === 'MULTIPLE_CHOICE') {
    const correctIndex =
      typeof data.correctIndex === 'number' && Number.isInteger(data.correctIndex)
        ? data.correctIndex
        : null;

    if (correctIndex === null) {
      return { isCorrect: null, correctAnswer: null };
    }

    return {
      isCorrect: typeof answer === 'number' && answer === correctIndex,
      correctAnswer: correctIndex,
    };
  }

  if (type === 'GAP_FILL') {
    const accepted = Array.isArray(data.accepted)
      ? data.accepted.filter((item): item is string => typeof item === 'string')
      : [];

    if (accepted.length === 0) {
      return { isCorrect: null, correctAnswer: null };
    }

    const correctAnswer = accepted[0] ?? null;
    if (typeof answer !== 'string') {
      return { isCorrect: false, correctAnswer };
    }

    const normalized = normalizeAnswerText(answer);
    return {
      isCorrect: accepted.some((item) => normalizeAnswerText(item) === normalized),
      correctAnswer,
    };
  }

  // SHORT_ANSWER is open-ended: return the model answer for self-checking
  // instead of pretending the server can grade it reliably.
  const sample = typeof data.sample === 'string' ? data.sample : null;
  return { isCorrect: null, correctAnswer: sample };
}
