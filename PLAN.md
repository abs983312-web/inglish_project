# Ingliz tili — IELTS 5.5 → 6.5 Android ilovasi
## Master-reja v5.0

> **Sana:** 2026-11-03  
> **Holat:** Amaldagi master-reja  
> **Asos:** v4 (bozor) + v4.1 (jahon benchmarklari) + v4.2 (mahsulot/narx/battle) + v4.3 (kontent)  
> **Platforma:** Android — Kotlin + Jetpack Compose  
> **Backend:** Node.js + Fastify + Prisma + PostgreSQL  
> **Til:** O'zbek (UI), Ingliz (o'rganish predmeti)

---

## Hujjat xaritasi

| Fayl | Mazmuni |
|------|---------|
| **PLAN.md** (shu fayl) | Yagona master-reja — barcha qarorlar |
| `C:\Users\pc\.opencode\plan\PLAN-v3.md` | Chuqur texnik ilova (API, DDL, AI, billing) |
| `C:\Users\pc\.opencode\plan\PLAN-v4.md` | Bozor tahlili va strategik asos |
| `C:\Users\pc\.opencode\plan\PLAN-v4.1-jahon-tajribasi.md` | Global case study va benchmarklar |
| `C:\Users\pc\.opencode\plan\PLAN-v4.2.md` | Narx, battle, launch |
| `C:\Users\pc\.opencode\plan\PLAN-v4.3-kontent.md` | Kontent ishlab chiqarish playbook |
| `C:\Users\pc\.opencode\plan\PLAN-v2-archive.md` | Eski v2 reja (arxiv) |

---

# 0. Bir sahifalik xulosa

## 0.1. Mahsulot nima

O'zbekistonlik IELTS topshiruvchilar uchun **AI murabbiy** — asosiy e'tibor
**Writing** va **Speaking** ga. Foydalanuvchi insho yozadi yoki ovozli javob
beradi, AI uni baholaydi, xatolarni tushuntiradi va qayta yozishga undaydi.
Qo'shimcha: lug'at, grammatika va **Kahoot uslubidagi Battle** (o'qituvchi
boshqaradigan jonli viktorina).

## 0.2. Kim uchun

| Segment | Tavsif | Ustuvorlik |
|---------|--------|------------|
| **Beachhead** | 5.5–6.0 ballga ega, 6.5 kerak bo'lgan 18–25 yosh | ⭐ Asosiy |
| Ikkinchi | 6.0–6.5, 7.0 ga intilayotgan | Keyingi |
| B2B | Til markazlari, o'qituvchilar | Parallel |

## 0.3. Nega hozir

- O'zbekistonda IELTS o'rtacha ball **6.07**; Writing **5.81**, Speaking **5.86** — eng zaif ikki ko'nikma
- 2024-yilda **~53 000** yosh 5.5+ ball olgan; talab o'smoqda
- Internet **33.1M** (89%), mobil **33.9M**, 4G qamrovi **97%**
- AI baholash endi arzon va yetarli darajada yaxshi
- Mahalliy raqobat asosan offline kurslar (400–800 ming so'm/oy)

## 0.4. Biznes model

| Element | Qaror |
|---------|-------|
| Model | Freemium + obuna |
| Launch narx | **12 900 so'm/oy** (cost-recovery) |
| Yillik | **99 000 so'm** |
| Founder | **9 900 so'm/oy** (birinchi 1 000, 12 oy) |
| Teacher | **99 000 so'm/oy** (30 o'yinchi) |
| Foyda | Keyinroq — hozir xarajatni qoplash |

## 0.5. v1 scope

✅ Writing (Task 1/2 + AI baho + rewrite)  
✅ Speaking (roleplay + STT/TTS + feedback)  
✅ Lug'at (500 so'z, FSRS)  
✅ Battle (friend + bot + classroom)  
✅ Progress + imtihon countdown  
✅ Obuna (Play Billing)  
❌ To'liq mock test, Listening/Reading, A1–C1 — v2

## 0.6. Timeline va byudjet

| Ko'rsatkich | Qiymat |
|-------------|--------|
| MVP | **21 hafta** + buffer = **24–28 hafta** |
| Byudjet | **$2 400–4 850** + kontent $190–385 |
| Launch | Beta 100 user → Play |

## 0.7. Asosiy risk

**Tarqatish.** Native app tanlandi — Telegram'ning friction nol afzalligi yo'q.
Birinchi 1 000 install rejasi kod yozishdan oldin tayyor bo'lishi shart.

---

# 1. Bozor

## 1.1. Makro kontekst

| Ko'rsatkich | Qiymat | Manba |
|-------------|--------|-------|
| Aholi | ~37M | — |
| Internet foydalanuvchilari | 33.1M (89%) | DataReportal |
| Mobil ulanishlar | 33.9M | GSMA |
| 4G qamrovi | 97% | — |
| O'rtacha ish haqi | 7.09M so'm (~$590) | H1 2026 |
| Median ish haqi | 4.17M so'm | H1 2026 |
| Minimal ish haqi | 1.36M so'm | Sent 2026 |

## 1.2. IELTS haqiqatlari (2024–2025, Academic, O'zbekiston)

| Ko'nikma | O'rtacha ball |
|----------|---------------|
| Overall | **6.07** |
| Listening | 5.99 |
| Reading | 6.37 |
| **Writing** | **5.81** |
| **Speaking** | **5.86** |

- **41%** 6.5+ oladi
- **23%** 7.0+ oladi
- Ya'ni **59%** 6.5 ga yetmaydi — asosiy bozor
- Writing va Speaking — eng past, eng qiyin, eng ko'p repetitor talab qiladigan qism

## 1.3. Muammo diagnostikasi

| Muammo | Hozirgi yechim | Narx |
|--------|----------------|------|
| Writing uchun feedback yo'q | Repetitor | 100–200 ming/soat |
| Speaking practice yo'q | Til markazi | 400–800 ming/oy |
| Xatolar tushuntirilmaydi | Kitob | — |
| Vaqt yo'q | Offline kurs | 3–6 oy |
| Qimmat | — | Oylik daromadning 10–20% |

## 1.4. Raqobat landshafti

| Raqobat | Kuchli | Zaif |
|---------|--------|------|
| Offline kurslar | Ishonch, jonli muloqot | Qimmat, vaqt, joy |
| Repetitorlar | Shaxsiy | Qimmat, nomuvofiq sifat |
| Duolingo | Bepul, gamifikatsiya | IELTS ga mos emas, writing feedback yo'q |
| ELSA | Talaffuz, arzon | Writing yo'q, IELTS to'liq emas |
| YouTube/Telegram | Bepul | Tizimsiz, feedback yo'q |
| Mahalliy ilovalar | Arzon | Sifatsiz, kontent kam |

## 1.5. Bo'shliq (white space)

> **5.5 → 6.5 uchun Writing + Speaking feedback.**  
> Bepul alternativada feedback yo'q; qimmat alternativada narx va vaqt to'sqinlik qiladi.

## 1.6. Bozor hajmi (bottom-up)

| Ko'rsatkich | Qiymat |
|-------------|--------|
| Yillik IELTS topshiruvchilar (taxmin) | ~100 000 |
| 5.5–6.5 segmenti | ~50 000 |
| To'lovga tayyor (10–15%) | 5 000–7 500 |
| ARPU/yil | $10–15 |
| **B2C TAM (UZ)** | **~$270k/yil** |

> **Xulosa:** O'zbekiston yolg'iz katta biznes emas. B2B + regional (KZ/KG/TJ) —
> o'sish yo'li. Lekin beachhead sifatida yetarli.

## 1.7. To'lov realligi — kritik

| Omil | Holat |
|------|-------|
| Google Play paid/IAP | Qo'llab-quvvatlanadi (taxmin) |
| Play merchant/payout UZ | **Tekshirilishi shart** |
| PayPal | ❌ Mavjud emas |
| Click/Payme | ✅ Mahalliy, lekin Play billing bilan integratsiya cheklangan |
| Karta | ✅ Uzcard/Humo |

> **Blocker:** Play Console'da merchant va payout tasdiqlanmasa, butun narx modeli
> o'zgaradi (web to'lov yoki alternativ kanal).

---

