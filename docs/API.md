# Inglish API — v1

Base URL (dev): `http://localhost:3000`

Barcha so'rov va javoblar JSON. `Content-Type: application/json`.

## Autentifikatsiya

- **Access token** — JWT, 15 daqiqa (`ACCESS_TOKEN_TTL`).
- **Refresh token** — opaque, 30 kun (`REFRESH_TOKEN_TTL_DAYS`), bazada faqat SHA-256 hash saqlanadi.
- Har `refresh` da eski refresh token bekor qilinadi va yangisi beriladi (**rotation**).
- Bekor qilingan refresh token qayta ishlatilsa, foydalanuvchining barcha aktiv refresh tokenlari bekor qilinadi.
- `Authorization: Bearer <accessToken>` himoyalangan endpointlar uchun.

## Endpointlar

### `GET /health`

Server tirikligini tekshiradi. Baza talab qilmaydi.

```json
{
  "status": "ok",
  "service": "inglish-api",
  "version": "0.1.0",
  "uptime": 98,
  "timestamp": "2026-11-04T11:11:06.011Z"
}
```

### `GET /v1/health`

Baza ulanishini tekshiradi. Baza ishlamasa `503` + `{"status":"degraded","db":"down"}`.

### `POST /v1/auth/register`

```json
{
  "email": "ali@example.com",
  "password": "StrongPass123!",
  "displayName": "Ali"
}
```

`201 Created`:

```json
{
  "user": {
    "id": "uuid",
    "email": "ali@example.com",
    "role": "USER",
    "emailVerified": false,
    "createdAt": "2026-11-04T11:10:00.000Z",
    "profile": {
      "displayName": "Ali",
      "uiLang": "uz",
      "sourceLang": "uz",
      "targetLang": "en",
      "cefrLevel": null,
      "targetBand": null,
      "examDate": null,
      "onboardingDone": false
    }
  },
  "accessToken": "eyJ...",
  "refreshToken": "opaque-token",
  "expiresIn": 900
}
```

Ro'yxatdan o'tishda `FREE` subscription avtomatik yaratiladi.

Xatolar:

- `400 VALIDATION_ERROR` — email/parol/ism talabga mos emas.
- `409 EMAIL_IN_USE` — email allaqachon ro'yxatdan o'tgan.
- `429 RATE_LIMITED` — 5 urinish/daqiqa.

### `POST /v1/auth/login`

```json
{
  "email": "ali@example.com",
  "password": "StrongPass123!"
}
```

`200` — `register` bilan bir xil javob.

- `401 UNAUTHORIZED` — email yoki parol noto'g'ri.
- `429 RATE_LIMITED` — 10 urinish/daqiqa.

### `POST /v1/auth/refresh`

```json
{ "refreshToken": "opaque-token" }
```

