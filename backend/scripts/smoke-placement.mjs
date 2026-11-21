/**
 * End-to-end smoke test for the placement vertical slice:
 * session creation with hidden answer keys, incomplete submission rejection,
 * full submission scoring, result persistence, and profile update.
 *
 * Run against a seeded dev server:
 *   npm run smoke:placement
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
  if (!condition) {
    throw new Error(message);
  }
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

const email = `placement-smoke-${Date.now()}@example.com`;
const password = 'StrongPass123!';

console.log('1. register');
const register = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, displayName: 'Placement Smoke' }),
});
assert(
  register.status === 201,
  `register failed: ${register.status} ${JSON.stringify(register.body)}`,
);
const auth = { authorization: `Bearer ${register.body.accessToken}` };

console.log('2. create placement session');
const created = await request('/v1/placement/sessions', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({}),
});
assert(
  created.status === 201,
  `placement create failed: ${created.status} ${JSON.stringify(created.body)}`,
);

let session = created.body.session;
assert(session.status === 'ACTIVE', 'expected ACTIVE session');
assert(Array.isArray(session.questions) && session.questions.length >= 20, `expected 20+ questions, got ${session.questions?.length}`);
assert(session.questions.every((question) => Array.isArray(question.options) && question.options.length >= 2), 'expected options on every question');

const sessionPayload = JSON.stringify(session);
assert(!sessionPayload.includes('correctIndex'), 'answer keys must not be exposed');
assert(!sessionPayload.includes('explanation'), 'explanations must not be exposed');

const sections = new Set(session.questions.map((question) => question.section));
assert(sections.has('GRAMMAR'), 'expected grammar questions');
assert(sections.has('VOCABULARY'), 'expected vocabulary questions');
assert(sections.has('READING'), 'expected reading questions');
assert(session.writing?.prompt, 'expected a writing prompt');
assert(session.speaking?.topic, 'expected a speaking prompt');

const writingText = 'Learning English is important for my future studies and work because it helps me communicate with people from different countries. For example, when I apply for a university programme, I will need to read instructions, write emails, and take part in interviews in English. Good English also gives me access to better resources and more opportunities. Therefore, I practise reading, writing, listening, and speaking every day.';
// 70+ words
const speakingTranscript = 'In the future, I plan to study at a university where English is the main language of teaching. I think English is important because it helps me travel, find a good job, and make friends from other countries. I would like to improve my speaking because I can understand many words, but I still need more practice to speak fluently and confidently.';

console.log('3. incomplete submission is rejected');
const incomplete = await request(`/v1/placement/sessions/${session.id}/submit`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ answers: [{ questionId: session.questions[0].id, answerIndex: 0 }] }),
});
assert(incomplete.status === 400, `expected 400 for incomplete submission, got ${incomplete.status}`);

console.log('4. submit all answers');
const answers = session.questions.map((question) => ({
  questionId: question.id,
  answerIndex: 0,
}));
const submitted = await request(`/v1/placement/sessions/${session.id}/submit`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ answers, writingText, speakingTranscript }),
});
assert(
  submitted.status === 200,
  `submit failed: ${submitted.status} ${JSON.stringify(submitted.body)}`,
);
session = submitted.body.session;
assert(session.status === 'FINISHED', 'expected FINISHED session');
assert(session.result, 'expected a result');
assert(session.result.totalCount === session.questions.length, 'expected all questions counted');
assert(typeof session.result.percentage === 'number', 'expected percentage');
assert(['B1', 'B2', 'C1'].includes(session.result.cefrLevel), 'expected a CEFR level');
assert(session.writing?.band === null || typeof session.writing?.band === 'number', 'expected writing band after finish');
assert(session.speaking?.band === null || typeof session.speaking?.band === 'number', 'expected speaking band after finish');
assert(typeof session.result.estimatedBand === 'number', 'expected an estimated band');
assert(['GRAMMAR', 'VOCABULARY', 'READING'].includes(session.result.weakSkill), 'expected a weak skill');

console.log('5. double submit is rejected');
const duplicate = await request(`/v1/placement/sessions/${session.id}/submit`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ answers, writingText, speakingTranscript }),
});
assert(duplicate.status === 409, `expected 409 for a double submit, got ${duplicate.status}`);

console.log('6. fetch session and latest result');
const fetched = await request(`/v1/placement/sessions/${session.id}`, { headers: auth });
assert(fetched.status === 200, `session fetch failed: ${fetched.status}`);
assert(fetched.body.session.status === 'FINISHED', 'expected the fetched session to be finished');
assert(fetched.body.session.result.cefrLevel === session.result.cefrLevel, 'expected the same result');

const latest = await request('/v1/placement/latest', { headers: auth });
assert(latest.status === 200, `latest fetch failed: ${latest.status}`);
assert(latest.body.session?.id === session.id, 'expected the latest session to match');

console.log('7. profile reflects the placement result');
const me = await request('/v1/me', { headers: auth });
assert(me.status === 200, `me failed: ${me.status}`);
assert(me.body.user?.profile?.cefrLevel === session.result.cefrLevel, `expected profile cefrLevel ${session.result.cefrLevel}, got ${JSON.stringify(me.body)}`);

console.log('8. account deletion');
const deleted = await request('/v1/me', { method: 'DELETE', headers: auth });
assert(deleted.status === 204, `delete failed: ${deleted.status}`);

console.log(`\nAll placement smoke checks passed for ${email}`);
