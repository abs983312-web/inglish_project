# 🧪 TEST.md — Test Strategiyasi va Plani
## IELTS AI Murabbiy — Android Ilovasi

> **Versiya:** 1.0 · **Sana:** 2026-11-09  
> **Maqsad:** Barcha test turlari, coverage maqsadlari va CI pipeline

---

## 1. Test Piramidasi

```
         ┌──────────┐
         │   E2E    │  ← 5% (manual + script) — kritik oqimlar
        ┌┴──────────┴┐
       │ Integration  │  ← 25% (Supertest, Room, Socket)
      ┌┴──────────────┴┐
     │   Unit Tests     │  ← 70% (Vitest, JUnit, MockK, Compose)
     └──────────────────┘
```

**Maqsad coverage:**
- Backend (Node.js): **80%** (branches + lines)
- Android (Kotlin): **70%** (unit + UI)

---

## 2. Backend Testlar (Node.js + Fastify)

### 2.1 Qoida

```bash
# Test komandalar
npm run test              # barcha testlar
npm run test:unit         # faqat unit
npm run test:integration  # faqat integration
npm run test:coverage     # coverage hisoboti
npm run test:watch        # watch mode

# Muhit: test DB (PostgreSQL test instance)
# AI: mock (haqiqiy API ga ulanmaydi)
# Redis: in-memory mock
```

### 2.2 Unit testlar (Vitest) — 80% coverage

#### `auth.service.test.ts`
```typescript
describe('AuthService', () => {
  test('register — to'g'ri kiritilsa foydalanuvchi yaratiladi')
  test('register — mavjud email → 409 Conflict')
  test('register — noto'g'ri email format → 400 Validation')
  test('register — qisqa parol (< 8 belgi) → 400')
  test('login — to'g'ri email+parol → access+refresh tokenlar')
  test('login — noto'g'ri parol → 401')
  test('login — email topilmadi → 401 (enumeration himoya)')
  test('login — 5 muvaffaqiyatsiz urinish → rate limit')
  test('refresh — to'g'ri token → yangi access token')
  test('refresh — eski (rotatsiya qilingan) token → 401')
  test('refresh — muddati o'tgan token → 401')
  test('logout — refresh token o'chiriladi')
  test('logout all — barcha tokenlar o'chiriladi')
})
```

#### `writing.service.test.ts`
```typescript
describe('WritingService', () => {
  test('submit — text 250+ so'z → AI ga yuboriladi')
  test('submit — text 250 dan kam → 400 Validation')
  test('submit — idempotency key qayta yuborilsa → 200 (cached)')
  test('band calculation — 4 mezon ortacha to'g'ri hisoblanadi')
  test('feedback parsing — AI JSON → structured FeedbackDto')
  test('feedback parsing — AI xato qaytarsa → fallback baho')
  test('cost tracking — token count saqlanadi')
  test('PRO check — bepul user 2+ submission → 402 PaymentRequired')
})
```

#### `battle.scoring.test.ts`
```typescript
describe('BattleScoring', () => {
  test('to'g'ri + 0ms → 1000 ball (tezlik max)')
  test('to'g'ri + timeLimitMs → 500 ball (tezlik min)')
  test('to'g'ri + timeLimitMs/2 → 750 ball')
  test('noto'g'ri → 0 ball')
  test('vaqt tugadi → 0 ball')
  test('streak 3+ → +100 bonus qo'shiladi')
  test('streak 2 → +50 bonus qo'shiladi')
  test('streak 1 → 0 bonus')
  test('streak reset noto'g'ri javobdan keyin')
  test('total oyinda maks ball to'g'ri hisoblanadi')
})
```

#### `fsrs.algorithm.test.ts`
```typescript
describe('FSRS Algorithm', () => {
  test('yangi karta — due_at: bugun')
  test('Easy rating → stability oshadi')
  test('Hard rating → stability kamayadi')
  test('Again rating → lapses++ va karta qaytadi')
  test('90-kun target retention saqlanadi')
  test('belgilangan intervalda review qaytadi')
})
```

#### `streak.service.test.ts`
```typescript
describe('StreakService', () => {
  test('kecha faol → streak +1')
  test('bugun allaqachon faol → streak o'zgarmaydi')
  test('2 kun o'tdi, freeze yo'q → streak = 0')
  test('2 kun o'tdi, freeze bor → streak saqlanadi, freeze -1')
  test('longest streak yangilanadi')
  test('timezone Asia/Tashkent bo'yicha kecha/bugun aniqlanadi')
})
```