# 2. Foydalanuvchi

## 2.1. Beachhead persona

**Ism:** Aziza, 21 yosh  
**Holat:** Universitet 3-kurs, magistraturaga xorijda o'qishni xohlaydi  
**Ball:** 5.5 (Writing 5.5, Speaking 5.5)  
**Maqsad:** 6.5, imtihon 3 oydan keyin  
**Byudjet:** 200–300 ming so'm/oy  
**Og'riq:** Repetitor qimmat; writing'ga kimdir qarashi kerak; gapirishga uyaladi  
**Xatti-harakat:** Telegram, Instagram, kechqurun 21:00–23:00

## 2.2. JTBD

> "Imtihonga tayyorlanayotganimda, writing va speaking'imni kimdir baholab,
> aniq nima xato qilayotganimni aytsin — men 6.5 olsam."

## 2.3. Value proposition

| Biz | Ular |
|-----|------|
| 24/7 AI feedback | Repetitor faqat dars vaqtida |
| Aniq band estimate | "Yaxshi" degan noaniq baho |
| Xatoni tushuntirish | Faqat natija |
| 12 900 so'm/oy | 400–800 ming/oy |
| Uyda, uyalmasdan | Guruhda gapirish stressi |

## 2.4. Positioning

```
                    Qimmat
                       │
        Offline kurs   │   Repetitor
                       │
    ───────────────────┼───────────────────
    Tizimsiz           │            Tizimli
    YouTube/Telegram   │   ★ BIZ
                       │   Duolingo (bepul)
                    Arzon
```

---

# 3. Strategiya

## 3.1. Pozitsiya

> **"IELTS 6.5 uchun AI murabbiy — writing va speaking'ni baholaydi, xatoni tushuntiradi."**

- "Noldan IELTS" emas
- "1000 ta so'z" emas
- "Imtihon formatiga mos" — asosiy

## 3.2. Uch differensiator

1. **Writing feedback** — 4 mezon bo'yicha, o'zbekcha izoh bilan
2. **Speaking roleplay** — real imtihon stsenariysi, talaffuz va fluency
3. **Battle** — o'qituvchi boshqaradigan jonli viktorina (B2B sotuv nuqtasi)

## 3.3. Brend pozitsiyasi

- **"IELTS" brend sifatida ishlatilmaydi** — faqat tavsifiy ("IELTS preparation")
- Rasmiy aloqa yo'qligi haqida disclaimer
- Kelajakda CEFR/DET/TORFL/TOPIK ga kengayish uchun "exam prep" pozitsiyasi

## 3.4. Narx strategiyasi

| Bosqich | Narx | Maqsad |
|---------|------|--------|
| Launch (0–6 oy) | 12 900 so'm/oy | Hajm, sharh, retention |
| O'sish (6–12 oy) | 19 900 so'm/oy | Marja |
| Barqaror (12+ oy) | 29 000–39 000 so'm/oy | Foyda |
| Founderlar | 9 900 so'm, 12 oy | Sodiqlik |

> Narx oshirilganda **eski foydalanuvchilar 12 oy eski narxda qoladi** — oldindan aytiladi.

## 3.5. GTM

| Kanal | Rol |
|-------|-----|
| ASO | "IELTS writing", "speaking practice" |
| Telegram | Kontent + Ads + bot |
| Mikro-influencer | IELTS o'qituvchilari |
| Til markazlari | B2B + promokod |
| Referal | 1 oy bepul |

## 3.6. B2B

- **Teacher rejasi:** 99 000 so'm/oy, 30 o'yinchi, dashboard
- **Pilot:** 3 markaz, 3-oydan
- **Sotuv:** battle'ni sinfda ko'rsatish — eng kuchli demo

---

# 4. Mahsulot

## 4.1. v1 scope

### Kiradi

| Modul | Tarkib |
|-------|--------|
| Auth | Email + Google, JWT |
| Placement | 4 ko'nikma, 10–15 daqiqa |
| **Writing** | Task 1/2, AI baho, 4 mezon, rewrite loop |
| **Speaking** | Part 1/2/3 roleplay, STT/TTS, feedback |
| **Lug'at** | 500 so'z, FSRS, kontekst |
| **Battle** | Friend Room, Bot, Classroom |
| Progress | Band trend, imtihon countdown |
| Obuna | Play Billing, trial |
| Push | Streak, battle taklifi, eslatma |

### Kirmaydi (v2)

- To'liq IELTS mock (4 modul)
- Listening/Reading modullari
- A1–C1 to'liq kurs
- Public random matchmaking
- Teacher dashboard (v1.5)
- Leaderboard (v1.5)
- Boshqa tillar

## 4.2. Asosiy foydalanuvchi oqimi

```
Install → Onboarding (maqsad, imtihon sanasi, daraja)
   ↓
Placement (qisqa test)
   ↓
Home: "Imtihongacha 47 kun" + bugungi vazifa
   ↓
Writing / Speaking / Lug'at / Battle
   ↓
Feedback + band trend
   ↓
Paywall (1 bepul baholashdan keyin)
```

## 4.3. Retention loop

**Asosiy mexanizm — imtihon sanasi countdown**, cheksiz streak emas:

```
Imtihongacha 47 kun
Bugun: 1 writing + 10 so'z + 5 daq speaking
Progress: 6.0 → 6.5 yo'lida 38%
```

- Har kuni **bitta aniq vazifa**
- Streak — qo'shimcha, asosiy emas
- Haftalik mock — v2
- Push: 19:00–20:00

## 4.4. Battle — Kahoot uslubi

### Nega host-driven

Random matchmaking likvidlik talab qiladi (1 000 DAU da navbat quriydi).
Kahoot modelida **xost o'yinchilarni o'zi olib keladi** — muammo yo'q.

### Spetsifikatsiya

| Parametr | Qiymat |
|----------|--------|
| Xona kodi | 6 raqam + deep link |
| O'yinchilar | 2–10 (bepul), 30 (Teacher) |
| Savollar | 10 / 15 / 20 |
| Vaqt/savol | 10 / 20 / 30 soniya |
| Mavzular | Lug'at, grammatika, kollokatsiya, listening |
| Chat | ❌ Yo'q (v1) |
| Bot to'ldirish | 15 soniyadan keyin |
| Reconnect | 15 soniya |

### Ballash

```
To'g'ri:        500
Tezlik bonusi:  +0…500 (capped)
Streak (3+):    +100
Noto'g'ri:      0
```

### Rejimlar

1. **Friend Room** — B2C
2. **Classroom** — B2B (asosiy foyda)
3. **Weekly Tournament** — yakshanba 19:00
4. **Bot Battle** — onboarding / yolg'iz

### Texnik

```
Android ──WebSocket──► Node.js Battle Service
                         ├── Redis (room state, pub/sub)
                         ├── PostgreSQL (savollar, natijalar)
                         └── Bot service
```

- Server-authoritative
- Javob kaliti clientga chiqmaydi
- Vaqt serverda
- Alternative: Supabase Realtime (2–3 hafta tejaydi)

### Metrikalar

| Metrika | Maqsad |
|---------|--------|
| Room completion | ≥ 70% |
| O'rtacha o'yinchi | ≥ 3 |
| Rematch | ≥ 15% |
| D7 lift | ≥ +3 pp |
| Teacher room/oy | ≥ 20 |

**Kill:** 4 haftada completion < 50% yoki D7 lift < 1 pp → beta'dan chiqarish.

## 4.5. AI feedback

| Element | Qaror |
|---------|-------|
| Model | Gemini (server-side) |
| Writing baho | 4 mezon + overall + confidence |
| Speaking | Transkript + fluency + pronunciation + band |
| Izoh tili | O'zbek |
| "Official" da'vosi | ❌ Hech qachon |
| Ishonch oralig'i | ✅ "6.0 ± 0.5" |
| Kalibrlash | 200+ insho, inson bilan |
| Inson spot-check | 5% |

**Muhim:** AI–inson korrelyatsiyasi ~0.78. "Aniq ball" emas, **"practice estimate"**.

## 4.6. Audio

