# 🎨 DESIGN.md — UI/UX Dizayn Tizimi
## IELTS AI Murabbiy — Android Ilovasi

> **Versiya:** 1.0 · **Sana:** 2026-11-09  
> **Platforma:** Android (Kotlin + Jetpack Compose + Material Design 3)  
> **Maqsadli segment:** 18–28 yosh, Aziza kabi foydalanuvchi

---

## 1. Dizayn Falsafasi

### 1.1 Asosiy tamoyillar

| Tamoyil | Tavsif |
|---------|--------|
| **Ishonch** | Ilova professionallik beradi — jiddiy, ammo qo'rqinchsiz |
| **Aniqlik** | Foydalanuvchi har doim "nima qilish kerak" ni biladi |
| **Tezlik** | 10–25 daqiqalik seans — har ekran maqsadga yo'naltirilgan |
| **Quvvat** | AI feedback kuchli, lekin "eslatuvchi do'st" uslubida |
| **O'zbek konteksti** | Mahalliy his, global sifat |

### 1.2 Duolingo dan farq
- Duolingo: o'ynoqi, gamification heavy → biz: **IELTS-focused, serious**
- Biz: kamroq karikatura, ko'proq **ma'lumot zichligi**
- Rang: Duolingo sariq-yashil → biz: **havorang asosli, akademik lekin zamonaviy**

---

## 2. 🎨 Rang Palitrasі (Material You)

### 2.1 Seed rong
```
Primary seed: #1A6FE8  (chuqur moviy — ishonch, akademiya)
```

### 2.2 Light Theme (asosiy)

| Token | Hex | Ishlatilish |
|-------|-----|-------------|
| `primary` | `#1A6FE8` | CTA tugmalar, aktiv holat |
| `onPrimary` | `#FFFFFF` | Primary ustidagi matn |
| `primaryContainer` | `#D6E4FF` | Kartalar, chip background |
| `onPrimaryContainer` | `#001849` | Container ustidagi matn |
| `secondary` | `#2ECC71` | To'g'ri javob, success, XP |
| `onSecondary` | `#FFFFFF` | — |
| `secondaryContainer` | `#C8F7DC` | To'g'ri javob background |
| `tertiary` | `#F39C12` | Streak, ogohlantirish, timer |
| `tertiaryContainer` | `#FEF3CD` | Streak background |
| `error` | `#E53935` | Noto'g'ri javob, xato |
| `errorContainer` | `#FFCDD2` | Xato background |
| `surface` | `#F8FAFC` | Ekran foni |
| `surfaceVariant` | `#EEF2FF` | Karta, bottom sheet |
| `outline` | `#B0BEC5` | Input chegarasi, divider |
| `outlineVariant` | `#E8EDF5` | Yengil divider |
| `background` | `#FAFBFF` | Asosiy fon |
| `onBackground` | `#1A1C1E` | Asosiy matn |
| `onSurface` | `#1A1C1E` | Karta ustidagi matn |
| `onSurfaceVariant` | `#44474E` | Ikkinchi darajali matn |

### 2.3 Dark Theme

| Token | Hex |
|-------|-----|
| `primary` | `#A8C8FF` |
| `onPrimary` | `#002E6B` |
| `primaryContainer` | `#004BA0` |
| `onPrimaryContainer` | `#D6E4FF` |
| `secondary` | `#6FD99A` |
| `tertiary` | `#FFD740` |
| `error` | `#FFB4AB` |
| `surface` | `#111318` |
| `background` | `#111318` |
| `onBackground` | `#E2E2E9` |
| `surfaceVariant` | `#44474E` |

### 2.4 Battle rejimi maxsus ranglari

```kotlin
// 4 javob tugmasi — Kahoot uslubi
val BattleRed    = Color(0xFFE53935)   // A variant
val BattleBlue   = Color(0xFF1565C0)   // B variant  
val BattleYellow = Color(0xFFF9A825)   // C variant
val BattleGreen  = Color(0xFF2E7D32)   // D variant

// Tugma ikonalar — klassik Kahoot shakllari
// A: ▲ Uchburchak   B: ◆ Olmos
// C: ● Doira        D: ■ Kvadrat
```

