import 'dotenv/config';
import { z } from 'zod';

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  HOST: z.string().default('0.0.0.0'),
  PORT: z.coerce.number().int().positive().default(3000),
  DATABASE_URL: z.string().min(1, 'DATABASE_URL is required'),
  JWT_SECRET: z
    .string()
    .min(32, 'JWT_SECRET must be at least 32 characters')
    .refine(
      (value) => !value.startsWith('replace-with-'),
      'JWT_SECRET must be replaced with a real random value',
    ),
  CORS_ORIGIN: z.string().default('*'),
  ACCESS_TOKEN_TTL: z
    .string()
    .regex(/^\d+\s*[smhd]$/i, 'ACCESS_TOKEN_TTL must look like 15m, 1h, or 30d')
    .default('15m'),
  REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().positive().default(30),
  TRUST_PROXY: z.enum(['true', 'false']).default('false').transform((value) => value === 'true'),
  GEMINI_API_KEY: z.string().optional(),
  GEMINI_MODEL: z.string().default('gemini-2.5-flash'),
  AI_SCORING_MODE: z.enum(['auto', 'mock', 'gemini']).default('auto'),
  // Billing: `auto` uses Google Play when credentials exist, otherwise mock.
  BILLING_MODE: z.enum(['auto', 'mock', 'google']).default('auto'),
  PLAY_PACKAGE_NAME: z.string().default('uz.inglish.app'),
  // Inline service-account JSON (preferred) or a path to the key file.
  GOOGLE_PLAY_SERVICE_ACCOUNT_JSON: z.string().optional(),
  GOOGLE_PLAY_SERVICE_ACCOUNT_FILE: z.string().optional(),
  // RTDN push authentication: either a shared bearer token or the OIDC audience.
  PLAY_RTDN_TOKEN: z.string().optional(),
  PLAY_RTDN_AUDIENCE: z.string().optional(),
  // Google OAuth Client ID (for verifying Google Sign-In tokens)
  GOOGLE_CLIENT_ID: z.string().optional(),
  GOOGLE_ANDROID_CLIENT_ID: z.string().optional(),
  // Transactional email: `auto` uses SMTP when configured, otherwise logs.
  EMAIL_MODE: z.enum(['auto', 'log', 'smtp']).default('auto'),
  EMAIL_FROM: z.string().default('Inglish <no-reply@inglish.uz>'),
  SMTP_HOST: z.string().optional(),
  SMTP_PORT: z.coerce.number().int().positive().default(587),
  SMTP_SECURE: z
    .enum(['true', 'false'])
    .default('false')
    .transform((value) => value === 'true'),
  SMTP_USER: z.string().optional(),
  SMTP_PASS: z.string().optional(),
  // Email verification / password reset codes.
  AUTH_CODE_TTL_MINUTES: z.coerce.number().int().positive().default(15),
  AUTH_CODE_MAX_ATTEMPTS: z.coerce.number().int().positive().default(5),
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  // eslint-disable-next-line no-console
  console.error('Invalid environment variables:', parsed.error.flatten().fieldErrors);
  process.exit(1);
}

export const env = parsed.data;
