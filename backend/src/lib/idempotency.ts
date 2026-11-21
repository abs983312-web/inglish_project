import { createHash } from 'node:crypto';

/** URL-safe keys only; long enough to avoid collisions, short enough to store. */
export const IDEMPOTENCY_KEY_PATTERN = /^[A-Za-z0-9._:-]{8,128}$/;

export function isValidIdempotencyKey(value: string): boolean {
  return IDEMPOTENCY_KEY_PATTERN.test(value);
}

/**
 * Stable hash of the parsed request body. Key order follows the JSON the
 * client sent, so the same logical request must be re-sent byte-for-byte.
 */
export function hashIdempotentRequest(body: unknown): string {
  return createHash('sha256').update(JSON.stringify(body ?? null)).digest('hex');
}