### 2.5 Band Score ranglari

```kotlin
val BandColors = mapOf(
  4.0..4.9 to Color(0xFFEF5350),  // Qizil — zaif
  5.0..5.4 to Color(0xFFFF7043),  // To'q to'q sariq
  5.5..5.9 to Color(0xFFFFA726),  // Sariq
  6.0..6.4 to Color(0xFF66BB6A),  // Yashil
  6.5..6.9 to Color(0xFF26A69A),  // Moviy-yashil
  7.0..7.9 to Color(0xFF42A5F5),  // Ko'k
  8.0..9.0 to Color(0xFF7E57C2)   // Binafsha
)
```

---

## 3. 📝 Typography (Material Type Scale)

### 3.1 Font tanlov
```
Primary: "Plus Jakarta Sans" (Google Fonts, bepul)
  → Zamonaviy, o'qilishi oson, akademik his
  → Variable font — barcha og'irliklar

Monospace (kod, band raqamlari): "JetBrains Mono" 
  → Band score ko'rsatish uchun
```

### 3.2 Type Scale

| Token | Font | Size | Weight | Line Height | Ishlatilish |
|-------|------|------|--------|-------------|-------------|
| `displayLarge` | PJS | 57sp | 400 | 64sp | — |
| `displayMedium` | PJS | 45sp | 400 | 52sp | — |
| `displaySmall` | PJS | 36sp | 500 | 44sp | Band score katta (6.5) |
| `headlineLarge` | PJS | 32sp | 600 | 40sp | Ekran sarlavhasi |
| `headlineMedium` | PJS | 28sp | 600 | 36sp | Karta sarlavhasi |
| `headlineSmall` | PJS | 24sp | 600 | 32sp | Section sarlavhasi |
| `titleLarge` | PJS | 22sp | 700 | 28sp | Tab, bottom bar |
| `titleMedium` | PJS | 16sp | 600 | 24sp | Karta nomi |
| `titleSmall` | PJS | 14sp | 600 | 20sp | Chip, badge |
| `bodyLarge` | PJS | 16sp | 400 | 24sp | Asosiy matn |
| `bodyMedium` | PJS | 14sp | 400 | 20sp | Ikkinchi darajali matn |
| `bodySmall` | PJS | 12sp | 400 | 16sp | Caption, label |
| `labelLarge` | PJS | 14sp | 600 | 20sp | Tugma matni |
| `labelMedium` | PJS | 12sp | 600 | 16sp | Chip matni |
| `labelSmall` | PJS | 11sp | 500 | 16sp | Overline |

---

## 4. 📏 Spacing va Layout Tizimi (8dp Grid)

```
4dp   → kichik bo'shliq (ikon-matn orasida)
8dp   → asosiy bo'shliq
12dp  → komponent ichki padding (kichik)
16dp  → standard padding
20dp  → katta komponent padding
24dp  → karta padding
32dp  → bo'lim orasida
48dp  → touch target minimal
56dp  → Bottom navigation bar balandligi
64dp  → Top app bar balandligi

Ekran ichki chetlari: 16dp (yon), 24dp (yuqori)
Karta yaxlitlanishi: 16dp (standart), 24dp (katta karta)
Tugma yaxlitlanishi: 12dp (medium), 50% (pill/full-round)
```

---

## 5. 🧩 Asosiy Komponentlar

### 5.1 Primary Button
```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}
// Balandlik: 56dp
// Radius: 12dp
// Font: labelLarge (14sp, 600)
// Minimal width: 120dp
```

### 5.2 Band Score Card
```kotlin
// Katta band raqami ko'rsatish
// Fon: primaryContainer
// Raqam: displaySmall (36sp), JetBrains Mono
// Qo'shimcha: "± 0.5 confidence" bodySmall
// Progress bar: band trajectory (6.0 → 6.5)
```