| Tur | Yechim |
|-----|--------|
| Statik dars audio | Oldindan yozilgan MP3, CDN |
| Dinamik matn (bepul) | Android TTS |
| Dinamik matn (PRO) | ElevenLabs, server-side, keshlangan |
| Speaking input | Android SpeechRecognizer |

Barcha API kalitlari **serverda**.

## 4.7. Offline

- Lug'at kartalari — Room'da
- Yozilgan insho — navbatda, keyin sync
- Audio — oldindan yuklab olish (PRO)
- Battle — offline **yo'q**

## 4.8. Accessibility

- TalkBack
- 48dp touch target
- Kontrast 4.5:1
- Reduced motion
- Shrift o'lchami 200%

## 4.9. Paywall UI va Monetizatsiya Oqimi

### Bepul (Freemium) vs PRO Imkoniyatlari:
| Imkoniyat | Bepul Foydalanuvchi | PRO Obunachi (59,000 so'm/oy) |
|-----------|---------------------|-------------------------------|
| Writing AI tekshiruv | Kuniga 1 ta (umumiy baho) | Cheksiz + chuqur grammatik/leksik xato tahlili |
| Speaking AI mashq | Kuniga 1 ta (45 soniya) | Cheksiz + talaffuz, fonema va ravonlik tahlili |
| Battle rejimi | Kuniga 3 ta o'yin | Cheksiz o'yinlar + barcha mavzuviy Packlar |
| Lug'at (FSRS) | 10 ta yangi so'z/kun | Cheksiz kartochkalar + Shaxsiy xatolar lug'ati |
| Band Score prognozi | Faqat umumiy CEFR | Har bir ko'nikma bo'yicha batafsil 0.5 aniqlikdagi grafik |
| Offline darslar | Yo'q | Mavjud (audio va lug'atlar) |

