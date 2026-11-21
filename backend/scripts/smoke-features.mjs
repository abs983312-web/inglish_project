/**
 * End-to-end smoke test for the authenticated feature surface:
 * profile update, hidden answer keys, answer checking, usage, practice
 * submissions, and account deletion.
 *
 * Run against a seeded dev server:
 *   npm run smoke:features
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

const email = `features-smoke-${Date.now()}@example.com`;
const password = 'StrongPass123!';

console.log('1. register');
const register = await request('/v1/auth/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, displayName: 'Feature Smoke' }),
});
assert(register.status === 201, `register failed: ${register.status} ${JSON.stringify(register.body)}`);
const auth = { authorization: `Bearer ${register.body.accessToken}` };

console.log('2. me');
const me = await request('/v1/me', { headers: auth });
assert(me.status === 200, `me failed: ${me.status}`);
assert(me.body.user.profile.displayName === 'Feature Smoke', 'unexpected display name');

console.log('2b. empty progress');
const emptyProgress = await request('/v1/progress', { headers: auth });
assert(emptyProgress.status === 200, `empty progress failed: ${emptyProgress.status}`);
assert(emptyProgress.body.progress.currentBand === null, 'expected no band before practice');
assert(
  emptyProgress.body.progress.exercises.accuracy === null,
  'expected no accuracy before practice',
);
assert(emptyProgress.body.progress.streakDays === 0, 'expected zero streak before practice');

console.log('3. profile update');
const profile = await request('/v1/me/profile', {
  method: 'PUT',
  headers: auth,
  body: JSON.stringify({
    displayName: 'Updated Smoke',
    cefrLevel: 'B2',
    targetBand: 6.5,
    examDate: '2027-03-15',
    onboardingDone: true,
  }),
});
assert(profile.status === 200, `profile update failed: ${profile.status} ${JSON.stringify(profile.body)}`);
assert(profile.body.user.profile.targetBand === 6.5, 'expected targetBand 6.5');
assert(profile.body.user.profile.examDate === '2027-03-15', 'expected exam date YYYY-MM-DD');
assert(profile.body.user.profile.onboardingDone === true, 'expected onboardingDone=true');

console.log('4. content units');
const units = await request('/v1/content/units');
assert(units.status === 200, `units failed: ${units.status}`);
assert(units.body.units.length > 0, 'expected content units');
const slug = units.body.units.find((unit) => unit.lessonCount > 0)?.slug;
assert(slug, 'expected a unit with lessons');

console.log('5. unit detail hides answer keys');
const detail = await request(`/v1/content/units/${slug}`);
assert(detail.status === 200, `unit detail failed: ${detail.status}`);
const exercises = detail.body.unit.lessons.flatMap((lesson) => lesson.exercises);
assert(exercises.length > 0, 'expected exercises');
for (const exercise of exercises) {
  assert(!('answerJson' in exercise), 'answerJson must not be exposed');
  assert(!('explanation' in exercise), 'explanation must not be exposed');
}

console.log('6. answer checking');
const choice = exercises.find((exercise) => exercise.type === 'MULTIPLE_CHOICE');
assert(choice, 'expected a multiple-choice exercise');
const probe = await request(`/v1/content/exercises/${choice.id}/answer`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ answer: 0 }),
});
assert(probe.status === 200, `answer check failed: ${probe.status} ${JSON.stringify(probe.body)}`);
assert('isCorrect' in probe.body.result, 'expected isCorrect in answer result');
assert('correctAnswer' in probe.body.result, 'expected correctAnswer in answer result');
const correctIndex = probe.body.result.correctAnswer;
const checked = await request(`/v1/content/exercises/${choice.id}/answer`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ answer: correctIndex }),
});
assert(checked.body.result.isCorrect === true, 'expected correct answer to validate');

console.log('7. usage');
const usage = await request('/v1/usage', { headers: auth });
assert(usage.status === 200, `usage failed: ${usage.status}`);
assert(usage.body.usage.plan === 'FREE', 'expected FREE plan');
assert(usage.body.usage.speaking.limit === 3, 'expected 3 speaking/day');
assert(usage.body.usage.writing.limit === 1, 'expected 1 writing/month');

console.log('8. vocabulary review');
const due = await request('/v1/vocabulary/due', { headers: auth });
assert(due.status === 200, `vocabulary due failed: ${due.status}`);
assert(due.body.items.length > 0, 'expected due vocabulary');
const review = await request(`/v1/vocabulary/${due.body.items[0].id}/review`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ rating: 3 }),
});
assert(review.status === 200, `vocabulary review failed: ${review.status}`);

console.log('9. writing submission and scoring');
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
assert(created.status === 201, `writing create failed: ${created.status}`);
const scored = await request(`/v1/writing/submissions/${created.body.submission.id}/submit`, {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({}),
});
assert(scored.status === 200, `writing scoring failed: ${scored.status} ${JSON.stringify(scored.body)}`);
assert(scored.body.submission.status === 'SCORED', 'expected writing SCORED');

console.log('10. speaking submission and scoring');
const transcript =
  'I would like to talk about my hometown, which is a small city in the east of the country. I have lived there for most of my life, and I visit my family there every summer. What I like most is the pace of life because it is much calmer than the capital. There is a large park where people walk in the evening, and the food is excellent. In recent years, several new cafes and shops have opened, but the city still feels friendly. I would like to live there in the future, although I may need to move for work.';
const speakingCreated = await request('/v1/speaking/submissions', {
  method: 'POST',
  headers: auth,
  body: JSON.stringify({ promptSlug: 'part1-hometown', transcript }),
});
assert(speakingCreated.status === 201, `speaking create failed: ${speakingCreated.status}`);
const speakingScored = await request(
  `/v1/speaking/submissions/${speakingCreated.body.submission.id}/submit`,
  { method: 'POST', headers: auth, body: JSON.stringify({}) },
);
assert(speakingScored.status === 200, `speaking scoring failed: ${speakingScored.status}`);
assert(speakingScored.body.submission.status === 'SCORED', 'expected speaking SCORED');

console.log('11. progress aggregation');
const progressResponse = await request('/v1/progress', { headers: auth });
assert(progressResponse.status === 200, `progress failed: ${progressResponse.status}`);
const progress = progressResponse.body.progress;
assert(progress.targetBand === 6.5, 'expected target band from profile');
assert(progress.examDate === '2027-03-15', 'expected exam date from profile');
assert(typeof progress.daysToExam === 'number', 'expected daysToExam');
assert(progress.writing.scoredCount >= 1, 'expected at least one scored writing submission');
assert(typeof progress.writing.latestBand === 'number', 'expected latest writing band');
assert(progress.speaking.scoredCount >= 1, 'expected at least one scored speaking submission');
assert(typeof progress.speaking.latestBand === 'number', 'expected latest speaking band');
assert(typeof progress.currentBand === 'number', 'expected current band estimate');
assert(progress.vocabulary.reviewed >= 1, 'expected at least one reviewed word');
assert(progress.vocabulary.total >= 1, 'expected vocabulary total');
assert(progress.exercises.attempted >= 2, 'expected recorded exercise attempts');
assert(progress.exercises.correct >= 1, 'expected at least one correct attempt');
assert(
  progress.exercises.accuracy !== null &&
    progress.exercises.accuracy > 0 &&
    progress.exercises.accuracy <= 1,
  'expected accuracy between 0 and 1',
);
assert(progress.streakDays >= 1, 'expected an active streak after practice');

console.log('12. account deletion');
const deleted = await request('/v1/me', { method: 'DELETE', headers: auth });
assert(deleted.status === 204, `delete failed: ${deleted.status}`);
const afterDelete = await request('/v1/me', { headers: auth });
assert(afterDelete.status === 401, `expected 401 after delete, got ${afterDelete.status}`);
const afterDeleteUsage = await request('/v1/usage', { headers: auth });
assert(
  afterDeleteUsage.status === 401,
  `expected 401 on every protected route after delete, got ${afterDeleteUsage.status}`,
);

console.log(`\nAll feature smoke checks passed for ${email}`);