### 5.3 Writing Feedback Card
```kotlin
// Tuzilma:
//  [Band estimate: 6.0 ± 0.5]  ← top
//  ─────────────────────────
//  Criteria breakdown:
//  Task Achievement  ████████░░  6.5
//  Coherence         ███████░░░  6.0
//  Lexical Resource  ████████░░  6.5
//  Grammar           ██████░░░░  5.5
//  ─────────────────────────
//  [Highlighted text with errors]
//  Error 1: "which" → "that" — izoh (o'zbekcha)
//  ─────────────────────────
//  [Rewrite] tugmasi
```

### 5.4 Vocabulary Flashcard
```kotlin
// Flip animation (perspektiv 3D)
// Old: inglizcha so'z + IPA + audio ikona
// Orqa: o'zbekcha tarjima + misol gap + eslab qoldish tugmasi
// FSRS tugmalari: "Qiyin" "To'g'ri" "Oson" (1, 3, 5 kundan keyin)
// Progress ring: 90-kunlik retention goal
```

### 5.5 Battle Savol Ekrani
```kotlin
// Layout:
// [Vaqt: 15] ████████████░░░░ [3/10]  ← yuqori
//
// "What does 'ubiquitous' mean?"        ← savol
//
// ┌──────────────┐  ┌──────────────┐
// │ ▲ Everywhere │  │ ◆ Expensive  │  ← A qizil, B ko'k
// └──────────────┘  └──────────────┘
// ┌──────────────┐  ┌──────────────┐
// │ ● Rare       │  │ ■ Beautiful  │  ← C sariq, D yashil
// └──────────────┘  └──────────────┘
//
// Tugma o'lchami: (screenWidth - 48dp) / 2
// Font: headlineSmall (24sp)
// Minimum balandlik: 80dp
```

### 5.6 CountdownArc (Kahoot dumaloq taymeri)
```kotlin
// Joylashuvi: uz.inglish.app.ui.components.CountdownArc
// Parametrlar: remainingSeconds: Float, totalSeconds: Float = 20f, warningThresholdSeconds = 5f
// Xususiyatlari:
// - Soat 12 dan boshlanuvchi silliq Canvas yoyi (StrokeCap.Round)
// - Oxirgi 5 soniyada qizil rangga o'tish (#E53935) va shiddatli pulsatsiya (scale 1.0f -> 1.12f)
// - Markazda qolgan soniyalar va "sek" yozuvi
```

### 5.7 LiveWaveform (Speaking audio vizualizatori)
```kotlin
// Joylashuvi: uz.inglish.app.ui.components.LiveWaveform
// Parametrlar: isRecording: Boolean, barCount: Int = 12, activeColor: Color, maxBarHeight: Dp = 44.dp
// Xususiyatlari:
// - 12 ta garmonik chastota chizig'i (Gauss taqsimoti asosida markazda balandroq)
// - isRecording = true paytida silliq sine/infiniteTransition to'lqin harakati
// - isRecording = false paytida silliq boshlang'ich jimjit holatga qaytish
```

### 5.8 ProgressRings (Konsentrik kunlik maqsad halqalari)
```kotlin
// Joylashuvi: uz.inglish.app.ui.components.ProgressRings
// Parametrlar: writingProgress: Float, speakingProgress: Float, vocabProgress: Float, size: Dp = 160.dp
// Xususiyatlari:
// - 3 ta konsentrik yoy: Writing (tashqi moviy), Speaking (o'rta yashil), Vocabulary (ichki sariq)
// - Har bir yoy uchun animateFloatAsState bilan silliq to'lish animatsiyasi
// - Markazda umumiy foiz yoki emodzi/belgi
```

### 5.9 KahootOptionButton (Battle 4-rangli javob tugmalari)
```kotlin
// Joylashuvi: uz.inglish.app.ui.components.KahootOptionButton
// Parametrlar: index: Int (0..3), text: String, onClick: () -> Unit, selected: Boolean, isCorrectReveal: Boolean?
// Xususiyatlari:
// - 4 xil rang va geometrik shakl: A: Qizil (▲), B: Moviy (◆), C: Sariq (●), D: Yashil (■)
// - Bosilganda elastik scale (0.94f) va vibratsiya (LocalHapticFeedback)
// - Tanlangan va to'g'ri/noto'g'ri (reveal) holatlar uchun animatsiyalangan ramkalar
```

