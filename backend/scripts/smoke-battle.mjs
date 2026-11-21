/**
 * End-to-end smoke test for the bot battle vertical slice:
 * room creation, hidden answer keys, server-side scoring, double-answer
 * protection, reveal/advance timing, finish state, and result ranking.
 *
 * Run against a seeded dev server:
 *   npm run smoke:battle
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

async function waitForRoom(auth, roomId, predicate, label) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    const response = await request(`/v1/battle/rooms/${roomId}`, { headers: auth });
    assert(response.status === 200, `${label}: room fetch failed (${response.status})`);
    if (predicate(response.body.room)) return response.body.room;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`${label}: timed out waiting for the room`);
}

await waitForHealth();

const email = `battle-smoke-${Date.now()}@example.com`;
const password = 'StrongPass123!';

console.log('1. register');
const register = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, displayName: 'Battle Smoke' }),
});
assert(
  register.status === 201,
  `register failed: ${register.status} ${JSON.stringify(register.body)}`,
);
const auth = { authorization: `Bearer ${register.body.accessToken}` };

console.log('2. create bot battle room');
const created = await request('/v1/battle/rooms', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ mode: 'BOT', questionCount: 5 }),
});
assert(
  created.status === 201,
  `room create failed: ${created.status} ${JSON.stringify(created.body)}`,
);

let room = created.body.room;
assert(typeof created.body.participantId === 'string', 'expected a participant id');
assert(room.status === 'ACTIVE', 'expected ACTIVE room');
assert(room.phase === 'QUESTION', 'expected the first question phase');
assert(room.mode === 'BOT', 'expected BOT mode');
assert(room.questionCount === 5, `expected 5 questions, got ${room.questionCount}`);
assert(room.scores.length === 2, 'expected two participants');
assert(room.scores.some((score) => score.isBot), 'expected a bot participant');
assert(room.scores.some((score) => !score.isBot), 'expected a human participant');
assert(room.currentQuestion, 'expected the first question');
assert(room.currentQuestion.options.length >= 2, 'expected answer options');

const roomPayload = JSON.stringify(room);
assert(!roomPayload.includes('correctIndex'), 'answer key must not be exposed before answering');
assert(!roomPayload.includes('explanation'), 'explanation must not be exposed before answering');

console.log('3. answer the first question with a timeout (-1)');
const firstRoundId = room.currentQuestion.id;
const firstAnswer = await request(`/v1/battle/rooms/${room.id}/answer`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ roundId: firstRoundId, answerIndex: -1 }),
});
assert(
  firstAnswer.status === 200,
  `first answer failed: ${firstAnswer.status} ${JSON.stringify(firstAnswer.body)}`,
);
assert(firstAnswer.body.result, 'expected an answer result once the round is revealed');
assert(firstAnswer.body.result.isCorrect === false, 'a timeout must be incorrect');
assert(firstAnswer.body.result.scoreGained === 0, 'a timeout must score 0');
assert(
  Number.isInteger(firstAnswer.body.result.correctIndex),
  'expected the correct index after answering',
);
room = firstAnswer.body.room;
assert(room.phase === 'REVEAL', `expected REVEAL after answering, got ${room.phase}`);
assert(room.reveal, 'expected reveal data');
assert(Number.isInteger(room.reveal.correctIndex), 'expected the correct index in the reveal');

console.log('4. double answer during the reveal is rejected');
const duplicate = await request(`/v1/battle/rooms/${room.id}/answer`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ roundId: firstRoundId, answerIndex: 0 }),
});
assert(duplicate.status === 409, `expected 409 for a duplicate answer, got ${duplicate.status}`);

console.log('5. answer the remaining questions');
let safety = 0;
while (!room.finished && safety < 20) {
  safety += 1;
  room = await waitForRoom(
    auth,
    room.id,
    (current) => current.finished || current.phase === 'QUESTION',
    `question ${safety}`,
  );
  if (room.finished) break;

  const question = room.currentQuestion;
  assert(question, 'expected a current question while the room is active');

  const response = await request(`/v1/battle/rooms/${room.id}/answer`, {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({
      roundId: question.id,
      answerIndex: safety % question.options.length,
    }),
  });
  assert(
    response.status === 200,
    `answer ${safety} failed: ${response.status} ${JSON.stringify(response.body)}`,
  );
  room = response.body.room;
}

assert(room.finished === true, 'expected the room to finish');
assert(room.phase === 'FINISHED', 'expected the finished phase');
assert(room.results && room.results.length === 2, 'expected two final results');
assert(room.results.every((entry) => typeof entry.rank === 'number'), 'expected ranks');
assert(
  room.results.every((entry) => entry.score >= 0),
  'expected non-negative final scores',
);
assert(room.currentQuestion === null, 'expected no current question after finishing');

console.log('6. fetch the finished room');
const fetched = await request(`/v1/battle/rooms/${room.id}`, { headers: auth });
assert(fetched.status === 200, `room fetch failed: ${fetched.status}`);
assert(fetched.body.room.finished === true, 'expected the fetched room to be finished');
assert(
  JSON.stringify(fetched.body.room).includes('correctIndex') === false,
  'answer keys must stay hidden after finishing',
);

console.log('7. answering after the finish is rejected');
const late = await request(`/v1/battle/rooms/${room.id}/answer`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({
    roundId: firstRoundId,
    answerIndex: 0,
  }),
});
assert(late.status === 409, `expected 409 after finish, got ${late.status}`);

console.log('8. account deletion');
const deleted = await request('/v1/me', { method: 'DELETE', headers: auth });
assert(deleted.status === 204, `delete failed: ${deleted.status}`);

console.log(`\nAll battle smoke checks passed for ${email}`);