`200`:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "yangi-opaque-token",
  "expiresIn": 900
}
```

- `401 UNAUTHORIZED` — token bekor qilingan, muddati o'tgan yoki qayta ishlatilgan.
- `429 RATE_LIMITED` — 30 urinish/daqiqa.

### `POST /v1/auth/logout`

```json
{ "refreshToken": "opaque-token" }
```

`204 No Content`. Token allaqachon bekor qilingan bo'lsa ham `204`.

### `POST /v1/auth/verify-email/request`

`Authorization` talab qilinadi. Joriy foydalanuvchining emailiga 6 xonali tasdiqlash kodini yuboradi; avvalgi kod bekor qilinadi.

`200`:

```json
{ "alreadyVerified": false, "expiresAt": "2026-11-06T14:15:00.000Z" }
```

- `alreadyVerified: true` — email allaqachon tasdiqlangan, yangi kod yuborilmaydi.
- `429 RATE_LIMITED` — 3 urinish/daqiqa.

### `POST /v1/auth/verify-email/confirm`

`Authorization` talab qilinadi.

```json
{ "code": "123456" }
```

`200`:

```json
{ "emailVerified": true }
```

- `400 CODE_INVALID` — kod noto'g'ri (urinish hisoblanadi).
- `400 CODE_EXPIRED` — kod muddati tugagan (`AUTH_CODE_TTL_MINUTES`, default 15 daqiqa).
- `400 CODE_LOCKED` — `AUTH_CODE_MAX_ATTEMPTS` (default 5) marta xato kiritilgach kod bekor qilinadi.

### `POST /v1/auth/password/forgot`

```json
{ "email": "ali@example.com" }
```

`204 No Content`. Email mavjudligini oshkor qilmaslik uchun har doim `204`.

- `429 RATE_LIMITED` — 3 urinish/daqiqa.

### `POST /v1/auth/password/reset`

```json
{ "email": "ali@example.com", "code": "123456", "password": "YangiParol123" }
```

`204 No Content`. Parol yangilanadi va barcha refresh tokenlar bekor qilinadi — barcha qurilmalar qaytadan kirishi kerak.

- `400 CODE_INVALID` / `CODE_EXPIRED` / `CODE_LOCKED`.
- `429 RATE_LIMITED` — 5 urinish/daqiqa.

### Email rejimlari

`EMAIL_MODE=auto|log|smtp`:

- `auto` — `SMTP_HOST` mavjud bo'lsa `smtp`, aks holda `log`.
- `log` — xat matni server logiga yoziladi, tashqi yuborilmaydi (faqat dev).
- `smtp` — `SMTP_HOST`, `SMTP_PORT`, `SMTP_SECURE`, `SMTP_USER`, `SMTP_PASS`, `EMAIL_FROM` orqali yuboriladi.

`NODE_ENV=production` da `EMAIL_MODE=log` bilan server ishga tushmaydi.

### `GET /v1/dev/outbox`

Faqat `NODE_ENV !== production` da ro'yxatga olinadi. `Authorization` talab qilinadi va faqat joriy foydalanuvchi emailiga yuborilgan oxirgi 10 xatni qaytaradi:

```json
{ "messages": [{ "to": "ali@example.com", "subject": "Inglish: emailni tasdiqlang", "text": "...", "sentAt": "..." }] }
```

Smoke testlar 6 xonali kodni shu yerdan oladi.

### `GET /v1/me`

`Authorization: Bearer <accessToken>` talab qilinadi.

```json
{ "user": { "...": "PublicUser" } }
```

- `401 UNAUTHORIZED` — token yo'q yoki yaroqsiz.

### `PUT /v1/me/profile`

`Authorization` talab qilinadi. Kamida bitta maydon yuborilishi kerak.

```json
{
  "displayName": "Ali",
  "cefrLevel": "B2",
  "targetBand": 6.5,
  "examDate": "2027-02-14",
  "onboardingDone": true
}
```

`cefrLevel`: `A2` | `B1` | `B2` | `C1` | `C2` yoki `null`.
`targetBand`: 4.0–9.0, 0.5 qadam bilan, yoki `null`.
`examDate`: `YYYY-MM-DD` yoki `null`.

`200` — `{ "user": { "...": "PublicUser" } }`.

### `DELETE /v1/me`

`Authorization` talab qilinadi. Akkauntni soft-delete qiladi va barcha refresh
tokenlarni bekor qiladi. `204 No Content`. Shundan so'ng mavjud access token ham
darhol kuchini yo'qotadi: barcha `Authorization` talab qiladigan endpointlar `401`
qaytaradi. Qayta kirish ham `401` qaytaradi.

## Content

### `GET /v1/content/units`

Query:

- `skill` — `WRITING` | `SPEAKING` | `VOCABULARY`
- `level` — `A2`, `B1`, `B2`, `C1`, `C2`

```json
{
  "units": [
    {
      "id": "uuid",
      "slug": "writing-task-2-opinion",
      "title": "Opinion essay",
      "description": "...",
      "skill": "WRITING",
      "cefrLevel": "B2",
      "order": 1,
      "lessonCount": 2
    }
  ]
}
```

### `GET /v1/content/units/:slug`

Unit, lesson va exercise'larni qaytaradi. **Javob kaliti (`answerJson`) va izoh
(`explanation`) bu endpointda qaytarilmaydi** — ular faqat javob tekshirilgandan
keyin ochiladi. Bu clientdan javobni oldindan ko'rish (cheat) yo'lini yopadi.

### `POST /v1/content/exercises/:id/answer`

`Authorization` talab qilinadi.

Multiple choice:

```json
{ "answer": 1 }
```

Gap fill / short answer:

```json
{ "answer": "However" }
```

```json
{
  "result": {
    "isCorrect": true,
    "correctAnswer": 1,
    "explanation": "..."
  }
}
```

- `MULTIPLE_CHOICE` — `answer` option indeksi, `isCorrect` `true`/`false`.
- `GAP_FILL` — matn; registr, ortiqcha bo'shliq va apostrof shakli hisobga olinmaydi.
- `SHORT_ANSWER` — matn; `isCorrect` `null` bo'ladi va `correctAnswer` sifatida
  namunaviy javob qaytadi (server ochiq javobni avtomatik baholamaydi).

## Vocabulary

Barcha vocabulary endpointlari `Authorization` talab qiladi.

### `GET /v1/vocabulary?q=&tag=&limit=50&offset=0`

`q` — so'z yoki tarjima bo'yicha qidiruv. `tag` — to'plam tegi (masalan `set-education`).
`limit` — 1..200, `offset` — 0 dan boshlanadi.

```json
{
  "items": [],
  "total": 50,
  "limit": 50,
  "offset": 0
}
```

### `GET /v1/vocabulary/sets`

10 ta IELTS mavzu to'plami va har biridagi so'zlar soni.

```json
{
  "sets": [
    {
      "slug": "education",
      "title": "Ta’lim",
      "description": "Universitet, maktab va o‘qish jarayoni haqidagi so‘zlar.",
      "tag": "set-education",
      "count": 50
    }
  ]
}
```

### `GET /v1/vocabulary/due?limit=20`

Takrorlash vaqti kelgan so'zlar + yangi so'zlar. Yangi so'zlar CEFR darajasi
bo'yicha (B1 → B2 → C1) tartiblanadi. `limit` — 1..50.

### `GET /v1/vocabulary/stats`

```json
{
  "stats": {
    "total": 520,
    "new": 500,
    "due": 20,
    "learning": 0,
    "review": 0
  }
}
```

### `POST /v1/vocabulary/:id/review`

```json
{ "rating": 3 }
```

`rating`: `1` — Again, `2` — Hard, `3` — Good, `4` — Easy.

```json
{
  "review": {
    "vocabularyId": "uuid",
    "state": "REVIEW",
    "dueAt": "2026-11-07T11:00:00.000Z",
    "stability": 2,
    "difficulty": 5,
    "reps": 1,
    "lapses": 0,
    "intervalDays": 2
  },
  "item": { "id": "uuid", "word": "significant", "definitionUz": "..." }
}
```

## Writing

### `GET /v1/writing/tasks?type=TASK_2`

`type` — `TASK_1` | `TASK_2`.

### `GET /v1/writing/tasks/:slug`

Task + `sampleAnswer` + `bandDescriptors`.

### `POST /v1/writing/submissions`

`Authorization` talab qilinadi.

```json
{
  "taskSlug": "writing-task-2-opinion",
  "text": "Nowadays ..."
}
```

`201` — draft submission. Matn 1..20 000 belgi.

### `POST /v1/writing/submissions/:id/submit`

Draftni AI baholashga yuboradi. Kamida 20 so'z talab qilinadi.

- `409 SCORING_IN_PROGRESS` — shu submission hozir baholanmoqda (2 daqiqa ichida).
- `429 QUOTA_EXCEEDED` — oylik Writing limiti tugagan.
- `502 AI_SCORING_FAILED` — AI provayder xatosi; qayta urinish mumkin.

Javobda `bandEstimate`, `confidenceLow`, `confidenceHigh`, `feedback`, `modelVersion` bo'ladi. `feedback` — `WritingFeedback`:

```json
{
  "overallBand": 6.0,
  "confidenceLow": 5.5,
  "confidenceHigh": 6.5,
  "criteria": [
    { "name": "task_response", "band": 6.0, "comment": "..." }
  ],
  "strengths": ["..."],
  "improvements": ["..."],
  "corrections": [
    { "original": "...", "corrected": "...", "explanation": "..." }
  ],
  "nextStep": "...",
  "disclaimer": "AI practice estimate, not an official IELTS score."
}
```

### `GET /v1/writing/submissions?limit=20`

Foydalanuvchining submissionlari ro'yxati.

### `GET /v1/writing/submissions/:id`

Bitta submission + task + feedback.

## Speaking

### `GET /v1/speaking/prompts?part=PART_2`

`part` — `PART_1` | `PART_2` | `PART_3`.

### `GET /v1/speaking/prompts/:slug`

### `POST /v1/speaking/submissions`

`Authorization` talab qilinadi.

```json
{
  "promptSlug": "speaking-part-2-place",
  "transcript": "I would like to talk about ...",
  "durationSeconds": 95
}
```

`201` — draft submission. Transcript 1..20 000 belgi.

### `POST /v1/speaking/submissions/:id/submit`

Transcriptni AI baholashga yuboradi. Kamida 20 belgi talab qilinadi.

- `409 SCORING_IN_PROGRESS`
- `429 QUOTA_EXCEEDED`
- `502 AI_SCORING_FAILED`

Muhim: serverga faqat transcript yuboriladi. Android ilova nutqni qurilmaning o'zida (STT) transkriptga aylantiradi; audio yuklanmaydi, shuning uchun **talaffuz baholanmaydi** va `pronunciation` mezoni `null` bo'ladi.

### `GET /v1/speaking/submissions?limit=20`

### `GET /v1/speaking/submissions/:id`

## Subscription va quota

### `GET /v1/subscription`

`Authorization` talab qilinadi.

```json
{
  "subscription": {
    "plan": "FREE",
    "status": "ACTIVE",
    "startedAt": "2026-11-04T11:10:00.000Z",
    "expiresAt": null,
    "provider": null
  }
}
```

`plan`: `FREE` | `PRO` | `TEACHER`. `status`: `ACTIVE` | `GRACE` | `ON_HOLD` | `CANCELED` | `EXPIRED`.

### `GET /v1/billing/products`

Ochiq endpoint. Google Play'da yaratilgan subscription mahsulotlari.

```json
{
  "products": [
    {
      "productId": "inglish_pro_monthly",
      "plan": "PRO",
      "title": "Pro - oylik",
      "description": "Cheksizga yaqin Writing/Speaking kvotasi, Battle 10 o'yinchi",
      "days": 30
    }
  ],
  "packageName": "uz.inglish.app"
}
```

### `GET /v1/billing/entitlement`

`Authorization` talab qilinadi. Joriy reja, amal qilish muddati va limitlar.

```json
{
  "entitlement": {
    "plan": "PRO",
    "status": "ACTIVE",
    "startedAt": "2026-11-06T12:00:00.000Z",
    "expiresAt": "2026-12-06T12:00:00.000Z",
    "provider": "GOOGLE_PLAY",
    "autoRenewing": true,
    "limits": { "speakingPerDay": 15, "writingPerMonth": 4 }
  }
}
```

### `POST /v1/billing/verify`

`Authorization` talab qilinadi. Play Billing'dan olingan `purchaseToken` serverda
`purchases.subscriptionsv2.get` bilan tekshiriladi, 3 kun ichida acknowledge
qilinadi va entitlement yangilanadi. Client token'ga ishonmaydi.

```json
{ "purchaseToken": "gp-token...", "productId": "inglish_pro_monthly" }
```

Javob `201` (yangi xarid) yoki `200` (idempotent qayta tekshirish):

```json
{
  "verified": true,
  "purchase": {
    "productId": "inglish_pro_monthly",
    "purchaseToken": "gp-token...",
    "orderId": "GPA.1234-...",
    "purchaseTime": "2026-11-06T12:00:00.000Z",
    "expiryTime": "2026-12-06T12:00:00.000Z",
    "autoRenewing": true,
    "mock": false
  },
  "entitlement": { "plan": "PRO", "...": "..." }
}
```

Xatolar:

- `400 PURCHASE_INVALID` / `PURCHASE_NOT_FOUND` - token Play'da topilmadi.
- `409 PURCHASE_ALREADY_CLAIMED` - token boshqa akkauntga bog'langan.
- `409 PURCHASE_INVALID` - xarid refund qilingan yoki muddati tugagan.
- `502 BILLING_VERIFY_FAILED` - Google Play API javob bermadi.
- `503 BILLING_NOT_CONFIGURED` - service account sozlanmagan.

### `POST /v1/billing/restore`

`Authorization` talab qilinadi. Qurilmadagi barcha faol Play xaridlarini qayta
tekshiradi (restore purchases tugmasi).

```json
{ "purchases": [{ "purchaseToken": "gp-token...", "productId": "inglish_pro_monthly" }] }
```

```json
{
  "restored": [{ "purchaseId": "...", "plan": "PRO", "status": "ACTIVE", "expiresAt": "..." }],
  "skipped": [{ "purchaseToken": "gp-token...", "reason": "PURCHASE_ALREADY_CLAIMED" }],
  "entitlement": { "plan": "PRO", "...": "..." }
}
```

### `POST /v1/billing/rtdn`

Google Pub/Sub push webhook. `message.data` base64 ichida `DeveloperNotification`
keladi. Autentifikatsiya:

- `PLAY_RTDN_AUDIENCE` sozlangan bo'lsa - `Authorization: Bearer <OIDC JWT>` tekshiriladi.
- `PLAY_RTDN_TOKEN` sozlangan bo'lsa - bearer yoki `x-rtdn-token` sarlavhasi solishtiriladi.
- Production'da ikkalasi ham bo'sh bo'lsa - `503 RTDN_NOT_CONFIGURED`.
- Development'da autentifikatsiya talab qilinmaydi.

`subscriptionNotification` va `voidedPurchaseNotification` qo'llab-quvvatlanadi.
Refund/chargeback kelganda subscription darhol `FREE`ga tushiriladi.

### Billing rejimlari

| `BILLING_MODE` | Xatti-harakat |
|---|---|
| `auto` (default) | Service account bo'lsa Google, bo'lmasa mock |
| `google` | Faqat Google Play Developer API |
| `mock` | `mock:<productId>:<days>` tokenlari (faqat dev) |

Debug APK'da `BILLING_MOCK=true` bo'ladi, shuning uchun Play'siz qurilmada ham
paywall to'liq sinaladi. Release APK'da bu qiymat `false`.

**Trial va refund.** 14 kunlik bepul trial Play Console'da offer sifatida
sozlanadi (kodda alohida yo'l yo'q): trial davrida subscription `ACTIVE` bo'ladi
va `expiryTime` Play'dan olinadi. Refund/chargeback `voidedPurchaseNotification`
orqali keladi va subscription darhol `FREE`ga tushiriladi (48 soatlik Play
qaytarish oynasi bilan mos).

### `GET /v1/usage`

`Authorization` talab qilinadi. Kunlik Speaking va oylik Writing limitlari Tashkent vaqti (UTC+5) bo'yicha hisoblanadi.

```json
{
  "usage": {
    "plan": "FREE",
    "speaking": {
      "used": 1,
      "limit": 3,
      "remaining": 2,
      "resetsAt": "2026-11-06T19:00:00.000Z"
    },
    "writing": {
      "used": 0,
      "limit": 1,
      "remaining": 1,
      "resetsAt": "2026-11-30T19:00:00.000Z"
    }
  }
}
```

| Plan | Speaking / kun | Writing / oy |
|---|---:|---:|
| FREE | 3 | 1 |
| PRO | 15 | 4 |
| TEACHER | 30 | 10 |

## Progress

### `GET /v1/progress`

`Authorization` talab qilinadi. Foydalanuvchining haqiqiy mashq tarixidan
hisoblangan taraqqiyoti. Band qiymatlari — AI **practice estimate**, rasmiy IELTS
balli emas.

```json
{
  "progress": {
    "examDate": "2027-03-15",
    "daysToExam": 129,
    "targetBand": 6.5,
    "currentBand": 6.0,
    "bandGap": 0.5,
    "streakDays": 3,
    "writing": {
      "scoredCount": 4,
      "latestBand": 6.0,
      "bestBand": 6.5,
      "recent": [
        { "id": "…", "band": 6.0, "scoredAt": "2026-11-06T10:00:00.000Z" }
      ]
    },
    "speaking": {
      "scoredCount": 2,
      "latestBand": 6.0,
      "bestBand": 6.0,
      "recent": []
    },
    "vocabulary": {
      "total": 120,
      "reviewed": 18,
      "due": 7,
      "learned": 4
    },
    "exercises": {
      "attempted": 24,
      "correct": 19,
      "accuracy": 0.792
    }
  }
}
```

- `currentBand` — Writing va Speaking oxirgi baholarining o'rtachasi, 0.5 qadamga
  yaxlitlangan. Baho bo'lmasa `null`.
- `bandGap` — `targetBand - currentBand`, bittasi yo'q bo'lsa `null`.
- `streakDays` — UTC kunlari bo'yicha ketma-ket faol kunlar. Faollik: baholangan
  writing/speaking, lug'at takrori yoki avtomatik tekshiriladigan mashq.
- `exercises.accuracy` — faqat avtomatik baholanadigan urinishlar (`MULTIPLE_CHOICE`,
  `GAP_FILL`) hisobga olinadi; ochiq `SHORT_ANSWER` kirmaydi.
- `vocabulary.due` — muddati kelgan takrorlashlar + hali ko'rilmagan yangi so'zlar.

## Placement

Placement — foydalanuvchining darajasini aniqlovchi qisqa test (20 daqiqa, 25 savol +
yozuv + gapirish): 10 grammatika, 10 lug'at, 5 o'qish, 60+ so'zlik yozuv paragrafi va
1 daqiqalik gapirish transkripti. Savollar serverda snapshot qilinadi, javob kaliti
javob berilgunga qadar mijozga yuborilmaydi.

### `POST /v1/placement/sessions`

`Authorization` talab qilinadi. Yangi placement sessiyasini yaratadi. Javob `201`:

```json
{
  "session": {
    "id": "…",
    "status": "ACTIVE",
    "questions": [
      { "id": "g1", "section": "GRAMMAR", "prompt": "…", "passage": null, "options": ["…", "…", "…", "…"] }
    ],
    "writing": { "slug": "placement-writing", "title": "Placement yozuv", "prompt": "…", "minWords": 60, "timeLimitMinutes": 5, "text": null, "band": null, "feedback": null },
    "speaking": { "slug": "placement-speaking", "part": "PART_1", "topic": "Future plans", "questions": ["…"], "prepTimeSeconds": 0, "speakTimeSeconds": 60, "transcript": null, "band": null, "feedback": null },
    "result": null
  }
}
```

### `GET /v1/placement/sessions/:id`

`Authorization` talab qilinadi. Faqat sessiya egasi ko'ra oladi.

### `POST /v1/placement/sessions/:id/submit`

`Authorization` talab qilinadi. Barcha savollarga javoblarni yozuv va gapirish
transkripti bilan birga yuboradi:

```json
{
  "answers": [{ "questionId": "g1", "answerIndex": 2 }],
  "writingText": "…60+ so'z…",
  "speakingTranscript": "…20+ so'z…"
}
```

Tugallanmagan javob `400 INCOMPLETE_PLACEMENT`, takroriy topshirish
`409 PLACEMENT_FINISHED`, qisqa yozuv/transkript `400 SUBMISSION_TOO_SHORT`.
Yozuv va gapirish mavjud AI scorer (Gemini/mock) orqali baholanadi; yakuniy
`estimatedBand` — MCQ bandi, yozuv bandi va gapirish bandining o'rtachasi (0.5 ga
yaxlitlangan). Javob `session.result` bilan qaytadi:

```json
{
  "session": {
    "id": "…",
    "status": "FINISHED",
    "result": {
      "correctCount": 19,
      "totalCount": 25,
      "percentage": 76,
      "cefrLevel": "C1",
      "estimatedBand": 6,
      "weakSkill": "VOCABULARY",
      "sectionStats": {
        "GRAMMAR": { "correct": 8, "total": 10 },
        "VOCABULARY": { "correct": 7, "total": 10 },
        "READING": { "correct": 4, "total": 5 }
      }
    }
  }
}
```

`estimatedBand` — AI/practice estimate, rasmiy IELTS balli emas. Natija profilingdagi
`cefrLevel` ni yangilaydi va onboardingni yakunlaydi.

### `GET /v1/placement/latest`

`Authorization` talab qilinadi. So'nggi yakunlangan natijani qaytaradi.

## Battle

Battle — Kahoot uslubidagi host-driven viktorina. Uch rejim bor:

| Rejim | Kim uchun | Xona kodi | Maks. o'yinchi |
|---|---|---|---|
| `BOT` | yolg'iz mashq / onboarding | yo'q | 2 |
| `FRIEND` | do'stlar (B2C) | 6 raqam | 10 (PRO 10) |
| `CLASSROOM` | o'qituvchi va sinf (B2B) | 6 raqam | 10 (TEACHER 30) |

O'yin **server-authoritative**: savollar, to'g'ri javob, javob vaqti va ballar
serverda hisoblanadi. Javob kaliti (`correctIndex`, `explanation`) faqat `REVEAL`
fazasida mijozga yuboriladi. Har bir o'yin 5–20 savoldan iborat (standart 10),
har bir savolga **10 / 20 / 30 soniya**, reveal fazasi **4 soniya**.

Room holati: `LOBBY` → `ACTIVE` → `FINISHED`.
Faza: `LOBBY`, `QUESTION`, `REVEAL`. Reveal tugagach server avtomatik keyingi
savolga o'tadi; mijozdan "keyingi savol" amali talab qilinmaydi.

### `POST /v1/battle/rooms`

`Authorization` talab qilinadi. Yangi xona yaratadi. `BOT` rejimida o'yin darhol
`ACTIVE` bo'ladi; `FRIEND` / `CLASSROOM` rejimida `LOBBY` bo'ladi.

```json
{
  "mode": "FRIEND",
  "questionCount": 10,
  "timeLimitMs": 20000,
  "autoBot": true
}
```

| Maydon | Qiymat |
|---|---|
| `mode` | `BOT` (default), `FRIEND`, `CLASSROOM` |
| `questionCount` | 5–20, default 10 |
| `timeLimitMs` | 10000, 20000, 30000; default 20000 |
| `autoBot` | `FRIEND` xonasida 15 soniyadan keyin bo'sh joyga bot qo'shish |

Javob `201`:

```json
{
  "room": {
    "id": "…",
    "code": "482913",
    "mode": "FRIEND",
    "status": "LOBBY",
    "phase": "LOBBY",
    "questionCount": 10,
    "currentPosition": 1,
    "timeLimitMs": 20000,
    "revealMs": 4000,
    "maxPlayers": 10,
    "hostParticipantId": "…",
    "serverNow": 1793942000000,
    "startedAt": null,
    "scores": [
      {
        "id": "…",
        "name": "Aziz",
        "isBot": false,
        "isHost": true,
        "isReady": true,
        "isOnline": false,
        "score": 0,
        "correctCount": 0,
        "streak": 0,
        "bestStreak": 0,
        "answeredCurrent": false,
        "rank": 1
      }
    ],
    "currentQuestion": null,
    "reveal": null,
    "finished": false,
    "results": null
  },
  "participantId": "…"
}
```

`BOT` rejimida javobda `currentQuestion` darhol keladi:

```json
{
  "currentQuestion": {
    "id": "…",
    "position": 1,
    "source": "VOCABULARY",
    "prompt": "What does “mitigate” mean?",
    "options": ["…", "…", "…", "…"],
    "endsAt": "2026-11-06T05:20:20.000Z"
  }
}
```

`currentQuestion`da `correctIndex` va `explanation` **yo'q**.

### `POST /v1/battle/rooms/join`

`Authorization` talab qilinadi. 6 xonali kod bilan `LOBBY` holatidagi xonaga
qo'shiladi. O'yin boshlanganidan keyin kech qo'shilish `ROOM_ALREADY_STARTED`
bilan rad etiladi (v1 da spectator rejimi yo'q).

```json
{ "code": "482913" }
```

Javob `200`: `{ "room": { … }, "participantId": "…" }`.

### `GET /v1/battle/rooms/:id`

`Authorization` talab qilinadi. Faqat xona ishtirokchisi ko'ra oladi. Faol o'yinda
joriy savolni, tugagan o'yinda `results` ro'yxatini qaytaradi. Bu endpoint
WebSocket uzilganda REST fallback va reconnect uchun ishlatiladi.

### `POST /v1/battle/rooms/:id/ready`

Faqat `LOBBY`da. Tana: `{ "ready": true }`. Javob: `{ "room": { … } }`.
Xost o'yinni boshlashdan oldin mehmonlar tayyor bo'lishi kerak.

### `POST /v1/battle/rooms/:id/bots`

Faqat xost va faqat `LOBBY`da. Xonaga bitta AI bot qo'shadi. Javob:
`{ "room": { … } }`.

### `POST /v1/battle/rooms/:id/start`

Faqat xost. Kamida 2 faol o'yinchi va xostdan tashqari barcha inson o'yinchilar
`isReady = true` bo'lishi kerak. Javob: `{ "room": { … } }`. O'yin `ACTIVE`ga
o'tadi va birinchi savol boshlanadi.

### `POST /v1/battle/rooms/:id/answer`

`Authorization` talab qilinadi. Joriy savolga javob beradi; botlarning javobi
ham serverda hisoblanadi.

```json
{
  "roundId": "…",
  "answerIndex": 2
}
```

`answerIndex` — 0-based variant indeksi. Vaqt tugab, o'yinchi variantni
tasdiqlamagan bo'lsa, ilova `-1` yuboradi (noto'g'ri javob sifatida baholanadi).
Javob vaqti **serverda** savol boshlangan paytdan o'lchanadi; mijoz yuborgan
vaqtga ishonilmaydi.

Javob:

```json
{
  "room": { "…": "yangilangan room, reveal yoki keyingi savol" },
  "participantId": "…",
  "result": {
    "roundId": "…",
    "isCorrect": true,
    "correctIndex": 2,
    "explanation": "“Mitigate” — kamaytirish. Example: …",
    "scoreGained": 780,
    "responseMs": 4200,
    "answerIndex": 2
  }
}
```

`result` faqat savol `REVEAL` fazasiga o'tganda to'ldiriladi; aks holda `null`.
Reveal ma'lumoti `room.reveal`da ham keladi:

```json
{
  "reveal": {
    "roundId": "…",
    "position": 1,
    "correctIndex": 2,
    "explanation": "…",
    "endsAt": "2026-11-06T05:20:24.000Z",
    "answers": [
      {
        "participantId": "…",
        "answerIndex": 2,
        "isCorrect": true,
        "responseMs": 4200,
        "scoreGained": 780
      }
    ]
  }
}
```

Ballash:

- to'g'ri javob: **500** ball;
- tezlik bonusi: **0–500** ball (server o'lchagan javob vaqti `timeLimitMs`ga nisbatan);
- 3 va undan ortiq ketma-ket to'g'ri javob: **+100** ball;
- noto'g'ri javob yoki timeout: **0** ball.

`result.responseMs` — server o'lchagan javob vaqti, `0…timeLimitMs` oralig'ida.
Bitta savolga ikki marta javob berib bo'lmaydi. O'yin tugagach `room.finished = true`,
`room.results` ball bo'yicha tartiblangan va `rank` berilgan bo'ladi.

### `POST /v1/battle/rooms/:id/leave`

`Authorization` talab qilinadi. `LOBBY`da xost xonani yopadi (`{ "closed": true }`);
mehmon xonadan chiqariladi. `ACTIVE`da o'yinchi `leftAt` bilan belgilanadi va
keyingi savollarda hisoblanmaydi. `FINISHED`da hech narsa o'zgarmaydi.

### `GET /v1/battle/packs`

`Authorization` talab qilinadi. Video darslik va viktorina to'plamlari (Battle Packs) ro'yxatini qaytaradi.
Query parametrlari:
- `q`: qidiruv so'zi (sarlavha/tavsif bo'yicha)
- `tag`: filtr tegi
- `limit`: sahifadagi elementlar soni (default 20, max 50)
- `offset`: sahifa siljishi (default 0)

### `POST /v1/battle/packs`

`Authorization` talab qilinadi. Yangi video darslik paketi yaratadi (`Idempotency-Key` qo'llab-quvvatlanadi).
Tana:
```json
{
  "title": "IELTS Speaking Part 2 Masterclass",
  "description": "Video dars va mavzulashtirilgan tezkor viktorina",
  "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "tags": ["speaking", "part2", "band7"],
  "questions": [
    {
      "prompt": "What tense should you primarily use for past experiences?",
      "options": ["Past Simple", "Present Continuous", "Future Perfect", "Present Simple"],
      "correctIndex": 0,
      "explanation": "Past experiences are narrated using Past Simple."
    }
  ]
}
```

### `GET /v1/battle/packs/:id`

`Authorization` talab qilinadi. Bitta paketning to'liq tafsilotlarini qaytaradi.

### `PUT /v1/battle/packs/:id`

`Authorization` talab qilinadi. Paket yaratuvchisi o'z paketini yangilashi mumkin.

### `DELETE /v1/battle/packs/:id`

`Authorization` talab qilinadi. Paket yaratuvchisi paketni o'chiradi.

### `POST /v1/battle/packs/:id/play`

`Authorization` talab qilinadi. Tanlangan paket savollari va videosi asosida yangi Battle xonasini ishga tushiradi (`FRIEND` yoki `CLASSROOM` rejimida).

```json
{
  "mode": "FRIEND",
  "shuffle": false
}
```

Javob `201`: Yangi yaratilgan Battle Room obyekti.

### `GET /v1/battle/rooms/:id/ws?token=<accessToken>`

WebSocket kanali. Faqat live room snapshot va eventlar yuboriladi; barcha o'yin
amallari REST orqali bajariladi. Token query parametrida uzatiladi. Token
loglarga tushmasligi uchun production'da reverse proxy loglarini tozalash kerak.

Serverdan keladigan xabarlar:

```json
{ "type": "room", "room": { … } }
{ "type": "room-closed", "code": "ROOM_CLOSED", "message": "Xona yopildi." }
{ "type": "error", "code": "UNAUTHORIZED", "message": "Token yaroqsiz." }
{ "type": "pong" }
```

Ulanish ochilganda server darhol `room` snapshot yuboradi. Har bir REST amali
(o'yinchi qo'shildi, ready, start, answer, leave)dan keyin yangi snapshot
broadcast qilinadi. Mijoz `ping` yuborishi mumkin; server `pong` qaytaradi.

WebSocket uzilsa, mijoz `GET /v1/battle/rooms/:id` bilan holatni yangilaydi va
qayta ulanadi. Access token muddati tugagan bo'lsa, REST so'rov tokenni yangilaydi.
Xona `FINISHED` bo'lgach kanal yopiladi.

### Deep link

Xona kodini ulashish uchun: `inglish://battle/<6-raqamli-kod>`.
Ilova kodni ochib, `POST /v1/battle/rooms/join` orqali xonaga qo'shiladi.

