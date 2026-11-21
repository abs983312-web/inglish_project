import { describe, expect, it } from 'vitest';
import { addDays, generateRefreshToken, hashToken, ttlToSeconds } from '../src/lib/tokens.js';

describe('tokens', () => {
  it('generates unique refresh tokens', () => {
    const tokens = new Set(Array.from({ length: 100 }, () => generateRefreshToken()));
    expect(tokens.size).toBe(100);
  });

  it('hashes tokens deterministically', () => {
    const token = 'example-refresh-token';
    expect(hashToken(token)).toBe(hashToken(token));
    expect(hashToken(token)).toHaveLength(64);
  });

  it('adds days in UTC', () => {
    const start = new Date('2026-01-01T00:00:00.000Z');
    expect(addDays(start, 30).toISOString()).toBe('2026-01-31T00:00:00.000Z');
  });

  it('parses TTL values', () => {
    expect(ttlToSeconds('15m')).toBe(900);
    expect(ttlToSeconds('1h')).toBe(3600);
    expect(ttlToSeconds('30d')).toBe(2_592_000);
    expect(ttlToSeconds('invalid')).toBe(900);
  });
});
