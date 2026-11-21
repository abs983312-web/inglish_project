import { createHmac, randomInt, timingSafeEqual } from 'node:crypto';
import { env } from '../config/env.js';

/** Six-digit numeric code, zero-padded (000000–999999). */
export function generateAuthCode(): string {
  return randomInt(0, 1_000_000).toString().padStart(6, '0');
}

/**
 * Codes are short, so they are never stored in plaintext. The HMAC binds the
 * code to the user and purpose, which prevents a leaked database row from being
 * replayed against another account.
 */
export function hashAuthCode(userId: string, purpose: string, code: string): string {
  return createHmac('sha256', env.JWT_SECRET)
    .update(`${userId}:${purpose}:${code}`)
    .digest('hex');
}

/** Constant-time comparison of two hex digests. */
export function authCodeMatches(expectedHash: string, actualHash: string): boolean {
  const expected = Buffer.from(expectedHash, 'hex');
  const actual = Buffer.from(actualHash, 'hex');
  if (expected.length !== actual.length || expected.length === 0) {
    return false;
  }
  return timingSafeEqual(expected, actual);
}
