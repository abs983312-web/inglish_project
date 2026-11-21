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

async function waitForHealth() {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    try {
      const health = await request('/health');
      if (health.status === 200) {
        return;
      }
    } catch {
      // not ready yet
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error('API did not become healthy');
}

await waitForHealth();

const email = `content-smoke-${Date.now()}@example.com`;
const password = 'StrongPass123!';

console.log('1. register');
const register = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, displayName: 'Content Smoke' }),
});
assert(register.status === 201, `register failed: ${register.status} ${JSON.stringify(register.body)}`);
const auth = {
  authorization: `Bearer ${register.body.accessToken}`,
};

console.log('2. content units');
const units = await request('/v1/content/units');
assert(units.status === 200, `units failed: ${units.status}`);
assert(units.body.units.length >= 3, `expected at least 3 units, got ${units.body.units.length}`);

console.log('3. writing unit detail');
const detail = await request('/v1/content/units/b2-writing-opinion-essays');
assert(detail.status === 200, `unit detail failed: ${detail.status}`);
assert(detail.body.unit.lessons.length >= 3, 'expected at least 3 lessons');
assert(
  detail.body.unit.lessons.some((lesson) => lesson.exercises.length > 0),
  'expected at least one exercise',
);

console.log('4. vocabulary due');
const due = await request('/v1/vocabulary/due', { headers: auth });
assert(due.status === 200, `vocabulary due failed: ${due.status}`);
assert(due.body.items.length > 0, 'expected new vocabulary items');
const firstWord = due.body.items[0];

console.log('5. vocabulary review');
const review = await request(`/v1/vocabulary/${firstWord.id}/review`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ rating: 3 }),
});
assert(review.status === 200, `review failed: ${review.status} ${JSON.stringify(review.body)}`);
assert(review.body.review.reps === 1, 'expected reps=1');
assert(review.body.review.state === 'REVIEW', 'expected REVIEW state for rating 3');

console.log('6. vocabulary stats');
const stats = await request('/v1/vocabulary/stats', { headers: auth });
assert(stats.status === 200, `stats failed: ${stats.status}`);
assert(stats.body.stats.total >= 500, `expected at least 500 vocabulary items, got ${stats.body.stats.total}`);
assert(stats.body.stats.review >= 1, 'expected at least one reviewed item');

console.log('7. vocabulary sets');
const sets = await request('/v1/vocabulary/sets', { headers: auth });
assert(sets.status === 200, `sets failed: ${sets.status}`);
assert(sets.body.sets.length === 10, `expected 10 sets, got ${sets.body.sets.length}`);
assert(
  sets.body.sets.every((set) => set.count === 50),
  `expected 50 words per set, got ${JSON.stringify(sets.body.sets.map((set) => set.count))}`,
);

console.log('8. vocabulary pagination');
const pageOne = await request('/v1/vocabulary?tag=set-education&limit=10&offset=0', {
  headers: auth,
});
assert(pageOne.status === 200, `page one failed: ${pageOne.status}`);
assert(pageOne.body.total === 50, `expected set total 50, got ${pageOne.body.total}`);
assert(pageOne.body.items.length === 10, `expected 10 items, got ${pageOne.body.items.length}`);
const pageTwo = await request('/v1/vocabulary?tag=set-education&limit=10&offset=10', {
  headers: auth,
});
assert(pageTwo.status === 200, `page two failed: ${pageTwo.status}`);
assert(pageTwo.body.items.length === 10, `expected 10 items on page two, got ${pageTwo.body.items.length}`);
const pageOneIds = new Set(pageOne.body.items.map((item) => item.id));
assert(
  pageTwo.body.items.every((item) => !pageOneIds.has(item.id)),
  'expected no overlap between pages',
);

console.log('9. vocabulary search');
const search = await request('/v1/vocabulary?q=curriculum', { headers: auth });
assert(search.status === 200, `search failed: ${search.status}`);
assert(search.body.total >= 1, 'expected curriculum in the vocabulary library');

console.log('10. writing tasks');
const tasks = await request('/v1/writing/tasks');
assert(tasks.status === 200, `tasks failed: ${tasks.status}`);
assert(tasks.body.tasks.length >= 2, 'expected at least 2 writing tasks');

console.log('11. writing submission');
const essay = [
  'Remote work has become common in many countries. Some people believe it is better for employees, while others prefer the office.',
  'On the one hand, working from home saves commuting time and gives employees more flexibility. This can reduce stress and help parents manage family responsibilities.',
  'On the other hand, the office supports teamwork and informal learning. New employees especially benefit from observing experienced colleagues and asking questions in person.',
  'In my opinion, a hybrid model is the most practical solution. Employees can focus at home for part of the week and come to the office for collaboration and training.',
  'In conclusion, remote work offers clear benefits, but it should not replace the office completely. A balanced approach supports both productivity and teamwork.',
].join('\n\n');

const created = await request('/v1/writing/submissions', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ taskSlug: 'task2-opinion-remote-work', text: essay }),
});
assert(created.status === 201, `writing submission failed: ${created.status} ${JSON.stringify(created.body)}`);
const writingId = created.body.submission.id;

console.log('12. writing scoring (mock)');
const scored = await request(`/v1/writing/submissions/${writingId}/submit`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({}),
});
assert(scored.status === 200, `writing scoring failed: ${scored.status} ${JSON.stringify(scored.body)}`);
assert(scored.body.submission.status === 'SCORED', 'expected SCORED status');
assert(typeof scored.body.submission.bandEstimate === 'number', 'expected a band estimate');
assert(scored.body.submission.feedback?.disclaimer, 'expected a disclaimer');

console.log('13. speaking prompts');
const prompts = await request('/v1/speaking/prompts');
assert(prompts.status === 200, `speaking prompts failed: ${prompts.status}`);
assert(prompts.body.prompts.length >= 3, 'expected at least 3 speaking prompts');

console.log('14. speaking submission');
const transcript =
  'I would like to talk about my hometown, which is a small city in the east of the country. I have lived there for most of my life, and I visit my family there every summer. What I like most is the pace of life because it is much calmer than the capital. There is a large park where people walk in the evening, and the food is excellent. In recent years, several new cafes and shops have opened, but the city still feels friendly. I would like to live there in the future, although I may need to move for work.';
const speakingCreated = await request('/v1/speaking/submissions', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({
    promptSlug: 'part1-hometown',
    transcript,
    durationSeconds: 75,
  }),
});
assert(
  speakingCreated.status === 201,
  `speaking submission failed: ${speakingCreated.status} ${JSON.stringify(speakingCreated.body)}`,
);
const speakingId = speakingCreated.body.submission.id;

console.log('15. speaking scoring (mock)');
const speakingScored = await request(`/v1/speaking/submissions/${speakingId}/submit`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({}),
});
assert(
  speakingScored.status === 200,
  `speaking scoring failed: ${speakingScored.status} ${JSON.stringify(speakingScored.body)}`,
);
assert(speakingScored.body.submission.status === 'SCORED', 'expected SCORED status');
assert(
  speakingScored.body.submission.feedback?.pronunciationNote,
  'expected a pronunciation note',
);

console.log(`\nAll content/practice smoke checks passed for ${email}`);