---

## 6. 📱 Navigatsiya Tizimi

### 6.1 Bottom Navigation (5 tab)

```
┌──────┬──────┬──────┬──────┬──────┐
│ 🏠   │ ✍️   │ 🗣️   │ 📚   │ 👤   │
│ Home │Write │Speak │ Vocab│Profile│
└──────┴──────┴──────┴──────┴──────┘
```

> **Nima uchun 5 tab?**  
> Battle navigation: Home ekranidan "Battle" karta → battle oqimi  
> Progress: Profile tab ichida  
> Bu foydalanuvchi mental modeliga mos

### 6.2 Ekran Ierarxiyasi

```
MainActivity (NavHost)
│
├── BottomNav
│   ├── HomeScreen
│   │   ├── TodayTaskCard
│   │   ├── BandProgressCard
│   │   ├── BattleCard → BattleFlow
│   │   ├── StreakCard
│   │   └── ExamCountdownCard
│   │
│   ├── WritingScreen
│   │   ├── TaskSelector (Task 1 / Task 2)
│   │   ├── WritingEditor
│   │   ├── SubmittingState (skeleton)
│   │   └── FeedbackScreen
│   │
│   ├── SpeakingScreen
│   │   ├── ScenarioSelector (Part 1/2/3)
│   │   ├── SpeakingRoleplay
│   │   │   ├── AI prompt card
│   │   │   ├── MicButton (pulse animation)
│   │   │   └── TranscriptCard
│   │   └── SpeakingFeedback
│   │
│   ├── VocabScreen
│   │   ├── DueCardsHeader (10 yangi + 25 takrorlash)
│   │   ├── FlashcardStack (swipe up = know, swipe down = again)
│   │   └── SessionComplete
│   │
│   └── ProfileScreen
│       ├── BandHistoryChart
│       ├── StreakHeatmap
│       ├── AchievementGrid
│       ├── SubscriptionCard
│       └── Settings
│
├── OnboardingFlow (1-marta)
│   ├── WelcomeScreen
│   ├── GoalScreen (maqsad band)
│   ├── ExamDateScreen (imtihon sanasi)
│   └── PlacementScreen
│
└── BattleFlow
    ├── BattleLobbyScreen (xona yaratish yoki qo'shilish)
    ├── BattleWaitingRoom (ishtirokchilar)
    ├── BattleCountdown (3-2-1)
    ├── BattleQuestionScreen
    ├── BattleAnswerReveal
    ├── BattleLeaderboard (oraliq)
    └── BattlePodium (yakuniy)
```

---

## 7. 🏠 Home Ekrani Dizayni

```
┌─────────────────────────────────────┐
│ [👤 Aziza]              [🔔] [⚙️]   │  ← Top bar (no elevation)
├─────────────────────────────────────┤
│                                     │
│  📅 Imtihongacha 47 kun             │  ← primaryContainer card
│  ████████████████░░░░░  6.0 → 6.5  │    band progress
│                                     │
├─────────────────────────────────────┤
│  Bugungi vazifa                     │
│  ┌─────────────────────────────┐    │
│  │ ✍️  1 Writing (Task 2)      │    │  ← primary colored card
│  │    "Davlatning ijtimoiy..."  │    │
│  │    [Boshlash]               │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌──────────┐  ┌──────────────┐     │
│  │ 📚 10    │  │ 🗣️ 5 daqiqa │     │  ← kichik kartalar
│  │ Vocab    │  │   Speaking   │     │
│  └──────────┘  └──────────────┘     │
│                                     │
│  ⚔️ Battle                 [Ko'rish]│  ← Battle section
│  ┌─────────────────────────────┐    │
│  │  Xona yaratish | PIN kirish │    │
│  └─────────────────────────────┘    │
│                                     │
│  🔥 Streak: 7 kun                   │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   │  ← Haftaning kurlari
│  D T C P J S Y                     │
│  ✓ ✓ ✓ ✓ ✓ ✓ ✓                     │
└─────────────────────────────────────┘
  🏠      ✍️      🗣️      📚      👤
```

