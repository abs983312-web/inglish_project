export const BATTLE_TIME_LIMIT_MS = 20_000;
export const BATTLE_BASE_POINTS = 500;
export const BATTLE_SPEED_MAX_POINTS = 500;
export const BATTLE_STREAK_BONUS = 100;
export const BATTLE_STREAK_THRESHOLD = 3;
export const BATTLE_BOT_ACCURACY_PERCENT = 65;

export const BATTLE_MIN_QUESTIONS = 5;
export const BATTLE_MAX_QUESTIONS = 20;
export const BATTLE_QUESTION_COUNTS = [10, 15, 20] as const;
export const BATTLE_TIME_LIMITS_MS = [10_000, 20_000, 30_000] as const;
/** How long the correct answer + scoreboard stay on screen between questions. */
export const BATTLE_REVEAL_MS = 4_000;
/** A friend room that is still empty after this delay gets a bot opponent. */
export const BATTLE_AUTO_BOT_DELAY_MS = 15_000;
/** Lobby rooms older than this are considered stale. */
export const BATTLE_LOBBY_TTL_MS = 2 * 60 * 60 * 1000;
export const BATTLE_CODE_LENGTH = 6;
export const BATTLE_MAX_PLAYERS = {
  FREE: 10,
  PRO: 10,
  TEACHER: 30,
} as const;

export type BattlePhase = 'LOBBY' | 'QUESTION' | 'REVEAL' | 'FINISHED';

export function battlePhase(
  status: 'LOBBY' | 'ACTIVE' | 'FINISHED',
  revealEndsAt: Date | null,
  now: Date = new Date(),
): BattlePhase {
  if (status === 'LOBBY') return 'LOBBY';
  if (status === 'FINISHED') return 'FINISHED';
  // Keep showing the reveal until the engine advances to the next question.
  // advanceAfterRevealLocked clears revealEndsAt at the same time.
  if (revealEndsAt) return 'REVEAL';
  return 'QUESTION';
}

export function normalizeRoomCode(value: string): string {
  return value.replace(/\D/g, '').slice(0, BATTLE_CODE_LENGTH);
}

export function isValidRoomCode(value: string): boolean {
  return new RegExp(`^\\d{${BATTLE_CODE_LENGTH}}$`).test(value);
}

export type ScoreInput = {
  isCorrect: boolean;
  responseMs: number;
  /** Consecutive correct answers including this one. */
  streak: number;
  timeLimitMs?: number;
};

/**
 * Kahoot-style scoring: a correct answer is worth 500 points, with up to 500
 * extra for speed and 100 for a streak of three or more.
 */
export function scoreAnswer({
  isCorrect,
  responseMs,
  streak,
  timeLimitMs = BATTLE_TIME_LIMIT_MS,
}: ScoreInput): number {
  if (!isCorrect) return 0;

  const limit = Math.max(timeLimitMs, 1);
  const clamped = Math.min(Math.max(responseMs, 0), limit);
  const speedBonus = Math.round(BATTLE_SPEED_MAX_POINTS * (1 - clamped / limit));
  const streakBonus = streak >= BATTLE_STREAK_THRESHOLD ? BATTLE_STREAK_BONUS : 0;

  return BATTLE_BASE_POINTS + speedBonus + streakBonus;
}

/** Stable 32-bit FNV-1a hash, used to make bot behaviour reproducible per round. */
export function hashSeed(value: string): number {
  let hash = 0x811c9dc5;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash >>> 0;
}

export type BotDecision = {
  isCorrect: boolean;
  answerIndex: number;
  responseMs: number;
};

/**
 * Deterministic bot so a room can be replayed and tested: the same room and
 * question always produce the same answer and response time.
 */
export function botDecision(
  roomId: string,
  position: number,
  optionCount: number,
  correctIndex: number,
): BotDecision {
  const seed = hashSeed(`${roomId}:${position}`);
  const isCorrect = seed % 100 < BATTLE_BOT_ACCURACY_PERCENT;
  const answerIndex = isCorrect
    ? correctIndex
    : wrongIndex(seed, correctIndex, optionCount);
  const responseMs = 2_500 + (hashSeed(`${roomId}:${position}:ms`) % 5_500);

  return { isCorrect, answerIndex, responseMs };
}

function wrongIndex(seed: number, correctIndex: number, optionCount: number): number {
  if (optionCount <= 1) return correctIndex;
  const offset = 1 + (seed % (optionCount - 1));
  return (correctIndex + offset) % optionCount;
}
