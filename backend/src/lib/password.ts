import { Algorithm, hash, verify } from '@node-rs/argon2';
import { env } from '../config/env.js';

const isTest = env.NODE_ENV === 'test';

const ARGON_OPTIONS = {
  algorithm: Algorithm.Argon2id,
  memoryCost: isTest ? 19_456 : 65_536,
  timeCost: isTest ? 2 : 3,
  parallelism: isTest ? 1 : 4,
  outputLen: 32,
} as const;

export function hashPassword(password: string): Promise<string> {
  return hash(password, ARGON_OPTIONS);
}

export function verifyPassword(passwordHash: string, password: string): Promise<boolean> {
  return verify(passwordHash, password, ARGON_OPTIONS);
}