---

## 8. ✍️ Writing Ekrani Dizayni

### 8.1 Editor holati
```
┌─────────────────────────────────────┐
│ ← Writing    Task 2    [Saqlash]    │
├─────────────────────────────────────┤
│ 📋 Topshiriq                        │
│  "Some people think that the best   │
│   way to improve public health is   │
│   to increase the number of..."     │
│                                     │
│  Min: 250 so'z  |  Time: 40 daqiqa │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ [Matn kiritish maydoni]         │ │
│ │                                 │ │
│ │                                 │ │
│ │                                 │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│  So'zlar: 127 / 250  🟡             │
│                                     │
│  [🤖 AI baho olish]  ← PRO tugma   │
└─────────────────────────────────────┘
```

### 8.2 Feedback holati
```
┌─────────────────────────────────────┐
│ ← Feedback              [📋 Nusxa] │
├─────────────────────────────────────┤
│  Taxminiy ball: 6.0 ± 0.5          │
│  ┌────────────────────────────────┐ │
│  │ Task Achievement  ████████░  6.5│ │
│  │ Coherence         ███████░░  6.0│ │
│  │ Lexical Resource  ████████░  6.5│ │
│  │ Grammar Accuracy  ██████░░░  5.5│ │
│  └────────────────────────────────┘ │
├─────────────────────────────────────┤
│  📝 Xatolar va takliflar            │
│                                     │
│  "...I [think] that this is a..."   │  ← sariq highlight
│  💡 "think" o'rniga "believe" yoki  │
│     "argue" ishlatish rasmiyroq     │
│                                     │
│  "...which improve health..."       │  ← qizil highlight  
│  ❌ "which" → "that" (restrictive   │
│     clause uchun)                   │
│                                     │
│  [🔄 Qayta yoz]    [💾 Saqlash]    │
└─────────────────────────────────────┘
```

---

## 9. ⚔️ Battle Ekrani Dizayni (To'liq Oqim)

### 9.1 Lobby ekrani
```
┌─────────────────────────────────────┐
│ ← Battle rejimi                     │
├─────────────────────────────────────┤
│                                     │
│         ⚔️ Battle                   │
│                                     │
│  ┌─────────────────────────────┐    │
│  │   + Yangi xona yaratish     │    │
│  │   Mavzu: [Lug'at ▼]        │    │
│  │   Savollar: [10 ▼]          │    │
│  │   Vaqt: [20 sek ▼]          │    │
│  │   [Xona yaratish]           │    │
│  └─────────────────────────────┘    │
│                                     │
│       — yoki —                      │
│                                     │
│  ┌─────────────────────────────┐    │
│  │   # PIN kodi kiriting       │    │
│  │   [ _ _ _ _ _ _ ]          │    │
│  │   [Qo'shilish]              │    │
│  └─────────────────────────────┘    │
│                                     │
│  🤖 Bot bilan mashq qiling           │  ← bepul
└─────────────────────────────────────┘
```

### 9.2 Kutish xonasi
```
┌─────────────────────────────────────┐
│                     [PIN: 847 291]  │
├─────────────────────────────────────┤
│  Ishtirokchilar (3/10)              │
│                                     │
│  👤 Aziza (host)  ✓                 │
│  👤 Bobur         ✓                 │
│  👤 Madina        ✓                 │
│  ...                                │
│                                     │
│         ⏳ Kutilmoqda...             │
│                                     │
│         [▶ Boshlash]                │  ← faqat host ko'radi
│    (Kamida 2 kishi kerak)           │
└─────────────────────────────────────┘
```

