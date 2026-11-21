# 🔍 Loyiha To'liq Tahlili — Expert Xulosasi
## v5.0 Master-Reja → Tanqidiy Ko'rib Chiqish

> **Sana:** 2026-11-09  
> **Maqsad:** Loyihaning barcha jihatlarini expert sifatida baholab, zaif nuqtalarni tuzatish va dizayn spesifikatsiyasi tayyorlash

---

## 1. 📊 Umumiy Baho

| Soha | Baho | Izoh |
|------|------|-------|
| Bozor tahlili | ⭐⭐⭐⭐⭐ | Juda to'liq — raqamlar, benchmark, persona |
| Mahsulot strategiyasi | ⭐⭐⭐⭐⭐ | Nisha aniq, differensiator kuchli |
| Texnik arxitektura | ⭐⭐⭐⭐ | Yaxshi, lekin Battle WebSocket detallar kam |
| Kontent strategiyasi | ⭐⭐⭐⭐⭐ | IP xavfi yaxshi ko'rib chiqilgan |
| Moliyaviy model | ⭐⭐⭐⭐ | Realistik, lekin CAC hisoblanmagan |
| **Dizayn spesifikatsiyasi** | ⭐⭐ | **YO'Q — bu eng katta kamchilik** |
| Test strategiyasi | ⭐⭐⭐ | Qisman bor, lekin manual test plani yo'q |
| GTM | ⭐⭐⭐⭐ | Realistik, Telegram kuchli |

---

## 2. ✅ Kuchli Tomonlar

### 2.1 Nisha tanlash — A+
- "Noldan IELTS" emas, balki **"5.5 → 6.5"** — bu juda to'g'ri
- O'zbekiston kontekstida aniq muammo: Writing 5.81, Speaking 5.86
- Beachhead persona (Aziza) — konkret va real

### 2.2 Biznes model — A
- 12 900 so'm/oy — auditoriyaga mos (median ish haqi 4.17M)
- B2B (Teacher rejasi) — kuchli; battle demo = eng yaxshi sotuv argumenti
- Founder narxi (9 900) — aqlli sodiqlik strategiyasi

### 2.3 Kontent strategiyasi — A+
- IP xavfi juda yaxshi ko'rib chiqilgan (§6.2)
- 6-bosqichli pipeline — professional
- 12-tekshiruvli avtomatik validator — ishonchli
- Kill criteria — data-driven

### 2.4 Texnik stack — A
- Fastify > Express — to'g'ri tanlov (2x tezroq)
- Prisma — type-safe, migratsiya boshqaruv
- FSRS — spaced repetition uchun eng zamonaviy
- Server-authoritative battle — xavfsizlik uchun to'g'ri

---

## 3. ❌ Topilgan Kamchiliklar va Tuzatishlar

### 3.1 KRITIK: Dizayn spesifikatsiyasi yo'q
**Muammo:** Rejada "Dizayn — Frilanser, 2–3 hafta, $500–1000" deyilgan, lekin:
- Rang palitrasI yo'q
- Typography tizimi yo'q
- Ekran oqimlari (user flows) yo'q
- Komponent bibliotekasi yo'q
- Dizayn tizimi (design system) yo'q

**Natija:** Frilanser dizayner nima qilishini bilmaydi; developer nima qurishini bilmaydi.

**Tuzatish:** `DESIGN.md` yaratildi (keyingi fayl).

---

### 3.2 KRITIK: Test strategiyasi yetishmaydi
**Muammo:** Hujjatda testlash haqida juda kam:
- Backend unit/integration test qamrovi yo'q (maqsad: 80%)
- Android test piramidasi yo'q
- AI sifat testi faqat g2 gate'da (200 insho) — bu kech
- Battle WebSocket stress test yo'q
- Oflayn sinxronizatsiya testi yo'q

**Tuzatish:** `TEST.md` yaratildi (keyingi fayl).

---

