/**
 * Multiplayer battle smoke test: friend room code, lobby readiness, host start,
 * WebSocket fan-out, reconnect, server-authoritative timeout, and final ranks.
 *
 * Run against a seeded dev server:
 *   npm run smoke:battle:multiplayer
 */
import WebSocket from 'ws';

const base = process.env.API_BASE_URL ?? 'http://127.0.0.1:3000';
const wsBase = base.replace(/^http/, 'ws');

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
      // not ready
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error('API did not become healthy');
}

async function waitForRoom(auth, roomId, predicate, label, attempts = 80) {
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    const response = await request(`/v1/battle/rooms/${roomId}`, { headers: auth });
    assert(response.status === 200, `${label}: fetch failed (${response.status})`);
    if (predicate(response.body.room)) return response.body.room;
    // The global limiter is 120 req/min/IP; 1s polling stays well below it.
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error(`${label}: timed out`);
}

function connectSocket(roomId, token) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(
      `${wsBase}/v1/battle/rooms/${roomId}/ws?token=${encodeURIComponent(token)}`,
    );
    const messages = [];
    const timeout = setTimeout(() => {
      socket.close();
      reject(new Error('WebSocket did not send a room snapshot'));
    }, 8000);

    socket.on('message', (raw) => {
      let message;
      try {
        message = JSON.parse(raw.toString());
      } catch {
        return;
      }
      messages.push(message);
      if (message.type === 'room') {
        clearTimeout(timeout);
        resolve({ socket, messages });
      }
    });
    socket.on('error', (error) => {
      clearTimeout(timeout);
      reject(error);
    });
  });
}

async function answer(auth, roomId, roundId, answerIndex) {
  return request(`/v1/battle/rooms/${roomId}/answer`, {
    method: 'POST',
    headers: auth,
    body: JSON.stringify({ roundId, answerIndex }),
  });
}

async function register(label) {
  const email = `${label}-${Date.now()}-${Math.random().toString(16).slice(2)}@example.com`;
  const response = await request('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({
      email,
      password: 'StrongPass123!',
      displayName: label,
    }),
  });
  assert(response.status === 201, `register ${label} failed: ${JSON.stringify(response.body)}`);
  return {
    email,
    auth: { authorization: `Bearer ${response.body.accessToken}` },
    accessToken: response.body.accessToken,
  };
}

await waitForHealth();

console.log('1. register host and guest');
const host = await register('Host');
const guest = await register('Guest');

console.log('2. host creates a friend room');
const created = await request('/v1/battle/rooms', {
  method: 'POST',
  headers: host.auth,
  body: JSON.stringify({
    mode: 'FRIEND',
    questionCount: 5,
    timeLimitMs: 10_000,
    autoBot: false,
  }),
});
assert(created.status === 201, `create failed: ${JSON.stringify(created.body)}`);
let room = created.body.room;
const hostParticipantId = created.body.participantId;
assert(room.status === 'LOBBY', `expected LOBBY, got ${room.status}`);
assert(room.phase === 'LOBBY', `expected LOBBY phase, got ${room.phase}`);
assert(/^\d{6}$/.test(room.code), `expected a 6-digit code, got ${room.code}`);
assert(room.maxPlayers === 10, `expected FREE maxPlayers 10, got ${room.maxPlayers}`);
assert(room.currentQuestion === null, 'lobby must not expose a question');
assert(room.scores.length === 1, 'expected only the host in the lobby');
assert(room.scores[0].isHost === true, 'host flag missing');
assert(room.scores[0].isReady === true, 'host must be ready');

console.log('3. guest joins by code');
const joined = await request('/v1/battle/rooms/join', {
  method: 'POST',
  headers: guest.auth,
  body: JSON.stringify({ code: room.code }),
});
assert(joined.status === 200, `join failed: ${JSON.stringify(joined.body)}`);
const guestParticipantId = joined.body.participantId;
room = joined.body.room;
assert(room.scores.length === 2, 'expected two players after joining');
assert(
  room.scores.some((entry) => entry.id === guestParticipantId),
  'guest participant missing',
);