### 9.3 Savol ekrani (Kahoot uslubi)
```
┌─────────────────────────────────────┐
│ [3/10]      ⏱️ 15        [🔥×2]    │  ← streak
├─────────────────────────────────────┤
│  ████████████████░░░░░░░░░░░░░░░░░  │  ← timer progress bar (orange)
├─────────────────────────────────────┤
│                                     │
│   "What does 'meticulous' mean?"    │  ← savol (headlineMedium)
│                                     │
├─────────────────────────────────────┤
│ ┌──────────────────┐ ┌────────────┐ │
│ │ ▲ Very careful   │ │ ◆ Reckless │ │  ← A: qizil, B: ko'k
│ └──────────────────┘ └────────────┘ │
│ ┌──────────────────┐ ┌────────────┐ │
│ │ ● Enthusiastic   │ │ ■ Creative │ │  ← C: sariq, D: yashil
│ └──────────────────┘ └────────────┘ │
│                                     │
│         👥 Aziza, Bobur, Madina     │  ← javob berganlari (ism emas, rank)
└─────────────────────────────────────┘
```

### 9.4 Javob reveal ekrani
```
┌─────────────────────────────────────┐
│                                     │
│   ✅ To'g'ri: ▲ Very careful        │  ← yashil background
│                                     │
│   Siz: ✅ +850 ball  (8.5 sek)      │
│   Streak: 🔥×3 → +100 bonus        │
│                                     │
│   📊 Statistika:                    │
│   A ████████████ 67% (2/3)         │
│   B ████ 22% (1/3)                  │
│   C ░░░░  0%                        │
│   D ████ 11% (1/3)                  │
│                                     │
│         [Keyingisi ▶]               │  ← faqat host
└─────────────────────────────────────┘
```

### 9.5 Oraliq Leaderboard
```
┌─────────────────────────────────────┐
│  5-savoldan keyin      [5/10]       │
├─────────────────────────────────────┤
│  🥇 Bobur        4 250  ↑           │
│  🥈 Aziza        3 980  —           │
│  🥉 Madina       3 100  ↑           │
│  4. Kamol        2 870  ↓           │
├─────────────────────────────────────┤
│  Siz: 🥈 Aziza   3 980              │  ← highlight
│       1-o'ringa: 270 ball qoldi     │
└─────────────────────────────────────┘
(5 soniya, keyin avtomatik davom etadi)
```

### 9.6 Podium (yakuniy)
```
┌─────────────────────────────────────┐
│  🎉 O'yin tugadi!                   │
├─────────────────────────────────────┤
│                                     │
│         🥇                          │
│       Bobur                         │
│       8 500                         │
│    ┌───────────┐                    │
│    │           │                    │
│ 🥈 │           │ 🥉                 │
│Aziza│           │Madina              │
│7 980│           │5 300               │
│┌───┐│           │┌───┐              │
││   ││           ││   │              │
└────────────────────────────────────┘
│  Siz: 🥈 Aziza                      │
│  Jami: 7 980 ball  +150 XP          │
│  Streak: 🔥 yangi rekord!            │
│                                     │
│  [🔄 Qayta o'ynash]  [🏠 Chiqish]  │
└─────────────────────────────────────┘
```

---

## 10. 🗣️ Speaking Ekrani Dizayni

```
┌─────────────────────────────────────┐
│ ← Speaking     Part 2    [⏱️ 2:00] │
├─────────────────────────────────────┤
│  📋 Topshiriq                       │
│  "Talk about a memorable journey    │
│   you have taken. You should say:   │
│   • where you went                  │
│   • who you went with..."           │
│  [1 daqiqa o'ylash]  00:45         │
├─────────────────────────────────────┤
│                                     │
│         [🎤]                        │  ← katta yashil mikrofon
│    ░░░░░░░░░░░░░░░░░░░              │  ← audio waveform (animatsiya)
│    Gapiring... 1:23                 │
│                                     │
│  "I would like to talk about my     │  ← real-time transkript
│   trip to Samarkand last summer..." │
│                                     │
│         [⏹ Tugatish]               │
└─────────────────────────────────────┘
```

---

## 11. 📚 Vocabulary Ekrani (FSRS)