### Paywall Ko'rsatilish Nuqtalari (Conversion Triggers):
1. **Trigger A (Soft limit yetganda):** Kunlik bepul 1 ta Writing yoki Speaking topshirilgandan so'ng ikkinchi marta urinish qilinganda.
2. **Trigger B ("Aha!" lahzasi — Placement test yakuni):** O'quvchi darajasini (masalan, Band 5.5) ko'rgach: *"Band 6.5 ga 60 kunda chiqish uchun PRO tayyorgarlik rejangizni oching"* banneri.
3. **Trigger C (Streak yutug'i — 7 kunlik faollik):** 7 kun to'xtovsiz o'qigan sodiq o'quvchiga 3 kunlik "PRO Trial" yoki 20% lik maxsus vaucher taklif qilish.

### Mahalliy To'lov Usullari (O'zbekiston):
- **Google Play Billing:** Xalqaro kartalar va Play Store balansi uchun.
- **Payme & Click (Uzcard / Humo):** O'zbekiston ichidagi eng qulay to'lov. Deep link orqali Payme/Click ilovasiga o'tish yoki to'g'ridan-to'g'ri billing API orqali amalga oshiriladi.
- **Narxlar (UZS):**
  - 1 oylik obuna: **59,000 so'm**
  - 3 oylik intensiv: **149,000 so'm** (~49,000 so'm/oy)
  - Yillik to'liq tayyorgarlik: **449,000 so'm** (~37,000 so'm/oy, eng tejamkor)

---

# 5. O'quv dizayni

## 5.1. Metodik asos

- **Communicative + task-based** — writing/speaking markazda
- **Micro-learning** — 10–25 daqiqa
- **Spaced repetition** — FSRS
- **Feedback loop** — yoz → baho → tuzat → qayta yoz
- **Exam alignment** — IELTS band descriptors

## 5.2. CEFR mapping

| CEFR | IELTS | Rol |
|------|-------|-----|
| B1 | 4.5–5.5 | Zaif foydalanuvchi (takrorlash) |
| **B2** | **5.5–6.5** | **Asosiy** |
| C1 | 6.5–7.5 | Yuqori maqsad |

## 5.3. Placement (Quick Level Check — 7–8 daqiqa)

Onboarding konversiyasini saqlash uchun 15 daqiqalik og'ir testdan **7–8 daqiqalik adaptiv Quick Level Check** tizimiga o'tildi (drop-off xavfini 65% dan <20% ga tushiradi):

- **Davomiyligi:** 7–8 daqiqa (maksimal 10 daqiqa)
- **Struktura:**
  1. **Lug'at va Grammatika (12 ta adaptiv savol, ~3 daqiqa):** 
     - 4 ta A2/B1 daraja savollari
     - Agar to'g'ri bo'lsa → 4 ta B2 savollari; noto'g'ri bo'lsa → A2 qaytariladi
     - 4 ta C1 darajadagi farqlovchi savollar
  2. **Mini Reading (1 ta qisqa matn, 120 so'z, 3 ta savol, ~2.5 daqiqa):**
     - Skimming & Scanning tezligini va inference (xulosa chiqarish) qobiliyatini tekshiradi
  3. **Writing (1 ta qisqa prompt, 50–75 so'z, ~2 daqiqa):**
     - Fikr bayon qilish, grammatik xatoliklar va bog'lovchilarni AI orqali tezkor skanerlash
  4. **Speaking (Ixtiyoriy / "Skip for now" tugmasi bilan, ~1 daqiqa):**
     - Shovqinli muhitda yoki transportda bo'lgan o'quvchilar uchun majburiy emas
     - Agar topshirilsa: 45 soniyalik audio yozuv (talaffuz va ravonlik tahlili)
- **Natija taqdimoti:**
  - CEFR darajasi (masalan, B1+)
  - Prognoz qilinayotgan IELTS Band Score (masalan: 5.0 – 5.5)
  - 4 ta ko'nikma bo'yicha kuchli va zaif tomonlar radari
  - Shaxsiy o'quv yo'l xaritasi (Roadmap to 6.5+) avtomatik shakllantiriladi

## 5.4. Dars anatomiyasi

```
1. Maqsad (30 soniya)
2. Tushuntirish (2–3 daqiqa, o'zbekcha)
3. Misollar (1–2 daqiqa)
4. Mashq (5–8 daqiqa, 10–15 savol)
5. Xato tahlili (1–2 daqiqa)
6. Mini-test (2 daqiqa)
7. Takrorlash kartasi (1 daqiqa)
```

## 5.5. FSRS

- Har so'z uchun `stability`, `difficulty`, `due_at`
- Kunlik limit: 10 yangi + 30 takrorlash
- 90% retention target
- Offline ishlaydi

## 5.6. O'zbek L1 interference

| Muammo | Sabab | Yechim |
|--------|-------|--------|
| Artikllar | O'zbek tilida yo'q | Alohida modul + drill |
| Prepositionlar | Tizimsiz | Kontekstli mashq |
| So'z tartibi | SOV vs SVO | Tuzatish mashqi |
| Present Perfect | Analog yo'q | Vaqt belgilari |
| Talaffuz: th, w/v | Fonemalar yo'q | Minimal pairs |
| Stress | Erkin | Audio + repeat |

## 5.7. Baholash validligi

| Tekshiruv | Usul |
|-----------|------|
| Writing MAE | ≤ 0.5 (inson bilan) |
| Writing korrelyatsiya | ≥ 0.75 |
| Speaking MAE | ≤ 0.75 |
| Score compression | Yuqori baholarni pasaytirmaslik |
| Bias | Mezon bo'yicha alohida tekshirish |
| Confidence | Har bahoda interval |

## 5.8. Kontent sifat standarti

- Har unit: izoh + 15 mashq + 1 writing/speaking topshiriq
- O'zbekcha izoh — tabiiy, tarjima emas
- Javob kaliti — 100% tekshirilgan
- Kitobdan ko'chirilmagan
- 5 talaba piloti: tushunarlilik ≥ 4/5

---

# 6. Kontent strategiyasi

## 6.1. Oltin qoida

> **Kitobdan faqat "nima o'qitish kerak" ro'yxatini olamiz. "Qanday o'qitish"
> matni, mashqlar va model javoblarni AI yozadi, o'qituvchi tasdiqlaydi.**

Bu qoida **barcha** reference kitoblarga bir xil qo'llanadi.

## 6.2. Huquqiy chegara

| ✅ Olish mumkin | ❌ Olish mumkin emas |
|---|---|
| Unit nomi va tartibi | Unit matni |
| Grammatik mavzu | Tushuntirish matni |
| CEFR daraja | Mashqlar |
| Imtihon skill turi | Javob kaliti |
| Umumiy metodik yondashuv | Model javoblar |
| Mavzu ro'yxati | Audio, rasm, scan, PDF |
| — | Kitob brendi marketingda |

**Trademark:** "Destination", "Collins", "Cambridge" — ilova nomida, Play listing'da,
reklamada **ishlatilmaydi**. Faqat ichki `source_ref` da.

## 6.3. Kitoblar reyestri

| Kitob | Olamiz | Olmaymiz |
|-------|--------|----------|
| **Destination B1/B2/C1&C2** | Unit tartibi, grammar topic, CEFR | Izoh, mashq, javob kaliti, audio |
| **Cambridge Vocabulary for IELTS** | Mavzu ro'yxati, so'z kategoriyalari | Ta'rif, mashq, test |
| **Complete IELTS 5–6.5** | Skill ketma-ketligi, task turi | Matn, topshiriq, model, audio |
| **Collins Writing for IELTS** | Strategiya sarlavhalari | Model javob, mashq |
| **Collins Speaking for IELTS** | Part 1/2/3 strategiyasi | Savol, model, audio |
| **Improve Your IELTS Writing/Speaking** | Mavzu xaritasi | Mashq, matn |
| **IELTS Advantage** | Skill checklist | Matn, topshiriq |
| **English Pronunciation in Use** | Fonema ro'yxati, minimal pairs | Drill matni, audio |
| **AWL (Coxhead)** | 570 so'z | — (public, atributsiya) |
| **IELTS band descriptors** | Rubrika mezonlari | — (public, shartlarni tekshirish) |
| **Common Mistakes in English** | Xato kategoriyalari | Misol, mashq |
| **Practical English Usage** | O'qituvchi reference | Hech narsa ko'chirilmaydi |

## 6.4. Pipeline — 6 bosqich

```
1. Unit map (JSON)
   ↓
2. AI generatsiya (structured JSON, inglizcha avval)
   ↓
3. Avtomatik validator
   ↓
4. O'qituvchi review (15–20 daq/unit)
   ↓
5. 5 talaba piloti
   ↓
6. Publish (version++)
```

**Qoida:** har bosqichdan o'tmasa — keyingisiga o'tilmaydi.

## 6.5. Unit map sxemasi

```json
{
  "unit_id": "b2-u07",
  "source_ref": "Destination B2, Unit 7",
  "cefr": "B2",
  "grammar_topic": "Present Perfect vs Past Simple",
  "vocab_topic": "Work and career",
  "exam_skill": "writing_task2",
  "target_band": "6.0-6.5",
  "uz_focus": "O'zbek tilida bu ikki zamon farqlanmaydi",
  "prerequisites": ["b2-u05"],
  "estimated_minutes": 25
}
```

> `source_ref` faqat ichki. API'da, UI'da, marketingda ko'rinmaydi.

## 6.6. Lesson sxemasi

```json
{
  "unit_id": "b2-u07",
  "title_uz": "Present Perfect va Past Simple",
  "explanation_en": "...",
  "explanation_uz": "...",
  "uz_interference": "...",
  "examples": [{"en": "...", "uz": "...", "note": "..."}],
  "exercises": [
    {
      "id": "ex-01",
      "type": "multiple_choice",
      "question": "...",
      "options": ["A", "B", "C", "D"],
      "answer_index": 1,
      "explanation_uz": "...",
      "cefr": "B2"
    }
  ],
  "writing_task": {
    "prompt": "...",
    "type": "opinion",
    "min_words": 250,
    "rubric_id": "ielts_task2_public"
  },
  "meta": {
    "ai_model": "gemini-...",
    "prompt_version": "v1.0",
    "generated_at": "2026-11-03"
  }
}
```

## 6.7. Prompt shablonlari

### Dars izohi

```
You are an experienced IELTS teacher and curriculum designer for Uzbek learners.
Target: CEFR {cefr}, IELTS band {target_band}.
Topic: {grammar_topic}
L1 interference to address: {uz_focus}

Write:
1. A clear English explanation (max 200 words).
2. A natural Uzbek translation (not literal).
3. 5 example sentences: English + Uzbek + a short note.
4. 3 common mistakes Uzbek speakers make, with corrections.

Rules:
- Do NOT copy any textbook text.
- Use B2-level vocabulary.
- Be precise; no invented grammar rules.
- Return JSON only.
```

### Mashq generatsiyasi

```
Generate {n} multiple-choice questions for {grammar_topic} at CEFR {cefr}.

Rules:
- Exactly one correct answer.
- 4 options; distractors plausible but clearly wrong.
- No "all of the above" / "none of the above".
- No duplicate questions.
- Each item: question, options, answer index, short Uzbek explanation.
- Cover different sub-skills.

Return JSON only.
```

### Validator (ikkinchi AI)

```
You are a QA reviewer. For each item, check:
1. Exactly one correct answer?
2. Any second defensible answer?
3. Distractors plausible but wrong?
4. CEFR level correct?
5. Ambiguous wording?
6. Duplicate of another item?

Return: [{id, valid, reasons}]. Do not rewrite; only judge.
```

### Writing model answer

```
Write an IELTS Task 2 essay at band {band} for this prompt: {prompt}

Follow the public IELTS band descriptors.
Then self-assess each criterion with evidence.
Do not claim this is an official score. Label it "practice estimate".
Return JSON: {essay, self_assessment, estimated_band, confidence_range}.
```

## 6.8. Avtomatik validator — 12 tekshiruv

| # | Tekshiruv | Muvaffaqiyatsizlik |
|---|-----------|--------------------|
| 1 | JSON schema | Rad |
| 2 | Javob options ichida | Rad |
| 3 | Bitta to'g'ri javob | Rad |
| 4 | Duplicate | Rad |
| 5 | CEFR readability | Ogohlantirish |
| 6 | So'kinish/siyosat | Rad |
| 7 | O'zbek tarjimasi bor | Rad |
| 8 | Uzunlik limiti | Rad |
| 9 | Plagiarism (7-gram) | Rad (>5%) |
| 10 | Band alignment | Ogohlantirish |
| 11 | Audio matn mosligi | Rad |
| 12 | Raqam/sana izchilligi | Ogohlantirish |

## 6.9. O'qituvchi review checklist

- [ ] Grammatika izohi to'g'ri
- [ ] O'zbek tili tabiiy
- [ ] Javob kaliti to'g'ri
- [ ] Distraktorlar mantiqiy
- [ ] CEFR darajasi mos
- [ ] L1 interference izohi to'g'ri
- [ ] Madaniy mos
- [ ] Kitobdan ko'chirilgan matn yo'q
- [ ] Audio matnga mos
- [ ] 5 talaba piloti ≥ 4/5

## 6.10. Eval to'plami — 50 item

| Tur | Soni | Gate |
|-----|------|------|
| Grammatika MCQ | 20 | Answer key ≥ 98% |
| Lug'at MCQ | 15 | Bitta javob ≥ 95% |
| Writing prompt + model | 10 | MAE ≤ 0.5 |
| Speaking roleplay | 5 | Format mosligi |

**Qo'shimcha gate'lar:**
- O'zbek tili tabiiyligi ≥ 4.0/5
- CEFR mosligi ≥ 90%
- Plagiarism < 5%

> Eval o'tmasa — 15 unitdan keyin scale qilinmaydi.

## 6.11. Xarajat va vaqt

### 15 unit (launch)

| Modda | Summa |
|-------|-------|
| AI generatsiya | $30–60 |
| O'qituvchi review | $110–225 |
| Pilot | $50–100 |
| **Jami** | **$190–385** |
| **Vaqt** | **2–3 hafta** |

### 60 unit (keyingi)

| Modda | Summa |
|-------|-------|
| AI generatsiya | $120–240 |
| O'qituvchi review | $450–630 |
| **Jami** | **$570–870** |
| **Vaqt** | **6–8 hafta** |

## 6.12. Kontent DB maydonlari

```sql
content_units (
  id, source_ref, unit_no, cefr, grammar_topic, vocab_topic,
  exam_skill, target_band, uz_focus, status
)

content_items (
  id, unit_id, type, skill, cefr,
  payload_json, answer_key, explanation_uz,
  source_ref, license,              -- original | public_attribution | licensed
  ai_model, prompt_version,
  review_status, reviewer_id, reviewed_at,
  version, published_at
)

content_reviews (
  id, item_id, reviewer_id, verdict, notes, created_at
)
```

**Constraint:** `review_status = 'published'` faqat `reviewer_id IS NOT NULL` bo'lsa.

## 6.13. Kontent gate'lari

| Gate | Shart | Harakat |
|------|-------|---------|
| G-C1 | Javob kaliti xatosi > 2% | Scale to'xtaydi |
| G-C2 | O'qituvchi rad etishi > 30% | Prompt qayta |
| G-C3 | Tushunarlilik < 70% | Unit qayta yoziladi |
| G-C4 | Writing MAE > 0.75 | Kalibrlash qayta |
| G-C5 | Plagiarism topilsa | Unit o'chiriladi |

## 6.14. Nima qilmaslik

| # | Xato | Natija |
|---|------|--------|
| 1 | PDF/scan joylash | Huquqbuzarlik |
| 2 | Mashq/javob ko'chirish | Huquqbuzarlik |
| 3 | Kitob nomini marketingda ishlatish | Trademark |
| 4 | Review'siz nashr | Ishonch yo'qolishi |
| 5 | Faqat AI, validatorsiz | Xato javoblar |
| 6 | "Official IELTS" da'vosi | Huquqiy risk |
| 7 | 15 unitdan oldin scale | Sifatsiz kontent |
| 8 | O'zbek izohini AI'ga to'liq qoldirish | Sun'iy til |
| 9 | Audio'ni keyin qo'shish | Sync muammosi |
| 10 | `source_ref` ni API'da ochish | Huquqiy iz |

---

# 7. Texnik arxitektura

## 7.1. Stack

| Qatlam | Texnologiya |
|--------|-------------|
| Android | Kotlin, Jetpack Compose, MVVM/MVI, Hilt, Room, Retrofit/Ktor, WorkManager, DataStore |
| Backend | Node.js 22, Fastify, Prisma, PostgreSQL 16, Redis, BullMQ |
| AI | Gemini API (server-side) |
| Audio | S3-compatible + CDN, Android TTS, ElevenLabs (PRO) |
| Push | FCM |
| Analytics | Firebase + o'z eventlari |
| CI/CD | GitHub Actions, Docker |
| Monitoring | Sentry, Grafana/Uptime |

## 7.2. Android arxitekturasi

```
app/
├── core/          (network, db, di, ui, analytics)
├── feature/
│   ├── auth/
│   ├── onboarding/
│   ├── placement/
│   ├── writing/
│   ├── speaking/
│   ├── vocab/
│   ├── battle/
│   ├── progress/
│   └── billing/
└── navigation/
```

- **Offline-first:** Room + sync queue
- **Compose navigation**
- **StateFlow + sealed UI state**
- **Idempotency:** har yozuvda `client_id`

## 7.3. Backend modullari

```
api/
├── auth
├── users
├── content
├── attempts
├── writing
├── speaking
├── vocab
├── battle
├── billing
├── notifications
└── admin
```

## 7.4. Data model (asosiy)

```sql
users (
  id uuid pk, email citext unique, password_hash,
  role text, created_at, deleted_at
)

profiles (
  user_id uuid pk references users,
  display_name, ui_lang, source_lang, target_lang,
  cefr_level, target_band, exam_date date,
  timezone, daily_goal_min, onboarding_done boolean
)

language_pairs (
  id uuid pk, source_lang, target_lang, is_active
)

courses (
  id uuid pk, pair_id, level, title, exam_target
)

units (
  id uuid pk, course_id, seq, title_uz, cefr, skill
)

lessons (
  id uuid pk, unit_id, type, payload_jsonb
)

content_items (
  id uuid pk, lesson_id, type, skill, cefr,
  payload_jsonb, answer_key_jsonb,
  source_ref, license, review_status,
  ai_model, prompt_version, version
)

attempts (
  id uuid pk, user_id, item_id,
  answer_jsonb, is_correct, score,
  idempotency_key unique, created_at
)

writing_submissions (
  id uuid pk, user_id, prompt_id, text, word_count,
  band_estimate numeric(2,1), confidence numeric(2,1),
  feedback_jsonb, status, created_at
)

speaking_sessions (
  id uuid pk, user_id, scenario_id,
  transcript_jsonb, band_estimate, confidence,
  feedback_jsonb, created_at
)

vocab_cards (
  id uuid pk, user_id, word, translation_uz,
  fsrs_state_jsonb, due_at, reps, lapses
)

subscriptions (
  id uuid pk, user_id, plan, status,
  store, purchase_token, expires_at,
  trial_ends_at, created_at
)

payments (
  id uuid pk, user_id, store, order_id,
  purchase_token unique, amount, currency,
  status, raw_jsonb, created_at
)

battle_rooms (
  id uuid pk, host_id, code unique,
  status, settings_jsonb, started_at, ended_at
)

battle_participants (
  id uuid pk, room_id, user_id, score, rank
)

battle_answers (
  id uuid pk, room_id, user_id, question_id,
  answer_index, is_correct, ms, created_at
)

xp_ledger (
  id uuid pk, user_id, amount, reason,
  ref_id, idempotency_key unique, created_at
)

streaks (
  user_id uuid pk, current int, longest int,
  last_active_date date, freezes int
)

devices (
  id uuid pk, user_id, fcm_token, platform, last_seen
)

audit_log (
  id uuid pk, actor_id, action, entity,
  entity_id, meta_jsonb, created_at
)
```

## 7.5. API (asosiy endpointlar)

| Metod | Yo'l | Vazifa |
|-------|------|--------|
| POST | `/v1/auth/register` | Ro'yxat |
| POST | `/v1/auth/login` | Kirish |
| POST | `/v1/auth/refresh` | Token yangilash |
| GET | `/v1/me` | Profil |
| PUT | `/v1/me/profile` | Profil yangilash |
| GET | `/v1/courses` | Kurslar |
| GET | `/v1/units/:id` | Unit |
| GET | `/v1/lessons/:id` | Dars |
| POST | `/v1/attempts` | Javob (idempotent) |
| POST | `/v1/writing/submissions` | Insho yuborish |
| GET | `/v1/writing/submissions/:id` | Baho |
| POST | `/v1/speaking/sessions` | Speaking boshlash |
| POST | `/v1/speaking/sessions/:id/turns` | Javob |
| GET | `/v1/vocab/due` | Takrorlash |
| POST | `/v1/vocab/:id/review` | Baholash |
| GET | `/v1/progress` | Statistika |
| POST | `/v1/battle/rooms` | Xona yaratish |
| POST | `/v1/battle/rooms/join` | Qo'shilish |
| WS | `/v1/battle/ws` | Real-time |
| GET | `/v1/subscription` | Obuna |
| POST | `/v1/billing/verify` | To'lovni tekshirish |
| POST | `/v1/webhooks/play` | RTDN |
| POST | `/v1/devices` | FCM token |

**Qoidalar:**
- JWT access 15 daqiqa, refresh 30 kun (rotatsiya)
- `Idempotency-Key` — barcha POST yozuvlarda
- Rate limit: 60 req/min/user
- Xatolar: `{code, message, details}`

## 7.6. Sync protokoli

- Har yozuvda `change_seq` (bigint, monotonik)
- Client: `GET /v1/sync?since=12345`
- Server: o'zgargan yozuvlar + yangi `cursor`
- Conflict: server wins, lekin client `client_updated_at` saqlanadi
- XP: idempotent (`idempotency_key`)
- Offline queue: WorkManager

## 7.7. AI quyi tizimi

```
Client → API → AI Service → Gemini
                    ↓
              cache (Redis)
                    ↓
              cost guard
```

- Barcha kalitlar serverda
- Prompt versiyalash
- JSON schema validation
- Retry (3, exponential)
- Fallback: oddiy baholash
- Token limitlari
- PII filter
- Log: `ai_model`, `prompt_version`, `tokens`, `latency`

## 7.8. Billing

- Google Play Billing 7
- Server-side verification
- RTDN webhook
- `purchaseToken` dedupe
- State machine: `pending → active → grace → hold → canceled/expired`
- Refund: 48 soat ichida, avtomatik
- Trial: 14 kun (test)

## 7.9. Push

- FCM
- Kanallar: `daily`, `battle`, `billing`
- Quiet hours: 22:00–08:00
- Deep link: `app://lesson/123`

## 7.10. Admin

- Kontent CRUD
- Review queue
- Foydalanuvchi qidirish
- Obuna/refund
- AI loglar
- Feature flags

---

# 8. Xavfsizlik va huquq

## 8.1. Threat model

| Tahdid | Yechim |
|--------|--------|
| Token o'g'irlash | Qisqa access, rotating refresh, device binding |
| AI prompt injection | Input sanitization, system prompt isolation |
| To'lov firibgarligi | Server verification, RTDN, dedupe |
| Kontent plagiati | 7-gram check |
| Ma'lumot sizib chiqishi | Encryption, least privilege |
| Bot/cheat | Server-authoritative battle |
| DDoS | Rate limit, CDN, WAF |

## 8.2. Auth & Xavfsizlik

- **Argon2id parametrlar standarti:**
  - *Dev / Test muhiti:* `m=19456` (19 MB), `t=2`, `p=1` (testlarning tez o'tishi uchun)
  - *Production muhiti:* `m=65536` (64 MB), `t=3`, `p=4` (OWASP 2026 brutforsga qarshi oltin standarti)
- **Token tizimi:**
  - Access Token: JWT 15 daqiqa (stateless, RS256/HS256)
  - Refresh Token: 30 kun (DBda saqlanadi, rotatsiya va reuse detection — bir martadan ortiq ishlatilsa barcha sessiyalar bekor qilinadi)
- **O'zbekiston bozoriga moslashgan kirish usullari:**
  - Telefon raqam + SMS OTP (Eskiz / PlayMobile provayderi)
  - Telegram Login Widget / Telegram Bot OTP (SMS xarajatini 70% tejaydi va 99% konversiya beradi)
  - Google Sign-In (Android uchun 1-tap Google One Tap API)
  - Email + Parol (alternativ)
- **Xavfsizlik to'siqlari:**
  - 5 marta noto'g'ri urinishdan keyin IP va akkaunt 15 daqiqaga bloklanadi (Rate Limiting)
  - Parol o'g'irlanishini tekshirish (HaveIBeenPwned API integratsiyasi)

## 8.3. Maxfiylik

- Minimal PII (faqat ism, telefon/email, daraja)
- O'chirish huquqi (foydalanuvchi ilovadan to'liq profilni o'chira oladi)
- Shaxsiy ma'lumotlarni eksport qilish (JSON formatida)
- Voyaga yetmaganlar: 13+ (ota-ona roziligi belgisi bilan)
- Audit log (har bir kirish, to'lov va xavfsizlik o'zgarishi qayd etiladi)

## 8.4. O'zbekiston qonuni (ZRU-1125 talablari)

**ZRU-1125 (Shaxsiy ma'lumotlar to'g'risidagi qonun yangi tahriri):**
- **Lokalizatsiya talabi:** O'zbekiston fuqarolarining shaxsiy ma'lumotlari (PII: telefon, F.I.Sh, to'lov rekvizitlari) O'zbekiston hududida joylashgan serverlarda (masalan: Servercore Tashkent yoki Uztelecom Data Center) saqlanishi shart.
- **Biometrik audio ma'lumotlar (Speaking):** O'quvchining ovozli yozuvlari faqat AI tahlili davomida xotirada saqlanadi va tahlil yakunlangach 24 soat ichida to'liq o'chiriladi (Ephemeral audio processing). Hech qanday uchinchi tomon treninglariga berilmaydi.
- **Huquqiy hujjatlar:** Foydalanuvchi ro'yxatdan o'tayotganda "Foydalanish shartlari" (Terms of Service) va "Maxfiylik siyosati" (Privacy Policy) ga o'zbek va rus tillarida rozilik belgilashi majburiy.

## 8.5. IELTS va IP

- "IELTS" — British Council/IDP/Cambridge mulki
- Faqat **tavsifiy** ishlatish ("IELTS preparation")
- Rasmiy aloqa yo'qligi disclaimer
- Band descriptors — public, atributsiya
- Kitoblar — faqat unit xaritasi

## 8.6. Play siyosati

- Data safety form
- Privacy policy
- Account deletion
- Billing policy
- Content rating
- Families policy (agar 13+ bo'lsa)

---

# 9. Analitika

## 9.1. KPI daraxti

```
Install
  → Activation (placement + 1 dars) ≥ 50%
    → D1 ≥ 30%
      → D7 ≥ 15%
        → D30 ≥ 6%
          → Paid ≥ 1.5%
            → Retention 3-oy ≥ 40%
              → Referral ≥ 10%
```

## 9.2. Eventlar

| Event | Parametrlar |
|-------|-------------|
| `app_open` | source |
| `onboarding_complete` | goal, exam_date |
| `placement_complete` | cefr, band |
| `lesson_start` | unit_id |
| `lesson_complete` | unit_id, score |
| `writing_submit` | task_type, words |
| `writing_feedback_view` | band |
| `speaking_start` | part |
| `speaking_turn` | duration |
| `battle_room_create` | mode |
| `battle_join` | room_id |
| `battle_complete` | rank, score |
| `paywall_view` | trigger |
| `trial_start` | plan |
| `purchase` | plan, price |
| `refund` | reason |

## 9.3. Dashboardlar

- Funnel (install → paid)
- Retention (D1/D7/D30)
- Content (unit completion)
- AI (latency, cost, error)
- Battle (rooms, completion)
- Revenue (MRR, churn, refund)

---

# 10. DevOps

## 10.1. Muhitlar

| Muhit | Maqsad |
|-------|--------|
| dev | Lokal |
| staging | Test |
| prod | Foydalanuvchi |

## 10.2. CI/CD

- GitHub Actions
- Android: lint, test, build, Play internal
- Backend: test, build Docker, migrate, deploy
- Migrations: Prisma, forward-only

## 10.3. Backup

- PostgreSQL: kunlik, 7 kun
- S3: versiyalash
- Restore test: oyiga 1

## 10.4. Xarajat (boshlang'ich)

| Modda | $/oy |
|-------|------|
| Server (2 vCPU, 4GB) | $20–40 |
| PostgreSQL | $15–30 |
| Redis | $10–20 |
| S3/CDN | $10–30 |
| AI | $20–50 |
| Monitoring | $0–20 |
| **Jami** | **$75–190** |

## 10.5. Kill switches

- AI o'chirish (fallback)
- Battle o'chirish
- Billing o'chirish
- Kontent bloklash

---

# 11. Moliyaviy model

## 11.1. Unit economics

| Modda | Qiymat |
|-------|--------|
| Narx | 12 900 so'm (~$1.0) |
| Play ulushi (15%) | $0.15 |
| Net | $0.85 |
| Variable cost (avg) | $0.40 |
| Variable cost (cap) | $0.80 |
| Marja (avg) | $0.45 |
| Marja (cap) | $0.05 |

## 11.2. Uch stsenariy (yil 1)

| Stsenariy | Konversiya | B2C MRR | B2B MRR | Jami |
|-----------|-----------|---------|---------|------|
| Pessimistik | 0.9% | $250 | $200 | ~$450 |
| **Baza** | **1.5%** | **$430** | **$400** | **~$830** |
| Optimistik | 3% | $800 | $1 000 | ~$1 800 |

## 11.3. Yil 2

| Ko'rsatkich | Qiymat |
|-------------|--------|
| Install | 100 000 |
| Konversiya | 2.0% |
| B2C MRR | ~$1 400 |
| B2B markazlar | 40 |
| B2B MRR | ~$1 600 |
| **Jami MRR** | **~$3 000** |
| Xarajat/oy | ~$900 |
| **Sof** | **~$2 100** |

> **Xulosa:** O'zbekiston B2C yolg'iz katta biznes emas. B2B + regional —
> o'sish yo'li. Cost-recovery narx marketingni qoplamaydi.

---

# 12. Jamoa, timeline, byudjet

## 12.1. Rollar

| Rol | Kim | Yuk |
|-----|-----|-----|
| Product/Founder | Siz | 100% |
| Android | Siz + frilanser | 60% |
| Backend | Siz + frilanser | 40% |
| Dizayn | Frilanser | 2–3 hafta |
| Kontent lead | Siz | 20% |
| O'qituvchi reviewer | Shartnoma | 1.5 soat/unit |
| QA | Siz + 5 beta | — |

## 12.2. Timeline (21 hafta)

| Bosqich | Hafta | Natija |
|---------|-------|--------|
| M0 — dizayn/spec | 2 | Figma, API, DB |
| M1 — poydevor | 3 | Auth, CI, staging |
| M2 — o'quv yadrosi | 6 | Writing, Speaking, Lug'at |
| M3 — Battle | 4 | Room, WebSocket, bot |
| M4 — monetizatsiya | 3 | Billing, paywall |
| M5 — beta + launch | 3 | 100 beta → Play |
| **Jami** | **21** | **+ buffer = 24–28** |

## 12.3. Byudjet (lean)

| Modda | Summa |
|-------|-------|
| Dizayn | $500–1 000 |
| Kontent (15 unit) | $190–385 |
| Audio | $200–500 |
| Huquqiy | $500 |
| Play + domen | $50 |
| Server (6 oy) | $450–1 140 |
| Marketing (launch) | $500–1 500 |
| **Jami** | **$2 390–5 075** |

## 12.4. Gate'lar

| Gate | Shart |
|------|-------|
| G0 — Validatsiya | 20 intervyu + 5 o'qituvchi + 300 waitlist |
| G1 — Alpha | Core loop ishlaydi, 20 tester, crash-free 99% |
| G2 — AI sifati | Writing r ≥ 0.75, MAE ≤ 0.5, 200 insho |
| G3 — Beta | 100 user, D7 ≥ 15%, activation ≥ 50% |
| G4 — Launch | 1 000 install, paid ≥ 1.5%, rating ≥ 4.3, refund < 5% |
| G5 — B2B | 3 pilot markaz (3–6 oy) |
| G6 — Scale | D30 ≥ 6%, 15 unit, paid ≥ 1.5% |

## 12.5. Kill criteria

| Bosqich | To'xtatish |
|---------|-----------|
| Alpha | 8 haftada core loop ishlamasa |
| Beta | 3 oyda D7 < 10% yoki paid < 1% |
| Launch | 6 oyda 1 000 install va 1.5% paid bo'lmasa |
| B2B | 6 oyda < 3 markaz |
| AI | r < 0.7 → model/prompt qayta |

---

# 13. GTM va launch

## 13.1. Pre-launch (4 hafta)

- Landing + waitlist (Telegram)
- 10 o'qituvchi suhbati → 3 pilot
- 50 beta foydalanuvchi
- Telegram kanal: kunlik maslahat + mini-test
- Play Console: closed testing

## 13.2. Launch (1-hafta)

| Kanal | Harakat | KPI |
|-------|---------|-----|
| Play Store | ASO | 300 install |
| Telegram Ads | $300, 3 auditoriya | 500 install |
| 5 mikro-influencer | IELTS o'qituvchilari | 300 install |
| 3 til markazi | Promokod | 200 install |
| Referal | 1 oy bepul | 100 install |
| **Jami** | | **1 400 install** |

## 13.3. Birinchi oy KPI

| Metrika | Maqsad |
|---------|--------|
| Install | 1 000+ |
| Activation | ≥ 50% |
| D7 | ≥ 15% |
| Paid | ≥ 1.5% |
| Crash-free | ≥ 99% |
| Rating | ≥ 4.3 |
| Refund | < 5% |

## 13.4. ASO

- Title: "IELTS Writing & Speaking — AI"
- Keywords: ielts, writing, speaking, band 6.5, english
- Screenshots: 5 ta (feedback, band, battle, progress, pricing)
- Video: 30 soniya
- Rating prompt: 3-darsdan keyin

## 13.5. Telegram

- Kanal: kunlik 1 maslahat
- Bot: mini-test + waitlist
- Ads: 18–25 yosh, Toshkent/Samarqand
- Guruh: beta feedback

## 13.6. B2B

- 3 markaz pilot
- O'qituvchi demo: battle
- Narx: 99 000 so'm/oy
- Dashboard: v1.5

---

# 14. Risklar

## 14.1. Registr

| # | Risk | Ehtimol | Ta'sir | Yumshatish |
|---|------|---------|--------|------------|
| 1 | Bozor kichik | Yuqori | Yuqori | B2B + regional |
| 2 | Play merchant UZ | O'rta | Yuqori | Erta tekshirish |
| 3 | AI baho noto'g'ri | O'rta | Yuqori | 200 insho eval, interval |
| 4 | Kontent huquqi | O'rta | Yuqori | Aligned, not copied |
| 5 | Retention past | Yuqori | Yuqori | Countdown, 15 unit |
| 6 | Tarqatish qimmat | Yuqori | O'rta | Organik + B2B |
| 7 | Raqobat (Duolingo) | Yuqori | O'rta | Nisha, feedback |
| 8 | Yolg'iz jamoaning burnout | O'rta | Yuqori | Scope cut, frilanser |
| 9 | Play rad etishi | O'rta | O'rta | Siyosat checklist |
| 10 | Valyuta/to'lov | O'rta | O'rta | Lokal narx |

## 14.2. Tekshirilishi kerak

- [ ] Play merchant/payout UZ
- [ ] IELTS trademark chegarasi
- [ ] ZRU-1125 talablari
- [ ] Gemini narxi va sifat
- [ ] 20 intervyu natijalari
- [ ] 5 o'qituvchi fikri
- [ ] Kontent litsenziyasi (agar kerak bo'lsa)

## 14.3. Kill criteria

Yuqorida 12.5.

---

# 15. Yo'l xaritasi

## 15.1. 90 kun

| Hafta | Ish |
|-------|-----|
| 1–2 | Spec, Figma, 20 intervyu |
| 3–5 | Auth, DB, CI |
| 6–11 | Writing, Speaking, Lug'at |
| 12–15 | Battle |
| 16–18 | Billing, paywall |
| 19–21 | Beta, launch |

## 15.2. 12 oy

- Launch + 1 000 install
- 15 unit → 60 unit
- B2B 10 markaz
- 1 000+ paid
- D30 ≥ 6%

## 15.3. 3 yil

- Regional: KZ, KG, TJ
- Boshqa tillar: rus, koreys
- 50 000+ paid
- B2B 200+ markaz

---

# 16. Qarorlar jurnali

## 16.1. Qabul qilingan

| # | Qaror | Sana |
|---|-------|------|
| 1 | Android-only, Kotlin + Compose | 2026-11 |
| 2 | Node.js + Fastify + Prisma + PostgreSQL | 2026-11 |
| 3 | Nisha: IELTS 5.5 → 6.5, Writing + Speaking | 2026-11 |
| 4 | Native app (Mini App emas) | 2026-11 |
| 5 | Cost-recovery narx: 12 900 so'm/oy | 2026-11 |
| 6 | Kahoot-uslubidagi Battle | 2026-11 |
| 7 | Kontent: kitob = xarita, AI = material | 2026-11 |
| 8 | Boshqa tillar — keyingi loyiha | 2026-11 |
| 9 | Gemini + Android STT/TTS + ElevenLabs | 2026-11 |
| 10 | Freemium + Play Billing | 2026-11 |

## 16.2. Ochiq

| # | Savol | Kim hal qiladi |
|---|-------|----------------|
| 1 | Ilova nomi | Siz |
| 2 | Yuridik shaxs | Siz |
| 3 | Teacher rejasi v1 da? | Siz |
| 4 | Play merchant UZ | Tekshirish |
| 5 | Kontent litsenziyasi | Huquqshunos |
| 6 | Launch byudjeti | Siz |

---

# Ilova A — Global benchmarklar (qisqa)

| Ko'rsatkich | Qiymat |
|-------------|--------|
| Duolingo D1 / D7 / D30 | 37.8% / 20.8% / 12.0% |
| Duolingo D365 | 6.8% |
| Duolingo MAU | 133M |
| Duolingo revenue | $1.038B |
| Duolingo organik o'sish | 80% |
| Education D35 conversion | 2.1% |
| Google Play install→paid | 0.9% |
| iOS install→paid | 3.1% |
| Education refund | 4.86% |
| Yillik retention | 44.1% |
| Oylik retention | 17.0% |
| Trial 17–32 kun → paid | 42.5% |
| Trial < 4 kun → paid | 25.5% |
| Trial 0-kunda | 80–82% |
| 1 yildan keyin >$9k/oy | 1/20 ilova |
| Speak ARR | $100M |
| Speak enterprise | 500+ |
| ELSA users | 13M |
| ELSA narx | $3–6/oy |
| AI–inson korrelyatsiya | 0.78 |
| DET qabul qiluvchi dasturlar | 6 500+ |
| Telegram Markaziy Osiyo | 30M+ |

**Saboqlar:**
1. Retention — mahsulotning o'zi
2. Konversiya 2%, 10% emas
3. Yillik reja 2.5x yaxshiroq
4. Trial 0-kunda
5. Uzun trial yaxshiroq
6. Education refund eng yuqori
7. Speaking-first ishlaydi
8. Talaffuz + predicted band — wedge
9. AI baho — 0.78, "official" emas
10. DET — tahdid
11. Telegram — 30M+
12. Live 1:1 va kontent o'chirish — o'lim

---

# Ilova B — Boshqa tillar (kelajak)

> Bu bo'lim faqat keyingi loyiha uchun. **v1 ga ta'sir qilmaydi.**

- Boshqa o'rganiladigan tillar — joriy loyiha ishga tushgandan keyin
- Sxemada `language_pair` (source → target) hozir qo'yiladi
- UI matnlari resurslarda, kontent bazada, rubrikalar parametrlangan
- Har yangi til = yangi kontent + rubrika + ovoz + imtihon
- Brend "IELTS" ga bog'lanmasin — "exam prep"

---

# Ilova C — Manbalar

| Manba | Nima |
|-------|------|
| AppMagic / Udonis (2026) | Duolingo retention, revenue |
| RevenueCat State of Subscription Apps 2025–2026 | Konversiya, trial, churn, refund |
| Forbes (2025-11) | Speak $100M ARR |
| Y Combinator (2025) | Speak enterprise |
| ELSA / Tracxn / CNBC | ELSA $60M, 13M user |
| Yoon (2023) meta-analysis | AI–inson 0.78 |
| ETS e-rater | AI — QC |
| Signal Detection Theory (2026) | Score compression |
| Duolingo English Test | 6 500+ dastur |
| UMDSOFT / Evido | Telegram 30M+ |
| Macmillan English | Destination B1/B2/C1&C2 |
| Cambridge / Collins | IELTS metodikasi |
| DataReportal / GSMA | Internet, mobil |
| O'zbekiston qonuni ZRU-1125 | Ma'lumotlar |

---

*Versiya: 5.0 · Sana: 2026-11-03*  
*Bu fayl — yagona master-reja. Ziddiyat bo'lsa, shu fayl ustun.*  
*Eski v2 reja: `C:\Users\pc\.opencode\plan\PLAN-v2-archive.md`*


---

# 17. Battle WebSocket Protokoli (To'liq)

## 17.1 Socket.IO Event sxemasi

### Client → Server

```typescript
// Xona yaratish (host)
socket.emit('room:create', {
  level: 'B2',           // 'B1' | 'B2' | 'C1'
  topic: 'vocabulary',   // 'vocabulary' | 'grammar' | 'mixed'
  questionCount: 10,     // 10 | 15 | 20
  timePerQuestion: 20    // 10 | 20 | 30 soniya
})

// Xonaga qo'shilish
socket.emit('room:join', { pin: '847291', nickname: 'Aziza' })

// O'yinni boshlash (faqat host)
socket.emit('room:start', { pin: '847291' })

// Javob yuborish
socket.emit('game:answer', {
  pin: '847291',
  questionIndex: 2,
  answerIndex: 0,         // 0–3 (A/B/C/D)
  clientTimestamp: Date.now()
})
```

### Server → Client

```typescript
socket.emit('room:created',      { pin: '847291', roomId: 'uuid' })
socket.emit('room:player_joined', { players: [{ id, nickname, isHost }] })
socket.emit('game:countdown',    { seconds: 3 })
socket.emit('game:question', {
  index: 2, total: 10,
  text: 'What does "meticulous" mean?',
  options: ['Very careful', 'Reckless', 'Enthusiastic', 'Creative'],
  durationMs: 20000,
  endTime: 1699543200000   // server timestamp
})
socket.emit('game:answer_ack', { received: true })
socket.emit('game:question_end', {
  correctIndex: 0,
  stats: [67, 22, 0, 11],   // % (A/B/C/D)
  yourResult: { isCorrect: true, pointsEarned: 850, streakBonus: 100, responseMs: 8500 }
})
socket.emit('game:leaderboard', {
  top5: [{ rank: 1, nickname: 'Bobur', score: 4250, delta: '+1' }],
  you: { rank: 2, score: 3980 }
})
socket.emit('game:over', {
  podium: [{ rank: 1, nickname: 'Bobur', score: 8500 }],
  you: { rank: 2, score: 7980, xpEarned: 150 }
})
socket.emit('room:error', { code: 'ROOM_NOT_FOUND', message: '...' })
```

## 17.2 Bot Service

```typescript
// Rule-based, AI emas (latency simulyatsiya)
const BotDifficulty = {
  easy:   { accuracy: 0.30, minMs: 5000, maxMs: 18000 },
  medium: { accuracy: 0.60, minMs: 2000, maxMs: 10000 },
  hard:   { accuracy: 0.85, minMs: 500,  maxMs: 4000  }
}
// 1–3 bot; haqiqiy O'zbek ismlari; 15 soniya kutilgandan keyin faol
```

## 17.3 Reconnect protokoli

```
Uzilish:
  → Redis: SET player:{userId}:reconnect {token} EX 15
  → 15 soniya ichida qayta ulansa → o'yin davom etadi
  → 15 soniyadan keyin → slot saqlanmaydi, o'yin davom etadi

Host uzilishi:
  → 30 soniya kutiladi
  → Qayta ulanmasa → birinchi qo'shilgan o'yinchi host bo'ladi
```

---

# 18. CAC va Unit Economics (To'liq)

## 18.1 Kanal bo'yicha CAC

| Kanal | Xarajat | Install | CAC |
|-------|---------|---------|-----|
| Telegram Ads | $300 | 500 | $0.60 |
| Mikro-influencer (5×) | $500 | 300 | $1.67 |
| Til markazlari (promo) | $0 | 200 | $0.00 |
| Referal | $0 | 100 | $0.00 |
| ASO (organic) | $0 | 300 | $0.00 |
| **Blended CAC** | **$800** | **1 400** | **$0.57** |

## 18.2 LTV va Payback

```
ARPU: $1.00/oy  →  Net (Play 15%): $0.85/oy
Churn (oylik): ~20%  →  Avg duration: 5 oy
LTV = $0.85 × 5 = $4.25

LTV:CAC = 4.25 / 0.57 = 7.5:1  ✅ (maqsad: >3:1)
Payback  = 0.57 / 0.85 ≈ 21 kun  ✅ (maqsad: <6 oy)
```

> Unit economics sog'lom. Asosiy risk — tarqatish ko'lami, narx emas.

---

# 19. Hujjatlar Xaritasi (Yangilangan)

| Fayl | Mazmuni |
|------|---------|
| **PLAN.md** (shu fayl) | Yagona master-reja — barcha strategik qarorlar |
| [`DESIGN.md`](DESIGN.md) | To'liq UI/UX tizim — rang, font, komponentlar, wireframe'lar |
| [`TEST.md`](TEST.md) | Test strategiyasi — unit, integration, E2E, CI pipeline |
| [`ANALYSIS.md`](ANALYSIS.md) | Expert tahlil — topilgan muammolar, harakatlar |

---

*Versiya: 5.1 · Sana: 2026-11-09*
*Bu fayl — yagona master-reja. Ziddiyat bo'lsa, shu fayl ustun.*
