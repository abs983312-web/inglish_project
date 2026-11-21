/**
 * End-to-end smoke test for the Play Billing vertical slice in mock mode:
 * product catalog, entitlement, purchase verification, idempotency, restore,
 * cross-account replay protection, and RTDN lifecycle notifications.
 *
 * Requires BILLING_MODE=mock (the default when no Play service account exists).
 * Run against a seeded dev server:
 *   npm run smoke:billing
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

async function register(label) {
  const email = `billing-smoke-${label}-${Date.now()}@example.com`;
  const password = 'StrongPass123!';
  const response = await request('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName: `Billing ${label}` }),
  });
  assert(
    response.status === 201,
    `register failed: ${response.status} ${JSON.stringify(response.body)}`,
  );
  return { authorization: `Bearer ${response.body.accessToken}` };
}

function rtdnBody(notification) {
  return JSON.stringify({
    message: {
      data: Buffer.from(JSON.stringify(notification), 'utf8').toString('base64'),
      messageId: `smoke-${Date.now()}`,
      publishTime: new Date().toISOString(),
    },
    subscription: 'projects/inglish-smoke/subscriptions/rtdn',
  });
}

await waitForHealth();

console.log('1. register two users');
const auth = await register('owner');
const other = await register('other');

console.log('2. product catalog');
const catalog = await request('/v1/billing/products');
assert(catalog.status === 200, `catalog failed: ${catalog.status}`);
assert(catalog.body.products.length === 3, `expected 3 products, got ${catalog.body.products.length}`);
assert(
  catalog.body.products.every((product) => !('periodDays' in product)),
  'catalog must not expose internal fields',
);

console.log('3. new user is FREE');
const initial = await request('/v1/billing/entitlement', { headers: auth });
assert(initial.status === 200, `entitlement failed: ${initial.status}`);
assert(initial.body.entitlement.plan === 'FREE', 'new user should be FREE');

console.log('4. verify a mock PRO purchase');
const token = `mock:inglish_pro_monthly:30:${Date.now()}`;
const verified = await request('/v1/billing/verify', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ purchaseToken: token }),
});
assert(
  verified.status === 201,
  `verify failed: ${verified.status} ${JSON.stringify(verified.body)}`,
);
assert(verified.body.verified === true, 'expected verified=true');
assert(verified.body.purchase.mock === true, 'expected mock purchase');
assert(verified.body.entitlement.plan === 'PRO', 'expected PRO entitlement');
assert(
  new Date(verified.body.entitlement.expiresAt).getTime() > Date.now(),
  'expected a future expiry',
);

console.log('5. entitlement and quota reflect PRO');
const entitlement = await request('/v1/billing/entitlement', { headers: auth });
assert(entitlement.body.entitlement.plan === 'PRO', 'expected PRO after verify');
assert(entitlement.body.entitlement.limits.speakingPerDay === 15, 'expected PRO speaking limit');

const usage = await request('/v1/usage', { headers: auth });
assert(usage.body.usage.plan === 'PRO', 'expected usage plan PRO');
assert(usage.body.usage.speaking.limit === 15, 'expected speaking limit 15');
assert(usage.body.usage.writing.limit === 4, 'expected writing limit 4');

console.log('6. re-verifying the same token is idempotent');
const again = await request('/v1/billing/verify', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ purchaseToken: token }),
});
assert(again.status === 200, `expected 200 on re-verify, got ${again.status}`);
assert(again.body.entitlement.plan === 'PRO', 'expected PRO on re-verify');

console.log('7. the token cannot be claimed by another account');
const replay = await request('/v1/billing/verify', {
  method: 'POST',
  headers: other,
  body: JSON.stringify({ purchaseToken: token }),
});
assert(
  replay.status === 409 && replay.body.code === 'PURCHASE_ALREADY_CLAIMED',
  `expected PURCHASE_ALREADY_CLAIMED, got ${replay.status} ${JSON.stringify(replay.body)}`,
);

console.log('8. restore purchases');
const restore = await request('/v1/billing/restore', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ purchases: [{ purchaseToken: token }] }),
});
assert(restore.status === 200, `restore failed: ${restore.status}`);
assert(restore.body.restored.length === 1, 'expected one restored purchase');
assert(restore.body.entitlement.plan === 'PRO', 'expected PRO after restore');

console.log('9. unknown tokens are rejected');
const bad = await request('/v1/billing/verify', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ purchaseToken: 'not-a-real-token' }),
});
assert(bad.status === 400, `expected 400 for unknown token, got ${bad.status}`);

console.log('10. RTDN test notification is acknowledged');
const testPush = await request('/v1/billing/rtdn', {
  method: 'POST',
  body: rtdnBody({ packageName: 'uz.inglish.app', testNotification: { version: '1.0' } }),
});
assert(testPush.status === 200, `expected 200 for test RTDN, got ${testPush.status}`);

console.log('11. RTDN voided purchase revokes access');
const refund = await request('/v1/billing/rtdn', {
  method: 'POST',
  body: rtdnBody({
    packageName: 'uz.inglish.app',
    eventTimeMillis: String(Date.now()),
    voidedPurchaseNotification: { purchaseToken: token, refundType: 1 },
  }),
});
assert(refund.status === 200, `expected 200 for refund RTDN, got ${refund.status}`);

const revoked = await request('/v1/billing/entitlement', { headers: auth });
assert(revoked.body.entitlement.plan === 'FREE', 'expected FREE after refund');

console.log('12. refunded token cannot be replayed');
const replayRefunded = await request('/v1/billing/verify', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ purchaseToken: token }),
});
assert(
  replayRefunded.status === 409,
  `expected 409 for refunded token, got ${replayRefunded.status}`,
);

console.log('13. teacher purchase maps to TEACHER');
const teacherToken = `mock:inglish_teacher_monthly:30:${Date.now()}`;
const teacher = await request('/v1/billing/verify', {
  method: 'POST',
  headers: other,
  body: JSON.stringify({ purchaseToken: teacherToken }),
});
assert(teacher.status === 201, `teacher verify failed: ${teacher.status}`);
assert(teacher.body.entitlement.plan === 'TEACHER', 'expected TEACHER entitlement');
assert(
  teacher.body.entitlement.limits.speakingPerDay === 30,
  'expected TEACHER speaking limit',
);

console.log('\nBilling smoke passed.');
