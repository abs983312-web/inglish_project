/**
 * End-to-end smoke test for Idempotency-Key replay.
 *
 * Uses vocabulary review because it is cheap, authenticated and mutating:
 * a replayed key must return the original response without applying the review
 * a second time.
 *
 * Run against a seeded dev server:
 *   npm run smoke:idempotency
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
  return { status: response.status, body, headers: response.headers };
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

await waitForHealth();

console.log('1. register a fresh user');
const email = `idempotency-${Date.now()}@example.com`;
const registered = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password: 'StrongPass123!', displayName: 'Idem' }),
});
assert(
  registered.status === 201,
  `register failed: ${registered.status} ${JSON.stringify(registered.body)}`,
);
const headers = { authorization: `Bearer ${registered.body.accessToken}` };

console.log('2. pick a vocabulary item');
const list = await request('/v1/vocabulary?limit=1', { headers });
assert(list.status === 200, `vocabulary list failed: ${list.status}`);
const item = list.body.items[0];
assert(item?.id, 'no vocabulary item returned');

const key = `smoke-${Date.now()}-${Math.random().toString(16).slice(2)}`;
const reviewPath = `/v1/vocabulary/${item.id}/review`;

console.log('3. first review with a key applies once');
const first = await request(reviewPath, {
  method: 'POST',
  headers: { ...headers, 'idempotency-key': key },
  body: JSON.stringify({ rating: 3 }),
});
assert(
  first.status === 200,
  `first review failed: ${first.status} ${JSON.stringify(first.body)}`,
);
assert(first.body.review.reps === 1, `expected reps=1, got ${first.body.review.reps}`);
assert(
  first.headers.get('idempotency-replayed') === null,
  'first response must not be marked as replayed',
);

console.log('4. replaying the same key returns the stored response');
const replay = await request(reviewPath, {
  method: 'POST',
  headers: { ...headers, 'idempotency-key': key },
  body: JSON.stringify({ rating: 3 }),
});
assert(replay.status === 200, `replay failed: ${replay.status}`);
assert(
  replay.headers.get('idempotency-replayed') === 'true',
  'replay must set Idempotency-Replayed: true',
);
assert(
  JSON.stringify(replay.body) === JSON.stringify(first.body),
  'replay must return the original body',
);
assert(replay.body.review.reps === 1, 'replay must not apply the review again');

console.log('5. the same key with a different body is rejected');
const conflict = await request(reviewPath, {
  method: 'POST',
  headers: { ...headers, 'idempotency-key': key },
  body: JSON.stringify({ rating: 1 }),
});
assert(
  conflict.status === 409 && conflict.body.code === 'IDEMPOTENCY_KEY_REUSED',
  `expected IDEMPOTENCY_KEY_REUSED, got ${conflict.status} ${JSON.stringify(conflict.body)}`,
);

console.log('6. malformed keys are rejected before touching the handler');
const invalid = await request(reviewPath, {
  method: 'POST',
  headers: { ...headers, 'idempotency-key': 'short' },
  body: JSON.stringify({ rating: 3 }),
});
assert(
  invalid.status === 400 && invalid.body.code === 'IDEMPOTENCY_KEY_INVALID',
  `expected IDEMPOTENCY_KEY_INVALID, got ${invalid.status} ${JSON.stringify(invalid.body)}`,
);

console.log('7. a new key applies the review again');
const second = await request(reviewPath, {
  method: 'POST',
  headers: { ...headers, 'idempotency-key': `${key}-2` },
  body: JSON.stringify({ rating: 3 }),
});
assert(second.status === 200, `second review failed: ${second.status}`);
assert(second.body.review.reps === 2, `expected reps=2, got ${second.body.review.reps}`);

console.log('8. requests without a key still work');
const noKey = await request(reviewPath, {
  method: 'POST',
  headers,
  body: JSON.stringify({ rating: 3 }),
});
assert(noKey.status === 200, `no-key review failed: ${noKey.status}`);
assert(noKey.body.review.reps === 3, `expected reps=3, got ${noKey.body.review.reps}`);

console.log('\nAll idempotency checks passed.');