```
┌─────────────────────────────────────┐
│ ← Lug'at         10 yangi | 25 rep │
├─────────────────────────────────────┤
│  Progress: ████████████░░░░ 72%     │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  │                             │    │
│  │      METICULOUS             │    │  ← flip card, old tomonı
│  │       /mɪˈtɪkjʊləs/        │    │
│  │                             │    │
│  │          [🔊]               │    │
│  └─────────────────────────────┘    │
│                                     │
│    👇 Ko'rish uchun bosing          │
│                                     │
│  ← Bilmadim    Bildim! →           │  ← swipe gesture
└─────────────────────────────────────┘

--- Orqa tomoni (flip) ---

│  ┌─────────────────────────────┐    │
│  │   METICULOUS                │    │
│  │   O'ta ehtiyotkor, aniq     │    │  ← o'zbekcha
│  │                             │    │
│  │   "She was meticulous       │    │  ← misol gap
│  │    about her research."     │    │
│  │   "U tadqiqotida juda       │    │
│  │    ehtiyotkor edi."         │    │
│  └─────────────────────────────┘    │
│                                     │
│  [Qiyin]   [To'g'ri]   [Oson]      │
│  3 kun      7 kun       21 kun      │
```

---

## 12. 👤 Profile Ekrani

```
┌─────────────────────────────────────┐
│  Profil                     [⚙️]   │
├─────────────────────────────────────┤
│  [Avatar]  Aziza Karimova           │
│            🎓 B2 • IELTS prep       │
│            📅 Imtihon: 47 kun       │
├─────────────────────────────────────┤
│  Band tarixi                        │
│  7 ┤                        ●       │
│  6 ┤          ●    ●   ●  ●         │
│  5 ┤  ●   ●                         │
│    └────────────────────────────    │
│    Avg  Okt  Nov  Dec  Jan          │
├─────────────────────────────────────┤
│  🔥 Streak: 7 kun (rekord: 23)      │
│  ⭐ XP: 2 450  •  🏆 8 yutuq        │
├─────────────────────────────────────┤
│  💎 PRO (12 900 so'm/oy)            │
│  Keyingi to'lov: 2027-01-15         │
│  [Boshqarish]                       │
└─────────────────────────────────────┘
```

---

## 13. 💰 Paywall (Narx ekrani)

```
┌─────────────────────────────────────┐
│                     [✕]            │
├─────────────────────────────────────┤
│                                     │
│    ✍️  Writing feedback bepul       │
│        tugadi                       │
│    Writing'ingizni baholash uchun   │
│    PRO ga o'ting                    │
│                                     │
├─────────────────────────────────────┤
│  ✅ Cheksiz writing baholash        │
│  ✅ Cheksiz speaking sessiyalari    │
│  ✅ Battle (3+ xona)                │
│  ✅ Band tarixi va statistika       │
│  ✅ 500 lug'at FSRS                 │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐    │
│  │  ★ YILLIK (eng foydali)     │    │  ← highlighted
│  │  99 000 so'm/yil            │    │
│  │  = 8 250 so'm/oy  (-36%)    │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  Oylik                      │    │
│  │  12 900 so'm/oy             │    │
│  └─────────────────────────────┘    │
│                                     │
│  [14 kun bepul sinab ko'ring]       │  ← CTA
│  Istalgan vaqt bekor qiling         │
│                                     │
│  Maxfiylik siyosati  |  Shartlar   │
└─────────────────────────────────────┘
```

---

## 14. 🌙 Dark Mode

Barcha tokenlar dark theme uchun §2.3 da aniqlangan.

**Maxsus qoidalar:**
- Surface elevation: dark modda hafif ko'tarilish `surface+1dp` = `#1E2026`
- Karta foni: `#1E2026` (surface) → `#252932` (surfaceVariant)
- Battle tugmalar dark modda: 20% qoraytirilgan variant
- Waveform: `secondary` rang (yashil) + opacity 0.7

---

## 15. 🎭 Animatsiyalar

| Element | Animatsiya | Davomiylik |
|---------|-----------|------------|
| Ekran o'tish | Shared element + fade | 300ms |
| Karta flip (vocab) | 3D perspektiv | 400ms |
| Battle countdown | Scale pulse (1.0→1.2→1.0) | 500ms/son |
| Podium paydo | Slide up + bounce | 600ms |
| Mic recording | Pulse + waveform | Loop |
| Band score | CountUp animatsiya | 1000ms |
| Timer bar | Linear progress, red last 5s | Continuous |
| Streak fire | Lottie animatsiya | Loop |
| Correct answer | Konfetti (Lottie) | 1500ms |
| Wrong answer | Shake (horizontal) | 300ms |

