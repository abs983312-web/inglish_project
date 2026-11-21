const base = process.env.API_BASE_URL ?? 'http://127.0.0.1:3000';

async function request(path, options = {}) {
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: {
      'content-type': 'application/json',
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
  if (!condition) {
    throw new Error(message);
  }
}

const email = `smoke-${Date.now()}@example.com`;
const password = 'StrongPass123!';

console.log('1. health');
const health = await request('/health');
assert(health.status === 200 && health.body?.status === 'ok', 'health failed');

console.log('2. register');
const register = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, displayName: 'Smoke Test' }),
});
assert(register.status === 201, `register failed: ${register.status}`);
assert(register.body.accessToken && register.body.refreshToken, 'tokens missing');

console.log('3. me');
const me = await request('/v1/me', {
  headers: { authorization: `Bearer ${register.body.accessToken}` },
});
assert(me.status === 200 && me.body.user.email === email, 'me failed');

console.log('4. refresh rotation');
const refreshed = await request('/v1/auth/refresh', {
  method: 'POST',
  body: JSON.stringify({ refreshToken: register.body.refreshToken }),
});
assert(refreshed.status === 200, `refresh failed: ${refreshed.status}`);
assert(refreshed.body.refreshToken !== register.body.refreshToken, 'token was not rotated');

console.log('5. old refresh token must be rejected');
const reused = await request('/v1/auth/refresh', {
  method: 'POST',
  body: JSON.stringify({ refreshToken: register.body.refreshToken }),
});
assert(reused.status === 401, `old refresh token still works: ${reused.status}`);

console.log('6. logout');
const logout = await request('/v1/auth/logout', {
  method: 'POST',
  body: JSON.stringify({ refreshToken: refreshed.body.refreshToken }),
});
assert(logout.status === 204, `logout failed: ${logout.status}`);

console.log('7. refresh after logout must be rejected');
const afterLogout = await request('/v1/auth/refresh', {
  method: 'POST',
  body: JSON.stringify({ refreshToken: refreshed.body.refreshToken }),
});
assert(afterLogout.status === 401, `refresh after logout still works: ${afterLogout.status}`);

console.log('8. login');
const login = await request('/v1/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password }),
});
assert(login.status === 200, `login failed: ${login.status}`);

console.log('9. wrong password');
const wrong = await request('/v1/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password: 'wrong-password' }),
});
assert(wrong.status === 401, `wrong password returned: ${wrong.status}`);

console.log(`\nAll auth smoke checks passed for ${email}`);