### Battle xatolari

`ROOM_FULL` (409), `ROOM_ALREADY_STARTED` (409), `ROOM_EXPIRED` (409),
`PLAYERS_NOT_READY` (409), `NOT_ENOUGH_PLAYERS` (409), `ROOM_CODE_EXHAUSTED` (409),
`BATTLE_FINISHED` (409), `BATTLE_ROUND_MISMATCH` (409), `BATTLE_ROUND_CLOSED` (409),
`BATTLE_ALREADY_ANSWERED` (409), `NOT_ENOUGH_QUESTIONS` (409), `INVALID_ANSWER` (400).

## Idempotency kalitlari

Mutatsiya qiluvchi muhim endpointlar `Idempotency-Key` headerni qo'llab-quvvatlaydi. Bu tarmoq uzilib javob yo'qolganda bir xil amalning ikki marta bajarilishini oldini oladi (masalan, AI baholash yoki to'lovni tekshirish).

- Header: `Idempotency-Key: <8–128 belgi>`; ruxsat etilgan belgilar `A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, `-`.
- Kalit foydalanuvchi bo'yicha yagona va **24 soat** saqlanadi.
- Birinchi muvaffaqiyatli (**2xx**) javob saqlanadi. Xuddi shu kalit va bir xil body bilan qayta so'rov yuborilsa, server saqlangan javobni `Idempotency-Replayed: true` header bilan qaytaradi va amalni takrorlamaydi.
- Xuddi shu kalit boshqa body bilan yuborilsa — `409 IDEMPOTENCY_KEY_REUSED`.
- Kalit formati noto'g'ri bo'lsa — `400 IDEMPOTENCY_KEY_INVALID`.
- 4xx/5xx javoblar saqlanmaydi: xato holatida qayta urinish amalni qaytadan bajaradi.

Qo'llab-quvvatlanadigan endpointlar:

- `POST /v1/writing/submissions/:id/submit`
- `POST /v1/speaking/submissions/:id/submit`
- `POST /v1/placement/sessions/:id/submit`
- `POST /v1/billing/verify`
- `POST /v1/battle/rooms`
- `POST /v1/vocabulary/:id/review`

Misol:

```bash
curl -X POST http://localhost:3000/v1/billing/verify \
  -H "Authorization: Bearer <accessToken>" \
  -H "Idempotency-Key: verify-7f3c9a2b" \
  -H "Content-Type: application/json" \
  -d '{"purchaseToken":"mock:inglish_pro_monthly:30:demo"}'
```

## Xato formati

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request body",
  "details": {
    "formErrors": [],
    "fieldErrors": { "email": ["Invalid email"] }
  }
}
```

| Code | HTTP | Ma'nosi |
|---|---|---|
| `VALIDATION_ERROR` | 400 | So'rov formati noto'g'ri |
| `SUBMISSION_TOO_SHORT` | 400 | Matn/transcript juda qisqa |
| `CODE_INVALID` | 400 | Tasdiqlash/tiklash kodi noto'g'ri |
| `CODE_EXPIRED` | 400 | Kod muddati tugagan |
| `CODE_LOCKED` | 400 | Urinishlar soni oshib ketgan, yangi kod kerak |
| `IDEMPOTENCY_KEY_INVALID` | 400 | `Idempotency-Key` formati noto'g'ri |
| `UNAUTHORIZED` | 401 | Token yo'q / yaroqsiz |
| `FORBIDDEN` | 403 | Ruxsat yo'q |
| `NOT_FOUND` | 404 | Topilmadi |
| `EMAIL_IN_USE` | 409 | Email band |
| `IDEMPOTENCY_KEY_REUSED` | 409 | Kalit boshqa body bilan ishlatilgan |
| `SCORING_IN_PROGRESS` | 409 | Submission allaqachon baholanmoqda |
| `ROOM_FULL` | 409 | Battle xonasi to'lgan |
| `ROOM_ALREADY_STARTED` | 409 | Battle allaqachon boshlangan |
| `PLAYERS_NOT_READY` | 409 | Barcha o'yinchilar tayyor emas |
| `NOT_ENOUGH_PLAYERS` | 409 | Kamida 2 o'yinchi kerak |
| `BATTLE_ROUND_CLOSED` | 409 | Savol vaqti tugagan |
| `BATTLE_ALREADY_ANSWERED` | 409 | Bu savolga allaqachon javob berilgan |
| `QUOTA_EXCEEDED` | 429 | Tarif limiti tugagan |
| `RATE_LIMITED` | 429 | So'rovlar soni limiti |
| `AI_SCORING_FAILED` | 502 | AI provayder xatosi |
| `INTERNAL_ERROR` | 500 | Server xatosi |

## Rate limit

- Global: **120 so'rov / 1 daqiqa / IP**.
- Register: **5 / daqiqa**.
- Login: **10 / daqiqa**.
- Refresh / logout: **30 / daqiqa**.
- Email tasdiqlash kodi so'rash: **3 / daqiqa**; tasdiqlash: **10 / daqiqa**.
- Parolni tiklash so'rash: **3 / daqiqa**; reset: **5 / daqiqa**.
- AI submit (Writing/Speaking): **10 / daqiqa** + tarif kvotasi.
- Battle room yaratish: **10 / daqiqa**; qo'shilish: **30 / daqiqa**; ready/start/bot: **30–60 / daqiqa**; Battle javobi: **120 / daqiqa**.

## Xavfsizlik qoidalari

- Gemini, ElevenLabs va boshqa AI kalitlari faqat serverda.
- Gemini API kaliti URL'ga emas, `x-goog-api-key` headerga qo'yiladi.
- Mobil ilova hech qachon AI provayderga to'g'ridan-to'g'ri murojaat qilmaydi.
- Parollar Argon2id bilan hashlanadi.
- Refresh tokenlar ochiq holda saqlanmaydi.
- Email tasdiqlash va parol tiklash kodlari 6 xonali, HMAC bilan hashlanadi va 15 daqiqada tugaydi; 5 xato urinishdan keyin bekor qilinadi.
- Parol o'zgarganda barcha refresh tokenlar bekor qilinadi.
- `Idempotency-Key` javoblari foydalanuvchi bo'yicha ajratiladi, 24 soatdan keyin o'chiriladi va 4xx/5xx javoblar keshlanmaydi.
- Android'da access/refresh tokenlar Keystore AES-256-GCM kaliti bilan shifrlanadi; eski plaintext tokenlar birinchi o'qishda avtomatik shifrlanadi.
- AI bahosi hech qachon "rasmiy IELTS balli" deb ko'rsatilmaydi.
- `NODE_ENV=production` da `CORS_ORIGIN=*` va `AI_SCORING_MODE=mock` bilan server ishga tushmaydi.