### 2.3 Integration testlar (Supertest)

#### `auth.routes.test.ts`
```typescript
// Real DB, mock AI
test('POST /v1/auth/register — 201 + tokens')
test('POST /v1/auth/register — 409 dup email')
test('POST /v1/auth/login — 200 + tokens')
test('POST /v1/auth/refresh — 200 + new tokens')
test('POST /v1/auth/logout — 204')
test('GET /v1/me — 200 + profile (JWT required)')
test('GET /v1/me — 401 (no token)')
test('GET /v1/me — 401 (expired token)')
```

#### `writing.routes.test.ts`
```typescript
// Real DB, mock Gemini API
test('POST /v1/writing/submissions — 202 Accepted')
test('GET /v1/writing/submissions/:id — 200 (processing)')
test('GET /v1/writing/submissions/:id — 200 (completed + feedback)')
test('POST — idempotency header qayta → cached 200')
test('POST — PRO not subscribed → 402')
```

#### `battle.ws.test.ts`
```typescript
// Socket.IO test client
test('host: xona yaratish → PIN olish')
test('player: PIN bilan qo'shilish → lobby')
test('lobby: 2+ kishi → host start bosishi mumkin')
test('game: savol keladi → barcha clientlarga')
test('game: javob yuborish → ack keladi')
test('game: vaqt tugadi → keyingi savol')
test('game: barcha savollar → natijalar')
test('reconnect: 15 soniya ichida → o'yin davom etadi')
test('reconnect: 15 soniyadan keyin → o'yin davom etadi (slot saqlanmaydi)')
test('host: disconnect + reconnect → host saqlanadi')
```

#### `billing.routes.test.ts`
```typescript
// Mock Google Play API
test('POST /v1/billing/verify — to'g'ri receipt → 200 subscribed')
test('POST /v1/billing/verify — noto'g'ri receipt → 400')
test('POST /v1/billing/verify — qayta yuborish → idempotent 200')
test('POST /v1/webhooks/play — renewal → subscription yangilandi')
test('POST /v1/webhooks/play — cancellation → status = canceled')
```

### 2.4 Load testlar (k6)

```javascript
// battle_load.js — 10 concurrent battle rooms
export default function() {
  // Maqsad: 10 xona × 10 kishi = 100 concurrent connection
  // P95 latency < 200ms (savol deliver)
  // 0 message loss
  // CPU < 70% (1 core)
}

// ai_load.js — Writing submission load
export default function() {
  // Maqsad: 50 req/min sustained
  // P95 latency < 5000ms (AI response)
  // Error rate < 1%
}
```

---

## 3. Android Testlar (Kotlin)

### 3.1 Unit testlar (JUnit 5 + MockK)

#### `WritingViewModelTest.kt`
```kotlin
@Test fun `submit trigger → state changes to Loading`()
@Test fun `AI feedback received → state changes to Success`()
@Test fun `AI error → state changes to Error with retry option`()
@Test fun `word count under 250 → submit button disabled`()
@Test fun `word count 250+ → submit button enabled`()
@Test fun `feedback parsing → all 4 criteria populated`()
@Test fun `rewrite → editor pre-filled with original text`()
```

#### `BattleViewModelTest.kt`
```kotlin
@Test fun `join room → state = Lobby`()
@Test fun `game start → state = Playing`()
@Test fun `question received → timer starts`()
@Test fun `answer submitted → waiting for reveal`()
@Test fun `reveal → score updated correctly`()
@Test fun `game over → final leaderboard shown`()
@Test fun `disconnect → reconnecting state`()
@Test fun `scoring formula → (1 - t/T × 0.5) × 1000`()
```

#### `FsrsCardTest.kt`
```kotlin
@Test fun `new card due_at today`()
@Test fun `Easy → next review in 7+ days`()
@Test fun `Again → next review today (lapses++)`()
@Test fun `retention 90% target maintained`()
```

### 3.2 UI testlar (Compose Testing)

#### `OnboardingFlowTest.kt`
```kotlin
@Test fun `onboarding 4 step → completion registered`()
@Test fun `goal screen → band selector works`()
@Test fun `exam date → calendar picker works`()
@Test fun `skip placement → goes to home`()
```

#### `WritingScreenTest.kt`
```kotlin
@Test fun `typing in editor → word count updates`()
@Test fun `submit with 300 words → loading state shown`()
@Test fun `feedback screen → all criteria visible`()
@Test fun `rewrite button → editor opens`()
```

