# Inglish — IELTS 5.5 → 6.5

O'zbekistonlik IELTS topshiruvchilar uchun AI murabbiy: **Writing**, **Speaking**,
lug'at va **Kahoot uslubidagi Battle**.

> Mahsulot, texnik va biznes reja: [`PLAN.md`](PLAN.md)  
> API shartnomasi: [`docs/API.md`](docs/API.md)

## Holat

| Qism | Holat |
|---|---|
| Monorepo skeleti | ✅ |
| Backend: health + auth (register/login/refresh/logout/me/profile/delete) | ✅ |
| Backend: email tasdiqlash + parolni tiklash (6 xonali kodlar) | ✅ |
| Backend: content, vocabulary (FSRS), writing, speaking, battle, placement | ✅ |
| Backend: subscription + kunlik/oylik kvotalar | ✅ |
| Prisma: users, content, practice, submissions, subscriptions, battle | ✅ |
| Auth va content oqimi haqiqiy PostgreSQL'da tekshirildi | ✅ |
| Backend typecheck + 67 test | ✅ |
| Android: Compose login/register/home + API client + token refresh | ✅ |
| Android `assembleDebug` | ✅ (JDK 21, `BUILD SUCCESSFUL`) |
| Android: Darslar, Writing, Speaking, Vocabulary, Battle, Placement ekranlari + bottom navigation | ✅ |
| Android: `/v1/usage` limitlari, profil/maqsad sozlamalari, hisobni o'chirish | ✅ |
| Android: email tasdiqlash banneri + 6 xonali kod dialogi | ✅ |
| Android: "Parolni unutdingizmi?" tiklash oqimi | ✅ |
| Android: javob kalitlarini serverda tekshirish (`POST /content/exercises/:id/answer`) | ✅ |
| Backend: `GET /v1/progress` (band trend, streak, lug'at, mashq aniqligi) | ✅ |
| Android: Asosiy ekranda taraqqiyot kartasi (band, maqsad, streak, aniqlik) | ✅ |
| Battle (bot + friend + classroom, WebSocket, server-authoritative) | ✅ |
| Placement (15 daqiqa, 25 savol, CEFR/band natijasi) | ✅ |
| Billing: Play Billing 7 client, server verify, RTDN, entitlement, paywall | ✅ |
| Speaking audio: qurilma STT (`SpeechRecognizer`) + TTS | ✅ |
| Backend: `Idempotency-Key` replay (AI submit, billing, battle, review) | ✅ |
| Android: tokenlar Keystore AES-256-GCM bilan shifrlangan | ✅ |

## Tuzilma

```
.
├── PLAN.md              # Master-reja (v5.0)
├── docs/API.md          # v1 API shartnomasi
├── backend/             # Node.js + Fastify + Prisma + PostgreSQL
│   ├── prisma/
│   ├── src/
│   ├── scripts/dev-db.mjs
│   └── test/
├── android/             # Kotlin + Jetpack Compose
│   └── app/src/main/java/uz/inglish/app/
└── docker-compose.yml   # PostgreSQL (production-like dev)
```

## Backend

Talablar: **Node.js 20+**.

```bash
cd backend
npm install
cp .env.example .env
# .env ichidagi JWT_SECRET'ni almashtiring:
node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
```

### Variant A — embedded PostgreSQL (eng tez, Docker kerak emas)

```bash
npm run db:dev        # 5433-portda lokal PostgreSQL ishga tushadi
```

`.env` ichida:

```env
DATABASE_URL=postgresql://inglish:inglish@localhost:5433/inglish?schema=public
```

### Variant B — Docker Compose

```bash
docker compose up -d postgres
```

`.env` ichida `localhost:5432` ishlatiladi.

### Migratsiya va ishga tushirish

```bash
npm run db:push       # jadvallarni yaratadi (prototip/dev)
npm run db:seed       # demo content
npm run dev           # http://localhost:3000
```

Yangi yoki production muhit uchun migratsiyalar:

```bash
npx prisma migrate deploy
```

`prisma/migrations/` papkasi mavjud. Allaqachon `db:push` bilan yaratilgan lokal baza uchun
bir marta bazaviy migratsiyani belgilash kerak:

```bash
npx prisma migrate resolve --applied 20261105000000_init
```

Foydali buyruqlar:

```bash
npm run typecheck
npm test
npm run build
npm run db:studio
npm run smoke:auth       # auth + refresh rotation
npm run smoke:auth:hardening  # email verification, password reset, session revocation
npm run smoke:content    # content, vocabulary, writing, speaking
npm run smoke:features   # profile, answer keys, usage, progress, account deletion
npm run smoke:battle     # bot battle: room, scoring, finish, ranking
npm run smoke:battle:multiplayer  # friend room, code join, ready/start, WebSocket, reconnect
npm run smoke:placement  # placement: session, scoring, profile update
npm run smoke:billing    # billing: products, verify, restore, RTDN, entitlement
npm run smoke:idempotency  # Idempotency-Key replay, conflict va invalid key
```

Demo auth oqimi:

```bash
curl http://localhost:3000/health
```

## Android

Talablar:

- JDK 17+ (tavsiya: 21)
- Android SDK Platform 36
- Android SDK Build-Tools
- Android Studio yoki `ANDROID_HOME`

`android/local.properties`:

```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

Build:

```bash
cd android
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`

### API manzili

`android/app/build.gradle.kts` ichida build type bo'yicha:

```kotlin
debug   → http://10.0.2.2:3000/
release → https://api.inglish.uz/
```

- Emulyator: `10.0.2.2` — host mashinadagi `localhost`.
- Haqiqiy qurilma: kompyuterning LAN IP'si (masalan `http://192.168.1.20:3000/`).
  Debug source set'dagi `network_security_config.xml` cleartext HTTP/WebSocket'ga ruxsat beradi.
- Release build faqat HTTPS'ga ruxsat beradi; cleartext faqat debug source set'da.

### Telefonda sinash

Debug build'ning API manzilini build vaqtida almashtirish mumkin:

```powershell
cd android
.\gradlew.bat assembleDebug -PapiBaseUrl=http://192.168.1.20:3000/
```

- `192.168.1.20` — kompyuterning LAN IP'si (`ipconfig` → IPv4 Address).
- Telefon va kompyuter bir Wi-Fi tarmog'ida (yoki telefon hotspotida) bo'lishi kerak.
- Windows Firewall'da Node.js uchun 3000-portga inbound ruxsat bo'lishi shart.
- Tekshirish: telefon brauzerida `http://192.168.1.20:3000/health` → `{"status":"ok"}`.
- APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

USB orqali (Wi-Fi/firewall muammosi bo'lmasa):

```powershell
adb reverse tcp:3000 tcp:3000
.\gradlew.bat assembleDebug -PapiBaseUrl=http://127.0.0.1:3000/
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

`adb reverse` telefon har safar qayta ulanganda qayta bajariladi.

Battle deep link'ini tekshirish:

```powershell
adb shell am start -a android.intent.action.VIEW -d "inglish://battle/482913" uz.inglish.app
```

## Auth oqimi

```
register/login ──► accessToken (15m) + refreshToken (30d)
                         │
                         ├─ Authorization: Bearer <accessToken>
                         │
accessToken muddati tugasa ──► POST /v1/auth/refresh ──► yangi juftlik
                                                        (eski refresh bekor)
```

- Parollar: Argon2id.
- Refresh tokenlar: bazada SHA-256 hash.
- AI/ElevenLabs kalitlari faqat serverda.
- AI bahosi hech qachon rasmiy IELTS balli deb ko'rsatilmaydi.

## Keyingi qadamlar

1. PostgreSQL'ni ishga tushirib (`npm run db:dev`), emulyatorda to'liq oqimni tekshirish:
   login → Darslar → Writing → Speaking → Lug'at → Battle.
2. Speaking: serverga audio yuklash va talaffuz (pronunciation) tahlili.
3. Battle: spectator rejimi, weekly tournament va reconnect grace (v1.5).
4. Play Console'da mahsulotlarni yaratish va service account bilan haqiqiy verify'ni yoqish (hozir dev'da mock).
5. Kontent generatsiya pipeline'i: yangi so'z va mashqlarni AI bilan tayyorlab, tekshiruvdan o'tkazish.
6. Xavfsizlik: Google sign-in va foydalanuvchi bo'yicha rate limit.
7. Gemini baholashni 200 ta essey bilan kalibrlash va 5% inson tekshiruvi.

## Ma'lum cheklovlar

- Speaking audio qurilma STT xizmati orqali transkriptga aylanadi (serverga audio yuborilmaydi); talaffuz (pronunciation) tahlili hali yo'q. Google nutq xizmati bo'lmagan qurilmada matn kiritish ishlayveradi.
- Battle bot, do'stlar va sinf xonalarini qo'llab-quvvatlaydi; WebSocket real-time, room code, deep link va reconnect bor. Spectator, weekly tournament va random matchmaking v1.5 ga qoldirilgan. Battle natijalari progress/streak statistikasiga qo'shilmaydi. WebSocket hub bitta server instansiyasi uchun (gorizontal scaling uchun Redis pub/sub kerak).
- Placementda grammatika, lug'at, o'qish, yozuv (60+ so'z) va gapirish (1 daqiqa transkript) bor; natija practice estimate hisoblanadi.
- Lug'at bazasida 520 ta so'z bor: 10 ta IELTS mavzu to'plami (har birida 50 ta) + 20 ta akademik demo so'z.
- Progress kartasi umumiy ko'rsatkichlarni beradi; batafsil band trend grafigi va haftalik hisobot hali yo'q.
- Billing: debug APK `BILLING_MOCK=true` bilan serverning `mock:<productId>:<days>` tokenlaridan foydalanadi. Haqiqiy Play xaridi uchun Play Console'da `inglish_pro_monthly`, `inglish_pro_yearly`, `inglish_teacher_monthly` mahsulotlari va service account (`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` yoki `..._FILE`) kerak. Release APK'da mock o'chirilgan.
- Email tasdiqlash va parol tiklash kodlari dev'da `EMAIL_MODE=log` bilan server logiga yoziladi (`GET /v1/dev/outbox` orqali smoke testlar o'qiydi). Production uchun SMTP (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `EMAIL_FROM`) sozlanishi shart.
- RTDN webhook imzosi `PLAY_RTDN_AUDIENCE` (Google OIDC) yoki `PLAY_RTDN_TOKEN` bilan tekshiriladi; production'da ikkalasi ham sozlanmasa endpoint `503` qaytaradi.
- Tokenlar Android Keystore AES-256-GCM kaliti bilan shifrlangan holda saqlanadi; eski plaintext tokenlar birinchi o'qishda avtomatik shifrlanadi. Keystore kaliti yo'qolsa (qurilma wipe yoki OS kalitni bekor qilsa) foydalanuvchi qayta login qiladi.
- AI submit, billing verify, battle room create va vocabulary review `Idempotency-Key` headerni qo'llab-quvvatlaydi; muvaffaqiyatli javob 24 soat saqlanadi va takroriy so'rovda `Idempotency-Replayed: true` bilan qaytariladi. 4xx/5xx javoblar saqlanmaydi.
- Global rate limit hozircha IP bo'yicha (reja: foydalanuvchi bo'yicha 60/min).