console.log('4. host cannot start before the guest is ready');
const earlyStart = await request(`/v1/battle/rooms/${room.id}/start`, {
  method: 'POST',
  headers: host.auth,
});
assert(earlyStart.status === 409, `expected 409 before ready, got ${earlyStart.status}`);
assert(earlyStart.body.code === 'PLAYERS_NOT_READY', `unexpected code ${earlyStart.body.code}`);

console.log('5. guest marks ready and host starts');
const ready = await request(`/v1/battle/rooms/${room.id}/ready`, {
  method: 'POST',
  headers: guest.auth,
  body: JSON.stringify({ ready: true }),
});
assert(ready.status === 200, `ready failed: ${JSON.stringify(ready.body)}`);
assert(
  ready.body.room.scores.find((entry) => entry.id === guestParticipantId)?.isReady === true,
  'guest ready flag missing',
);

const started = await request(`/v1/battle/rooms/${room.id}/start`, {
  method: 'POST',
  headers: host.auth,
});
assert(started.status === 200, `start failed: ${JSON.stringify(started.body)}`);
room = started.body.room;
assert(room.status === 'ACTIVE', 'expected ACTIVE after start');
assert(room.phase === 'QUESTION', 'expected the first question');
assert(room.currentQuestion, 'missing first question');
assert(!JSON.stringify(room).includes('correctIndex'), 'answer key leaked at start');

console.log('6. both players connect over WebSocket');
const hostSocket = await connectSocket(room.id, host.accessToken);
const guestSocket = await connectSocket(room.id, guest.accessToken);
assert(hostSocket.messages.some((message) => message.type === 'room'), 'host got no room event');
assert(guestSocket.messages.some((message) => message.type === 'room'), 'guest got no room event');
assert(
  guestSocket.messages.some(
    (message) =>
      message.type === 'room' &&
      message.room.scores.some((entry) => entry.id === hostParticipantId && entry.isOnline),
  ),
  'online presence was not broadcast',
);

console.log('7. both answer the first question');
const firstRoundId = room.currentQuestion.id;
const hostAnswer = await answer(host.auth, room.id, firstRoundId, 0);
assert(hostAnswer.status === 200, `host answer failed: ${JSON.stringify(hostAnswer.body)}`);
assert(
  hostAnswer.body.result === null,
  'host must not see the key before the reveal',
);
assert(
  !JSON.stringify(hostAnswer.body.room).includes('correctIndex'),
  'answer key leaked before reveal',
);

const guestAnswer = await answer(guest.auth, room.id, firstRoundId, 1);
assert(guestAnswer.status === 200, `guest answer failed: ${JSON.stringify(guestAnswer.body)}`);
room = guestAnswer.body.room;
assert(room.phase === 'REVEAL', `expected REVEAL, got ${room.phase}`);
assert(room.reveal, 'missing reveal payload');
assert(Number.isInteger(room.reveal.correctIndex), 'missing correct index in reveal');
assert(room.reveal.answers.length === 2, 'expected answers from both players');
assert(guestAnswer.body.result, 'the last answer should include the result');

console.log('8. WebSocket receives the reveal');
await new Promise((resolve) => setTimeout(resolve, 300));
assert(
  hostSocket.messages.some(
    (message) => message.type === 'room' && message.room.phase === 'REVEAL',
  ),
  'host did not receive the reveal over WebSocket',
);

console.log('9. reconnect works');
hostSocket.socket.close();
await new Promise((resolve) => setTimeout(resolve, 300));
const reconnected = await connectSocket(room.id, host.accessToken);
assert(
  reconnected.messages.some(
    (message) => message.type === 'room' && message.room.phase === 'REVEAL',
  ),
  'reconnect did not restore the room snapshot',
);

console.log('10. a disconnected player times out on the next question');
room = await waitForRoom(
  host.auth,
  room.id,
  (current) => current.phase === 'QUESTION' && current.currentPosition === 2,
  'second question',
);
const secondRoundId = room.currentQuestion.id;
const hostSecond = await answer(host.auth, room.id, secondRoundId, 0);
assert(hostSecond.status === 200, `host second answer failed: ${JSON.stringify(hostSecond.body)}`);
room = await waitForRoom(
  host.auth,
  room.id,
  (current) => current.phase === 'REVEAL' && current.currentPosition === 2,
  'second reveal',
  100,
);
const guestReveal = room.reveal.answers.find(
  (entry) => entry.participantId === guestParticipantId,
);
assert(guestReveal, 'missing guest timeout answer');
assert(guestReveal.answerIndex === -1, 'timeout must be recorded as -1');
assert(guestReveal.isCorrect === false, 'timeout must be incorrect');
assert(guestReveal.scoreGained === 0, 'timeout must score 0');

