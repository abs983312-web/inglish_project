import { createHash, randomBytes } from 'node:crypto';

/** Opaque refresh token sent to the client. Never stored in plain text. */
export function generateRefreshToken(): string {
  return randomBytes(48).toString('base64url');
}

export function hashToken(token: string): string {
  return createHash('sha256').update(token).digest('hex');
}

export function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setUTCDate(copy.getUTCDate() + days);
  return copy;
}

/** Converts "15m", "1h", "30d" to seconds. Falls back to 900 on invalid input. */
export function ttlToSeconds(ttl: string): number {
  const match = /^(\d+)\s*([smhd])$/i.exec(ttl.trim());
  if (!match) return 900;

  const value = Number(match[1]);
  const unit = match[2]?.toLowerCase();
  const multiplier = unit === 'd' ? 86_400 : unit === 'h' ? 3_600 : unit === 'm' ? 60 : 1;
  return value * multiplier;
}
