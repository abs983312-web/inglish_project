import { describe, expect, it } from 'vitest';
import {
  BATTLE_BASE_POINTS,
  BATTLE_SPEED_MAX_POINTS,
  BATTLE_STREAK_BONUS,
  BATTLE_TIME_LIMIT_MS,
  battlePhase,
  botDecision,
  hashSeed,
  isValidRoomCode,
  normalizeRoomCode,
  scoreAnswer,
} from '../src/lib/battle.js';

describe('battle scoring', () => {
  it('gives no points for a wrong answer', () => {
    expect(scoreAnswer({ isCorrect: false, responseMs: 100, streak: 5 })).toBe(0);
  });

  it('rewards instant answers with the maximum speed bonus', () => {
    expect(scoreAnswer({ isCorrect: true, responseMs: 0, streak: 1 })).toBe(
      BATTLE_BASE_POINTS + BATTLE_SPEED_MAX_POINTS,
    );
  });

  it('gives only the base points at the time limit', () => {
    expect(
      scoreAnswer({
        isCorrect: true,
        responseMs: BATTLE_TIME_LIMIT_MS,
        streak: 1,
      }),
    ).toBe(BATTLE_BASE_POINTS);
  });

  it('clamps out-of-range response times', () => {
    expect(scoreAnswer({ isCorrect: true, responseMs: -500, streak: 1 })).toBe(
      BATTLE_BASE_POINTS + BATTLE_SPEED_MAX_POINTS,
    );
    expect(
      scoreAnswer({ isCorrect: true, responseMs: 60_000, streak: 1 }),
    ).toBe(BATTLE_BASE_POINTS);
  });

  it('adds a streak bonus from the third consecutive correct answer', () => {
    const fast = { isCorrect: true, responseMs: 0 };
    expect(scoreAnswer({ ...fast, streak: 2 })).toBe(
      BATTLE_BASE_POINTS + BATTLE_SPEED_MAX_POINTS,
    );
    expect(scoreAnswer({ ...fast, streak: 3 })).toBe(
      BATTLE_BASE_POINTS + BATTLE_SPEED_MAX_POINTS + BATTLE_STREAK_BONUS,
    );
  });
});

describe('battle bot', () => {
  it('is deterministic for the same room and position', () => {
    const first = botDecision('room-1', 3, 4, 2);
    const second = botDecision('room-1', 3, 4, 2);
    expect(second).toEqual(first);
  });

  it('always returns an index inside the option range', () => {
    for (let position = 1; position <= 50; position += 1) {
      const decision = botDecision(`room-${position}`, position, 4, 1);
      expect(decision.answerIndex).toBeGreaterThanOrEqual(0);
      expect(decision.answerIndex).toBeLessThan(4);
      expect(decision.isCorrect).toBe(decision.answerIndex === 1);
    }
  });

  it('never picks the correct index when it is meant to be wrong', () => {
    for (let position = 1; position <= 200; position += 1) {
      const decision = botDecision('wrong-check', position, 4, 2);
      if (!decision.isCorrect) {
        expect(decision.answerIndex).not.toBe(2);
      }
    }
  });

  it('produces a positive, human-like response time', () => {
    const decision = botDecision('room-2', 1, 4, 0);
    expect(decision.responseMs).toBeGreaterThanOrEqual(2_500);
    expect(decision.responseMs).toBeLessThan(8_000);
  });

  it('hashes different inputs to different seeds', () => {
    expect(hashSeed('a')).not.toBe(hashSeed('b'));
  });
});

describe('battle rooms', () => {
  it('normalises a pasted 6-digit code', () => {
    expect(normalizeRoomCode(' 12 34-56 ')).toBe('123456');
    expect(normalizeRoomCode('123456789')).toBe('123456');
  });

  it('only accepts exactly six digits', () => {
    expect(isValidRoomCode('123456')).toBe(true);
    expect(isValidRoomCode('12345')).toBe(false);
    expect(isValidRoomCode('12345a')).toBe(false);
    expect(isValidRoomCode('1234567')).toBe(false);
  });

  it('keeps the reveal phase until the engine clears it', () => {
    const now = new Date('2026-01-01T00:00:00.000Z');
    expect(battlePhase('LOBBY', null, now)).toBe('LOBBY');
    expect(battlePhase('ACTIVE', null, now)).toBe('QUESTION');
    expect(
      battlePhase('ACTIVE', new Date(now.getTime() + 1_000), now),
    ).toBe('REVEAL');
    expect(
      battlePhase('ACTIVE', new Date(now.getTime() - 1_000), now),
    ).toBe('REVEAL');
    expect(battlePhase('FINISHED', null, now)).toBe('FINISHED');
  });
});