console.log('11. finish the remaining questions');
let safety = 0;
while (!room.finished && safety < 20) {
  safety += 1;
  room = await waitForRoom(
    host.auth,
    room.id,
    (current) => current.finished || current.phase === 'QUESTION',
    `round ${safety}`,
  );
  if (room.finished) break;
  const question = room.currentQuestion;
  assert(question, 'missing current question');
  const first = await answer(host.auth, room.id, question.id, 0);
  assert(first.status === 200, `host round ${safety} failed: ${JSON.stringify(first.body)}`);
  const second = await answer(guest.auth, room.id, question.id, 1);
  assert(second.status === 200, `guest round ${safety} failed: ${JSON.stringify(second.body)}`);
  room = second.body.room;
}

assert(room.finished, 'room did not finish');
assert(room.phase === 'FINISHED', `expected FINISHED, got ${room.phase}`);
assert(room.results?.length === 2, 'expected two final results');
assert(room.results.every((entry) => Number.isInteger(entry.rank)), 'missing final ranks');

console.log('12. late join is rejected');
const lateJoin = await request('/v1/battle/rooms/join', {
  method: 'POST',
  headers: guest.auth,
  body: JSON.stringify({ code: room.code }),
});
assert(lateJoin.status === 409, `expected 409 for late join, got ${lateJoin.status}`);
assert(lateJoin.body.code === 'ROOM_ALREADY_STARTED', `unexpected ${lateJoin.body.code}`);

console.log('13. classroom room works and does not auto-fill a bot');
const classroom = await request('/v1/battle/rooms', {
  method: 'POST',
  headers: host.auth,
  body: JSON.stringify({
    mode: 'CLASSROOM',
    questionCount: 5,
    timeLimitMs: 10_000,
    autoBot: true,
  }),
});
assert(classroom.status === 201, `classroom create failed: ${JSON.stringify(classroom.body)}`);
assert(classroom.body.room.mode === 'CLASSROOM', 'expected CLASSROOM mode');
assert(classroom.body.room.status === 'LOBBY', 'classroom must start in LOBBY');
assert(/^\d{6}$/.test(classroom.body.room.code), 'classroom needs a 6-digit code');
assert(classroom.body.room.maxPlayers === 10, 'FREE classroom maxPlayers must be 10');

const classroomJoin = await request('/v1/battle/rooms/join', {
  method: 'POST',
  headers: guest.auth,
  body: JSON.stringify({ code: classroom.body.room.code }),
});
assert(classroomJoin.status === 200, `classroom join failed: ${JSON.stringify(classroomJoin.body)}`);
assert(classroomJoin.body.room.scores.length === 2, 'classroom should have two players');
assert(
  classroomJoin.body.room.scores.every((entry) => !entry.isBot),
  'classroom must not auto-fill a bot',
);

const guestLeave = await request(`/v1/battle/rooms/${classroom.body.room.id}/leave`, {
  method: 'POST',
  headers: guest.auth,
});
assert(guestLeave.status === 200, `classroom guest leave failed: ${JSON.stringify(guestLeave.body)}`);
assert(guestLeave.body.closed === false, 'guest leave must not close the classroom');
const hostLeave = await request(`/v1/battle/rooms/${classroom.body.room.id}/leave`, {
  method: 'POST',
  headers: host.auth,
});
assert(hostLeave.status === 200, `classroom host leave failed: ${JSON.stringify(hostLeave.body)}`);
assert(hostLeave.body.closed === true, 'host leave must close the classroom');

console.log('14. clean up');
hostSocket.socket.close();
guestSocket.socket.close();
reconnected.socket.close();
await request('/v1/me', { method: 'DELETE', headers: host.auth });
await request('/v1/me', { method: 'DELETE', headers: guest.auth });

console.log(`\nAll multiplayer battle checks passed (${host.email}, ${guest.email})`);
