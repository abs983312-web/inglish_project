import nodemailer, { type Transporter } from 'nodemailer';
import { env } from '../config/env.js';

export interface EmailMessage {
  to: string;
  subject: string;
  text: string;
  sentAt: string;
}

const OUTBOX_LIMIT = 50;
const outbox: EmailMessage[] = [];

let transporter: Transporter | null = null;

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

/**
 * `auto` uses SMTP when it is configured, otherwise logs. Production refuses to
 * start in log mode (see app.ts) so verification codes are never silently
 * dropped.
 */
export function emailMode(): 'log' | 'smtp' {
  if (env.EMAIL_MODE === 'log') return 'log';
  if (env.EMAIL_MODE === 'smtp') return 'smtp';
  return env.SMTP_HOST ? 'smtp' : 'log';
}

function getTransporter(): Transporter {
  if (transporter) return transporter;

  if (!env.SMTP_HOST) {
    throw new Error('SMTP_HOST is required when EMAIL_MODE=smtp');
  }

  transporter = nodemailer.createTransport({
    host: env.SMTP_HOST,
    port: env.SMTP_PORT,
    secure: env.SMTP_SECURE,
    auth:
      env.SMTP_USER && env.SMTP_PASS
        ? { user: env.SMTP_USER, pass: env.SMTP_PASS }
        : undefined,
  });

  return transporter;
}

export async function sendEmail(input: {
  to: string;
  subject: string;
  text: string;
}): Promise<void> {
  const message: EmailMessage = {
    to: normalizeEmail(input.to),
    subject: input.subject,
    text: input.text,
    sentAt: new Date().toISOString(),
  };

  // The in-memory outbox powers `GET /v1/dev/outbox` in development and is
  // capped so a long-running dev server cannot leak memory.
  outbox.push(message);
  if (outbox.length > OUTBOX_LIMIT) {
    outbox.shift();
  }

  if (emailMode() === 'log') {
    // eslint-disable-next-line no-console
    console.info(
      `[email:log] to=${message.to} subject="${message.subject}"\n${message.text}`,
    );
    return;
  }

  await getTransporter().sendMail({
    from: env.EMAIL_FROM,
    to: message.to,
    subject: message.subject,
    text: message.text,
  });
}

/** Dev-only helper: recent messages addressed to one email. */
export function getOutboxFor(email: string): EmailMessage[] {
  const target = normalizeEmail(email);
  return outbox
    .filter((message) => message.to === target)
    .slice(-10)
    .reverse();
}
