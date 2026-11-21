import { describe, expect, it } from 'vitest';
import {
  authCodeMatches,
  generateAuthCode,
  hashAuthCode,
} from '../src/lib/auth-codes.js';

const USER_ID = '11111111-1111-1111-1111-111111111111';

describe('auth codes', () => {
  it('generates a zero-padded 6-digit code', () => {
    for (let index = 0; index < 200; index += 1) {
      expect(generateAuthCode()).toMatch(/^\d{6}$/);
    }
  });

  it('hashes deterministically for the same user, purpose and code', () => {
    const first = hashAuthCode(USER_ID, 'EMAIL_VERIFY', '123456');
    const second = hashAuthCode(USER_ID, 'EMAIL_VERIFY', '123456');
    expect(first).toBe(second);
    expect(first).toHaveLength(64);
  });

  it('binds the hash to the user, purpose and code', () => {
    const base = hashAuthCode(USER_ID, 'EMAIL_VERIFY', '123456');
    expect(hashAuthCode(USER_ID, 'PASSWORD_RESET', '123456')).not.toBe(base);
    expect(
      hashAuthCode('22222222-2222-2222-2222-222222222222', 'EMAIL_VERIFY', '123456'),
    ).not.toBe(base);
    expect(hashAuthCode(USER_ID, 'EMAIL_VERIFY', '654321')).not.toBe(base);
  });

  it('compares matching and non-matching hashes', () => {
    const stored = hashAuthCode(USER_ID, 'EMAIL_VERIFY', '123456');
    expect(authCodeMatches(stored, hashAuthCode(USER_ID, 'EMAIL_VERIFY', '123456'))).toBe(
      true,
    );
    expect(authCodeMatches(stored, hashAuthCode(USER_ID, 'EMAIL_VERIFY', '000000'))).toBe(
      false,
    );
  });

  it('rejects malformed hashes instead of throwing', () => {
    expect(authCodeMatches('not-hex', 'also-not-hex')).toBe(false);
    expect(authCodeMatches('', '')).toBe(false);
  });
});
