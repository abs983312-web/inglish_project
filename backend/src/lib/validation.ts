import type { z } from 'zod';
import { AppError } from './errors.js';

export function parseInput<T>(schema: z.ZodType<T>, value: unknown, message = 'Invalid request'): T {
  const result = schema.safeParse(value);
  if (!result.success) {
    throw AppError.badRequest('VALIDATION_ERROR', message, result.error.flatten());
  }
  return result.data;
}