#### `BattleScreenTest.kt`
```kotlin
@Test fun `lobby screen → pin code displayed`()
@Test fun `question screen → 4 color buttons visible`()
@Test fun `timer bar → decreases over time`()
@Test fun `correct answer → green feedback shown`()
@Test fun `wrong answer → shake animation + red`()
@Test fun `podium screen → top 3 shown`()
```

#### `VocabScreenTest.kt`
```kotlin
@Test fun `card shown → word visible`()
@Test fun `tap card → flip animation + translation shown`()
@Test fun `'Easy' button → next card shown`()
@Test fun `all cards done → completion screen shown`()
```

### 3.3 Integration testlar (Hilt + Room)

```kotlin
@Test fun `save writing draft → Room → retrieve`()
@Test fun `sync queue → offline attempt → online → server sync`()
@Test fun `vocab card → FSRS state persisted in Room`()
@Test fun `streak → last_active_date updated correctly`()
```

---

## 4. AI Sifat Testlari

### 4.1 Writing Evaluation Eval

**Metod:** 200 ta real IELTS insho, inson bahosi bilan taqqoslash

```
Maqsad:
  Pearson r ≥ 0.75
  MAE ≤ 0.5 band
  Confidence interval to'g'ri (actual 80% vaqt CI ichida)

Tekshirish:
  - Score compression yo'q (yuqori ballar pasaytirilmagan)
  - Har 4 mezon alohida
  - Band 5.0–7.5 oralig'ida yaxshi ishlaydi
  
Gate G2: r < 0.75 yoki MAE > 0.5 → model/prompt qayta yoziladi
```

### 4.2 Speaking Evaluation Eval

```
Maqsad:
  MAE ≤ 0.75
  Fluency score korrelyatsiyasi ≥ 0.70
  Pronunciation flag accuracy ≥ 85%
```

### 4.3 Content Validator Eval

```
50 item eval to'plami:
  - 20 Grammatika MCQ → answer key 98%+
  - 15 Lug'at MCQ → bitta javob 95%+
  - 10 Writing prompt → MAE ≤ 0.5
  - 5 Speaking roleplay → format mosligi
```

---

## 5. Manual Test Skriptlari (QA Checklist)

### 5.1 Auth oqimi
```
[ ] Email bilan ro'yxatdan o'tish
[ ] Email verification link ishlaydi
[ ] Google Sign-In ishlaydi
[ ] Noto'g'ri parol → xato xabari
[ ] Parolni tiklash oqimi to'liq
[ ] Barcha qurilmalardan chiqish
[ ] Token muddati o'tganda refresh ishlaydi
```

### 5.2 Writing oqimi
```
[ ] Task 1 va Task 2 ko'rsatiladi
[ ] Matn yozish mumkin (klaviatura)
[ ] 250 so'z cheklov (task 2) ishlaydi
[ ] Submit → loading animatsiya
[ ] Feedback → 4 mezon + overall band
[ ] Inline xatolar highlight qilingan
[ ] Qayta yozish tugmasi ishlaydi
[ ] Netsiz (offline) yozish, online bo'lganda sync
```

### 5.3 Speaking oqimi
```
[ ] Mic ruxsati so'raladi
[ ] Scenario ko'rsatiladi
[ ] 1 daqiqa o'ylash vaqti (Part 2)
[ ] Mikrofon bosib gapirish
[ ] Real-time transkript ko'rinadi
[ ] Tugatish → feedback
[ ] TTS (AI) javob tinglash mumkin
[ ] Netsiz → xato xabari (online required)
```

### 5.4 Battle oqimi (2 qurilma)
```
[ ] Xona yaratish → PIN olish
[ ] PIN bilan qo'shilish
[ ] Kutish xonasida ishtirokchilar ko'rinadi
[ ] Host "boshlash" bosadi → 3 soniya countdown
[ ] Savol ko'rsatiladi (barcha qurilmalarda bir vaqtda)
[ ] Javob berish → tugma o'chiriladi
[ ] Vaqt tugaganda javob yuklanmagan → 0 ball
[ ] Reveal → to'g'ri/noto'g'ri + statistika
[ ] Oraliq leaderboard (5-savoldan keyin)
[ ] Yakuniy podium
[ ] XP beriladi
[ ] Rematch ishlaydi
[ ] 1 qurilma tarmoqdan chiqsa → reconnect
```