**Muhim:** `reduceMotion` aniqlansa → barcha animatsiyalar off.

---

## 15.1 📦 Tavsiya etilgan Jetpack Compose Kutubxonalari

| Kutubxona | Bog'liqlik (Gradle) | Vazifasi / Qayerda ishlatilishi |
|-----------|---------------------|--------------------------------|
| **Vico** | `com.patrykandpatrick.vico:compose:1.15.0` | Band score (4.0 → 9.0) oylik dinamik grafigi, spline chart |
| **Konfetti** | `nl.dionsegijn:konfetti-compose:2.0.4` | Battle yutug'i (1-o'rin) va Band 7.0+ natijada partlovchi konfetti |
| **Shimmer** | `com.valentinilk.shimmer:compose-shimmer:1.3.1` | AI Writing/Speaking tahlili vaqtida skeleton shimmer effekti |
| **Coil** | `io.coil-kt.coil3:coil-compose:3.0.4` | Avatarlar va IELTS mavzulari rasmlarini asinxron keshlab yuklash |

---

## 15.2 🇺🇿 O'zbekiston Bozoriga Mos UI Xususiyatlari

1. **Kirish (Auth) — Telefon + Telegram / SMS OTP:**
   - Foydalanuvchi elektron pochta o'rniga `+998 XX XXX-XX-XX` kiritadi.
   - SMS bilan bir qatorda Telegram Bot orqali 1-click OTP tasdiqlash tugmasi ko'rsatiladi (O'zbekistonda eng tez va arzon usul).
2. **Shriftlar va Kirill/Lotin gliflari:**
   - O'zbek tili lotin imlosidagi tutuq belgisi (`oʻ`, `gʻ`) uchun maxsus `Inter` va `Plus Jakarta Sans` UTF-8 fontlari qo'llaniladi.
3. **Band Score Reveal Animatsiyasi (CountUp):**
   - Natija chiqqanda 0.0 dan boshlab o'quvchining haqiqiy balligacha (masalan 6.5) 1.2 soniya davomida raqamlar aylanib chiqadi.


---

## 16. 📐 Figma Fayli Tuzilmasi

```
📁 IELTS AI App
├── 📄 Cover
├── 📄 Design System
│   ├── Colors (Light + Dark)
│   ├── Typography
│   ├── Spacing
│   └── Icons
├── 📄 Components
│   ├── Buttons (Primary, Secondary, Ghost)
│   ├── Cards (Band, Writing, Vocab, Battle)
│   ├── Inputs (Text, Pin)
│   ├── Bottom Nav
│   └── Dialogs
├── 📄 Flows
│   ├── Onboarding
│   ├── Home
│   ├── Writing
│   ├── Speaking
│   ├── Vocabulary
│   ├── Battle
│   ├── Profile
│   └── Paywall
└── 📄 Prototypes
    ├── Core flow
    └── Battle flow
```

---

## 17. ✅ Dizayn Validatsiya Checklist

**Frilanser dizayner topshirishdan oldin:**
- [ ] Barcha rang tokenlar §2 ga mos
- [ ] Typography §3 ga mos
- [ ] 8dp grid saqlangan
- [ ] Dark mode barcha ekranlarda tekshirilgan
- [ ] Touch target minimal 48dp
- [ ] Kontrast 4.5:1 (WCAG AA)
- [ ] Reduced motion variant bor
- [ ] Font o'lchami 200% da sinab ko'rilgan (accessibility)
- [ ] Battle ekrani 320dp minimal ekranda ishlaydi
- [ ] Paywall 3 narx rejasini ko'rsatadi (Founder + Oylik + Yillik)
- [ ] Boʻsh holat (empty state) har ekranda bor
- [ ] Loading skeleton har ekranda bor
- [ ] Xato holat (error state) har ekranda bor

---

*DESIGN.md v1.0 · 2026-11-09*  
*Figma link: [qo'shiladi]*

