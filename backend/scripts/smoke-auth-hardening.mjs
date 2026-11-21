/**
 * End-to-end smoke test for account security:
 * email verification codes, password reset codes, wrong-code rejection,
 * session revocation after a password change, and no account enumeration.
 *
 * Uses the development-only `GET /v1/dev/outbox` endpoint, which returns only
 * messages addressed to the authenticated user.
 *
 * Run against a seeded dev server:
 *   npm run smoke:auth:hardening
 */
const base = process.env.API_BASE_URL ?? 'http://127.0.0.1:3000';

async function request(path, options = {}) {
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'content-type': 'application/json' } : {}),
      ...(options.headers ?? {}),
    },
  });

  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  return { status: response.status, body };
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function waitForHealth() {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    try {
      const health = await request('/health');
      if (health.status === 200) return;
    } catch {
      // not ready yet
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error('API did not become healthy');
}

function extractCode(message) {
  const match = message.text.match(/\b(\d{6})\b/);
  assert(match, `no 6-digit code in email: ${message.subject}`);
  return match[1];
}

async function latestCode(headers, subjectFragment) {
  const outbox = await request('/v1/dev/outbox', { headers });
  assert(outbox.status === 200, `outbox failed: ${outbox.status}`);
  const message = outbox.body.messages.find((item) =>
    item.subject.includes(subjectFragment),
  );
  assert(message, `no "${subjectFragment}" email in outbox`);
  return extractCode(message);
}

await waitForHealth();

console.log('1. register and read the automatic verification email');
const email = `auth-hardening-${Date.now()}@example.com`;
const oldPassword = 'StrongPass123!';
const newPassword = 'NewStrongPass456!';

const registered = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password: oldPassword, displayName: 'Hardening' }),
});
assert(
  registered.status === 201,
  `register failed: ${registered.status} ${JSON.stringify(registered.body)}`,
);
const headers = { authorization: `Bearer ${registered.body.accessToken}` };
const refreshToken = registered.body.refreshToken;
assert(registered.body.user.emailVerified === false, 'new user must start unverified');

console.log('2. request a fresh verification code');
const requested = await request('/v1/auth/verify-email/request', {
  method: 'POST',
  headers,
});
assert(requested.status === 200, `verify request failed: ${requested.status}`);
assert(requested.body.alreadyVerified === false, 'expected alreadyVerified=false');

const verifyCode = await latestCode(headers, 'emailni tasdiqlang');
assert(/^\d{6}$/.test(verifyCode), 'verification code must be 6 digits');

console.log('3. a wrong code is rejected');
const wrongVerifyCode = verifyCode === '000000' ? '111111' : '000000';
const wrong = await request('/v1/auth/verify-email/confirm', {
  method: 'POST',
  headers,
  body: JSON.stringify({ code: wrongVerifyCode }),
});
assert(
  wrong.status === 400 && wrong.body.code === 'CODE_INVALID',
  `expected CODE_INVALID, got ${wrong.status} ${JSON.stringify(wrong.body)}`,
);

console.log('4. the correct code verifies the email');
const confirmed = await request('/v1/auth/verify-email/confirm', {
  method: 'POST',
  headers,
  body: JSON.stringify({ code: verifyCode }),
});
assert(confirmed.status === 200, `confirm failed: ${confirmed.status}`);
assert(confirmed.body.emailVerified === true, 'expected emailVerified=true');

const me = await request('/v1/me', { headers });
assert(me.body.user.emailVerified === true, '/me must show emailVerified=true');

console.log('5. requesting again reports alreadyVerified');
const again = await request('/v1/auth/verify-email/request', {
  method: 'POST',
  headers,
});
assert(again.status === 200 && again.body.alreadyVerified === true, 'expected alreadyVerified=true');

console.log('6. request a password reset code');
const forgot = await request('/v1/auth/password/forgot', {
  method: 'POST',
  body: JSON.stringify({ email }),
});
assert(forgot.status === 204, `forgot failed: ${forgot.status}`);

const resetCode = await latestCode(headers, 'parolni tiklash');
assert(/^\d{6}$/.test(resetCode), 'reset code must be 6 digits');

console.log('7. a wrong reset code is rejected');
const wrongReset = await request('/v1/auth/password/reset', {
  method: 'POST',
  body: JSON.stringify({
    email,
    code: resetCode === '000000' ? '111111' : '000000',
    password: newPassword,
  }),
});
assert(
  wrongReset.status === 400 && wrongReset.body.code === 'CODE_INVALID',
  `expected CODE_INVALID, got ${wrongReset.status}`,
);

console.log('8. the correct reset code changes the password');
const reset = await request('/v1/auth/password/reset', {
  method: 'POST',
  body: JSON.stringify({ email, code: resetCode, password: newPassword }),
});
assert(reset.status === 204, `reset failed: ${reset.status} ${JSON.stringify(reset.body)}`);

console.log('9. the old password no longer works');
const oldLogin = await request('/v1/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password: oldPassword }),
});
assert(oldLogin.status === 401, `expected 401 for the old password, got ${oldLogin.status}`);

console.log('10. the new password works');
const newLogin = await request('/v1/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password: newPassword }),
});
assert(newLogin.status === 200, `expected 200 for the new password, got ${newLogin.status}`);

console.log('11. a password change revokes existing refresh tokens');
const staleRefresh = await request('/v1/auth/refresh', {
  method: 'POST',
  body: JSON.stringify({ refreshToken }),
});
assert(staleRefresh.status === 401, `expected 401 for the stale refresh token, got ${staleRefresh.status}`);

console.log('12. forgot-password does not reveal unknown accounts');
const unknown = await request('/v1/auth/password/forgot', {
  method: 'POST',
  body: JSON.stringify({ email: `nobody-${Date.now()}@example.com` }),
});
assert(unknown.status === 204, `expected 204 for an unknown email, got ${unknown.status}`);

console.log('13. clean up');
const deleted = await request('/v1/me', {
  method: 'DELETE',
  headers: { authorization: `Bearer ${newLogin.body.accessToken}` },
});
assert(deleted.status === 204, `delete failed: ${deleted.status}`);

console.log('\nAuth hardening smoke passed.');