### 5.5 Vocabulary oqimi
```
[ ] Kartalar FSRS bo'yicha tartiblangan
[ ] Karta flip animatsiyasi
[ ] Audio to'g'ri talaffuz
[ ] "Qiyin/To'g'ri/Oson" → next_review to'g'ri hisoblandi
[ ] Offline ishlaydi (Room DB)
[ ] Sessiya tugaganda XP beriladi
```

### 5.6 Billing oqimi (Play sandbox)
```
[ ] Paywall to'g'ri vaqtda ko'rsatiladi
[ ] Narxlar to'g'ri (12 900 / 99 000 so'm)
[ ] 14 kunlik trial boshlanadi
[ ] To'lov oqimi (Play sandbox)
[ ] PRO feature unlock bo'ladi
[ ] Obunani bekor qilish
[ ] Muddati o'tsa → free ga qaytish
```

### 5.7 Offline testlar (Airplane mode)
```
[ ] Home ekrani ochiladi
[ ] Vocab kartalar ishlaydi
[ ] Yozilgan insho navbatda saqlanadi
[ ] Online bo'lganda sync ishlaydi
[ ] AI Speaking → xato xabari (online required)
[ ] Battle → xato xabari (online required)
```

---

## 6. Performance Benchmarks

| Mezon | Maqsad | O'lchash |
|-------|--------|----------|
| Cold start | < 2 saniya | Android Profiler |
| Home load | < 500ms | Tracing |
| AI feedback | < 8 saniya P95 | Server metrics |
| Battle question deliver | < 200ms P95 | WebSocket metrics |
| Vocab card flip | < 16ms (60fps) | Compose trace |
| Screen transition | < 300ms | Compose trace |
| Memory (normal) | < 200MB | Android Profiler |
| Memory (battle) | < 250MB | Android Profiler |
| Battery (1 soat) | < 5% drain | Battery Historian |
| App size | < 30MB | APK Analyzer |

---

## 7. CI Pipeline (GitHub Actions)

```yaml
# .github/workflows/ci.yml

on: [push, pull_request]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env: { POSTGRES_PASSWORD: test }
      redis:
        image: redis:7
    steps:
      - npm ci
      - npm run test:coverage
      - # Coverage < 80% → FAIL

  android-test:
    runs-on: ubuntu-latest
    steps:
      - ./gradlew test          # unit tests
      - ./gradlew koverReport   # coverage
      - # Coverage < 70% → FAIL

  android-lint:
    steps:
      - ./gradlew lint detekt   # lint + static analysis

  backend-build:
    needs: backend-test
    steps:
      - docker build -t api:$SHA .
      - docker push

  android-build:
    needs: android-test
    steps:
      - ./gradlew assembleRelease
      - # Play internal track upload (main only)
```

---

## 8. Bug Severity Darajalari

| Daraja | Ta'rif | SLA |
|--------|--------|-----|
| **P0 — Kritik** | Ilova ishlamaydi, ma'lumot yo'qolishi, billing xatosi | 4 soat |
| **P1 — Yuqori** | Writing/Speaking ishlamaydi, battle crash | 24 soat |
| **P2 — O'rta** | UI xatosi, noto'g'ri ball, animatsiya broken | 1 hafta |
| **P3 — Past** | Kichik UI, text xatosi, minor perf | 2 hafta |

---

## 9. Beta Testlash Rejasi

### 9.1 Alpha (20 tester, ichki)
```
Davomiylik: 2 hafta
Fokus: core loop (writing + speaking + vocab)
Mezonlar:
  - Crash-free ≥ 99%
  - Core loop completion ≥ 70%
  - P0/P1 bug yo'q
```

### 9.2 Closed Beta (100 user)
```
Davomiylik: 4 hafta
Fokus: barcha feature, battle
Mezonlar:
  - D7 ≥ 15%
  - Activation ≥ 50%
  - Rating ≥ 4.0 (in-app survey)
  - Crash-free ≥ 99%
  - Paid conversion ≥ 1%
```

### 9.3 Launch
```
Gate G4 o'tsa → Play open testing → Full release
```

---

## 10. Test Muhiti O'zgaruvchilari

```env
# test/.env.test
NODE_ENV=test
DATABASE_URL=postgresql://test:test@localhost:5433/ielts_test
REDIS_URL=redis://localhost:6380
GEMINI_API_KEY=MOCK          # haqiqiy API ishlatilmaydi
ELEVENLABS_API_KEY=MOCK
JWT_ACCESS_SECRET=test_secret
JWT_REFRESH_SECRET=test_refresh
PLAY_PACKAGE_NAME=com.test.ielts
```

---

*TEST.md v1.0 · 2026-11-09*