### 3.3 MUHIM: Battle WebSocket protokoli tafsilsiz
**Muammo:** §4.4 da battle spesifikatsiyasi bor, lekin:
- WebSocket event nomlari aniqlanmagan
- Reconnect protokoli qanday? (15 soniya aytilgan, lekin texnik detallar yo'q)
- Bot service qanday ishlaydi? (AI? Rule-based? Latency simulyatsiya?)
- Observer mode (o'qituvchi ko'rish uchun) yo'q

---

### 3.4 MUHIM: CAC (Customer Acquisition Cost) hisoblanmagan
**Muammo:** §11 moliyaviy modelda CAC yo'q:
- Telegram Ads: $300 → 500 install → CAC = $0.60/install
- Influencer: 5 × ~$100 = $500 → 300 install → CAC = $1.67/install
- Blended CAC ≈ $0.90/install
- LTV (avg): 4 oy × $0.85 = $3.40
- LTV:CAC = 3.8:1 — qabul qilinishi mumkin, lekin chakkam

**Tavsiya:** Organik kanallarni kuchaytirib, CAC ni $0.50 ga tushirish.

---

### 3.5 O'RTA: Placement test UI oqimi noaniq
**Muammo:** §5.3 da placement qisqacha tasvirlangan:
- Foydalanuvchi 15 daqiqa test qilishga rozi bo'ladimi?
- Speaking (1 daqiqa) — micro mic permission, foydalanuvchi tayyor bo'lmasa?
- Natija ko'rsatish UI qanday?

**Tavsiya:** Placement 7–8 daqiqaga qisqartirilsin, speaking ixtiyoriy bo'lsin.

---

### 3.6 O'RTA: Narx sahifasi (Paywall) spesifikatsiyasi yo'q
**Muammo:** §4.2 da "Paywall (1 bepul baholashdan keyin)" deyilgan, lekin:
- Paywall qanday ko'rinadi?
- Trial tugaganda nima bo'ladi?
- Downgrade oqimi?

---

### 3.7 KICHIK: Argon2id parametrlari
§8.2 da Argon2id aytilgan — bu bcrypt dan yaxshiroq tanlov.
Lekin parametrlar yo'q. Prod uchun tavsiya: `m=65536, t=3, p=4`.

---

### 3.8 KICHIK: ZRU-1125 blokeri LOW prioritetda
§8.4 da "Huquqiy maslahat olinishi shart" — bu HIGH priority blocker bo'lishi kerak:
- Server O'zbekistonda bo'lishi kerakmi?
- Bu hosting xarajatiga ta'sir qiladi

---

## 4. 🧪 Test Tahlili va Minimal Test To'plami

### Backend (Node.js/Fastify) — Jest

```
Unit tests (target: 80%):
  ✓ Auth service: register, login, refresh, logout
  ✓ Writing evaluation: band calculation formula
  ✓ Battle: scoring formula (500 + 0..500 tezlik + 100 streak)
  ✓ FSRS: card scheduling algorithm
  ✓ Billing: state machine transitions

Integration tests (Supertest):
  ✓ POST /v1/auth/register — validation, duplicate email
  ✓ POST /v1/writing/submissions — AI mock
  ✓ WS /v1/battle/ws — room create, join, answer, results
  ✓ POST /v1/billing/verify — Play receipt mock

Load tests (k6):
  ✓ Battle: 10 concurrent clients, 20-question room
  ✓ AI endpoint: 50 req/min sustained
```

### Android (Kotlin) — JUnit 5 + MockK + Compose Testing

```
Unit tests (target: 70%):
  ✓ WritingViewModel: submit state, feedback parsing
  ✓ BattleViewModel: room state machine, scoring
  ✓ FSRS: card due_at calculation
  ✓ SpeechRecognizer wrapper

UI tests (Compose Testing):
  ✓ Onboarding screens completion
  ✓ Writing editor + submit button
  ✓ Battle lobby + question screen timer
  ✓ Vocabulary card swipe (FSRS)

E2E (manual):
  ✓ Full auth flow (register, verify, login)
  ✓ Writing submit → AI feedback view
  ✓ Battle: 2 real devices, 5 questions
  ✓ Offline: airplane mode → sync
```

---

## 5. 🎯 Ustuvor Harakatlar

| # | Harakat | Muhimlik | Muddat |
|---|---------|----------|--------|
| 1 | DESIGN.md yaratish (rang, font, ekranlar) | KRITIK | Hozir |
| 2 | TEST.md va CI pipeline | KRITIK | 1-hafta |
| 3 | Play merchant UZ tekshirish | BLOCKER | Darhol |
| 4 | 20 intervyu o'tkazish (G0 gate) | KRITIK | 2-hafta |
| 5 | Battle WS event protokoli aniqlash | MUHIM | 2-hafta |
| 6 | ZRU-1125 uchun huquqshunos | MUHIM | 2-hafta |
| 7 | CAC model qo'shish | O'RTA | 1-hafta |
| 8 | Paywall UI spesifikatsiyasi | O'RTA | 1-hafta |

---

*Tahlil yaratildi: 2026-11-09*

