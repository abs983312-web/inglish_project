import { describe, expect, it } from 'vitest';
import {
  hashIdempotentRequest,
  isValidIdempotencyKey,
} from '../src/lib/idempotency.js';

describe('idempotency keys', () => {
  it('accepts URL-safe keys of 8-128 characters', () => {
    expect(isValidIdempotencyKey('abc-123_X.Y:z')).toBe(true);
    expect(isValidIdempotencyKey('a'.repeat(128))).toBe(true);
  });

  it('rejects short, long, empty or unsafe keys', () => {
    expect(isValidIdempotencyKey('short')).toBe(false);
    expect(isValidIdempotencyKey('a'.repeat(129))).toBe(false);
    expect(isValidIdempotencyKey('')).toBe(false);
    expect(isValidIdempotencyKey('has spaces')).toBe(false);
    expect(isValidIdempotencyKey('emoji-😀')).toBe(false);
    expect(isValidIdempotencyKey('slash/inside')).toBe(false);
  });

  it('hashes the same body deterministically', () => {
    const body = { answers: [{ questionId: 'a', answerIndex: 1 }], writingText: 'hi' };
    expect(hashIdempotentRequest(body)).toBe(hashIdempotentRequest(body));
    expect(hashIdempotentRequest(body)).toHaveLength(64);
  });

  it('distinguishes different bodies and null', () => {
    const base = hashIdempotentRequest({ rating: 1 });
    expect(hashIdempotentRequest({ rating: 2 })).not.toBe(base);
    expect(hashIdempotentRequest(undefined)).toBe(hashIdempotentRequest(null));
    expect(hashIdempotentRequest(undefined)).not.toBe(base);
  });
});
