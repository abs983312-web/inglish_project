package uz.inglish.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.core.network.dto.UsageBucketDto
import uz.inglish.app.core.network.dto.UsageDto
import uz.inglish.app.core.network.dto.UserDto
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.ProgressRings
import uz.inglish.app.ui.components.formatBand
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    user: UserDto?,
    usageState: UsageUiState,
    progressState: ProgressUiState,
    profileError: String?,
    verifyLoading: Boolean,
    verifyNotice: String?,
    verifyError: String?,
    onRefreshUsage: () -> Unit,
    onRefreshProgress: () -> Unit,
    onOpenLessons: () -> Unit,
    onOpenWriting: () -> Unit,
    onOpenSpeaking: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenBattle: () -> Unit,
    onOpenPlacement: () -> Unit,
    onOpenPaywall: () -> Unit,
    onUpdateProfile: (displayName: String, cefrLevel: String?, targetBand: Double?, examDate: String?) -> Unit,
    onClearProfileError: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRequestEmailVerification: () -> Unit,
    onConfirmEmailVerification: (String) -> Unit,
    onClearVerifyMessages: () -> Unit,
    onLogout: () -> Unit,
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var verifyCode by remember { mutableStateOf("") }

    val name = user?.profile?.displayName ?: "o'quvchi"
    val targetBand = user?.profile?.targetBand
    val profile = user?.profile
    val needsOnboarding = profile == null ||
        !profile.onboardingDone ||
        profile.targetBand == null ||
        profile.examDate == null

    val daysLeft = remember(profile?.examDate) {
        val examDate = profile?.examDate
        if (examDate.isNullOrBlank()) {
            null
        } else {
            runCatching {
                ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(examDate))
            }.getOrNull()
        }
    }

    val countdown = when (val days = daysLeft) {
        null -> "Sana kiritilmagan"
        else -> when {
            days < 0 -> "Imtihon sanasi o'tgan"
            days == 0L -> "Bugun"
            else -> "$days kun"
        }
    }

    LaunchedEffect(user?.emailVerified) {
        if (user?.emailVerified == true) {
            showVerifyDialog = false
            verifyCode = ""
            onClearVerifyMessages()
        }
    }

    LaunchedEffect(verifyNotice) {
        if (verifyNotice != null && user?.emailVerified != true) {
            showVerifyDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Salom, $name 👋",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = InglishInk,
                    )
                    Text(
                        text = targetBand?.let { "Bugun $it sari bir qadam." }
                            ?: "Maqsad bandini belgilang.",
                        color = InglishMuted,
                        fontSize = 13.sp,
                    )
                }
                TextButton(onClick = onLogout) {
                    Text("Chiqish", color = InglishGreen)
                }
            }
        }

        if (profileError != null) {
            item { ErrorCard(profileError, onClearProfileError) }
        }

        if (user != null && !user.emailVerified) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6DD)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Emailni tasdiqlang",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = InglishInk,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Parolni tiklash va xaridlar tarixi uchun emailingizni tasdiqlang.",
                            fontSize = 12.sp,
                            color = InglishMuted,
                        )
                        if (verifyNotice != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(verifyNotice, fontSize = 12.sp, color = InglishGreen)
                        }
                        if (verifyError != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(verifyError, fontSize = 12.sp, color = Color(0xFFB3261E))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRequestEmailVerification,
                                enabled = !verifyLoading,
                            ) {
                                Text(
                                    if (verifyLoading) "Yuborilmoqda..." else "Kod yuborish",
                                    fontSize = 12.sp,
                                )
                            }
                            TextButton(
                                onClick = {
                                    onClearVerifyMessages()
                                    showVerifyDialog = true
                                },
                            ) {
                                Text("Kodni kiritish", fontSize = 12.sp, color = InglishGreen)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = InglishGreen),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Imtihongacha", color = Color(0xFFBFD8CC), fontSize = 13.sp)
                    Text(
                        text = countdown,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("Maqsad: ")
                            append(targetBand?.toString() ?: "belgilanmagan")
                            profile?.cefrLevel?.let { append(" · $it") }
                        },
                        color = Color(0xFFE8F3ED),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                    ) {
                        Text("Maqsadni tahrirlash", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        if (needsOnboarding) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6DD)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Boshlash uchun maqsadni belgilang",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = InglishInk,
                        )
                        Text(
                            "Maqsad bandi va imtihon sanasi rejani shaxsiylashtirish uchun kerak.",
                            fontSize = 12.sp,
                            color = InglishMuted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showProfileDialog = true },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Maqsadni belgilash")
                        }
                    }
                }
            }
        }

        item {
            ProgressCard(
                progressState = progressState,
                onRetry = onRefreshProgress,
            )
        }

        item {
            UsageCard(
                usage = usageState.usage,
                loading = usageState.loading,
                error = usageState.error,
                onRetry = onRefreshUsage,
                onUpgrade = onOpenPaywall,
            )
        }

        item {
            Text(
                text = "Mashq qilish",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = InglishInk,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item {
            PracticeCard(
                title = "Darslar",
                subtitle = "Nazariya va mashqlar",
                badge = "Boshlash",
                onClick = onOpenLessons,
            )
        }
        item {
            PracticeCard(
                title = "Writing",
                subtitle = "AI baholash · 4 mezon",
                badge = "Task 1 & 2",
                onClick = onOpenWriting,
            )
        }
        item {
            PracticeCard(
                title = "Speaking",
                subtitle = "AI suhbat · fikr-mulohaza",
                badge = "Part 1–3",
                onClick = onOpenSpeaking,
            )
        }
        item {
            PracticeCard(
                title = "Lug'at",
                subtitle = "FSRS takrorlash",
                badge = "So'zlar",
                onClick = onOpenVocabulary,
            )
        }
        item {
            PracticeCard(
                title = "Battle",
                subtitle = "Bot bilan 1v1 · 20 soniya",
                badge = "O'yin",
                onClick = onOpenBattle,
            )
        }
        item {
            PracticeCard(
                title = "Placement",
                subtitle = "Darajani aniqlash · 15 daqiqa",
                badge = "Test",
                onClick = onOpenPlacement,
            )
        }
        item {
            PracticeCard(
                title = "Obuna",
                subtitle = "Pro va Teacher rejalari",
                badge = "Play Billing",
                onClick = onOpenPaywall,
            )
        }

        item {
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Hisobni o'chirish", color = Color(0xFFB3261E), fontSize = 12.sp)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showProfileDialog) {
        ProfileDialog(
            user = user,
            onDismiss = { showProfileDialog = false },
            onSave = { displayName, cefr, band, examDate ->
                onUpdateProfile(displayName, cefr, band, examDate)
                showProfileDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hisobni o'chirish") },
            text = {
                Text(
                    "Hisobingiz o'chiriladi va barcha sessiyalar yopiladi. Bu amalni bekor qilib bo'lmaydi. Davom etasizmi?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                ) {
                    Text("O'chirish", color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Bekor qilish") }
            },
        )
    }

    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = {
                showVerifyDialog = false
                verifyCode = ""
                onClearVerifyMessages()
            },
            title = { Text("Emailni tasdiqlash") },
            text = {
                Column {
                    Text(
                        "Emailingizga yuborilgan 6 xonali kodni kiriting.",
                        fontSize = 13.sp,
                        color = InglishMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = verifyCode,
                        onValueChange = { value ->
                            verifyCode = value.filter { it.isDigit() }.take(6)
                        },
                        label = { Text("6 xonali kod") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (verifyNotice != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(verifyNotice, fontSize = 12.sp, color = InglishGreen)
                    }
                    if (verifyError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(verifyError, fontSize = 12.sp, color = Color(0xFFB3261E))
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onRequestEmailVerification,
                        enabled = !verifyLoading,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (verifyLoading) "Yuborilmoqda..." else "Kodni qayta yuborish",
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmEmailVerification(verifyCode) },
                    enabled = verifyCode.length == 6 && !verifyLoading,
                ) {
                    Text("Tasdiqlash")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVerifyDialog = false
                        verifyCode = ""
                        onClearVerifyMessages()
                    },
                ) {
                    Text("Bekor qilish")
                }
            },
        )
    }
}

@Composable
private fun ProgressCard(
    progressState: ProgressUiState,
    onRetry: () -> Unit,
) {
    val progress = progressState.progress

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Taraqqiyot",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = InglishInk,
                    modifier = Modifier.weight(1f),
                )
                val streak = progress?.streakDays ?: 0
                if (streak > 0) {
                    Box(
                        modifier = Modifier.background(Color(0xFFEAF3EE), RoundedCornerShape(50)),
                    ) {
                        Text(
                            "$streak kun ketma-ket",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = InglishGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            when {
                progressState.error != null -> {
                    Spacer(Modifier.height(6.dp))
                    Text(progressState.error, fontSize = 12.sp, color = Color(0xFFB3261E))
                    TextButton(onClick = onRetry) { Text("Qayta urinish") }
                }

                progressState.loading && progress == null -> {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                progress != null -> {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BandStat("Writing", progress.writing.latestBand)
                            Spacer(Modifier.height(8.dp))
                            BandStat("Speaking", progress.speaking.latestBand)
                        }
                        val writingFraction = ((progress.writing.latestBand ?: 0.0) / 9.0).toFloat().coerceIn(0f, 1f)
                        val speakingFraction = ((progress.speaking.latestBand ?: 0.0) / 9.0).toFloat().coerceIn(0f, 1f)
                        val vocabFraction = if (progress.vocabulary.total > 0) {
                            (progress.vocabulary.reviewed.toFloat() / progress.vocabulary.total.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        ProgressRings(
                            writingProgress = writingFraction,
                            speakingProgress = speakingFraction,
                            vocabProgress = vocabFraction,
                            size = 88.dp,
                            ringStroke = 6.dp,
                            ringSpacing = 3.dp,
                        )
                    }

                    val target = progress.targetBand
                    val current = progress.currentBand
                    if (target != null && current != null && target > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Text(
                                "Hozir ${formatBand(current)}",
                                fontSize = 12.sp,
                                color = InglishInk,
                                modifier = Modifier.weight(1f),
                            )
                            Text("Maqsad ${formatBand(target)}", fontSize = 12.sp, color = InglishMuted)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (current / target).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = InglishGreen,
                            trackColor = Color(0xFFEDE7DA),
                        )
                        val gap = progress.bandGap
                        Text(
                            if (gap != null && gap > 0) {
                                "Maqsadga ${formatBand(gap)} band qoldi"
                            } else {
                                "Maqsad bandiga yetdingiz"
                            },
                            fontSize = 10.sp,
                            color = InglishMuted,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEDE7DA))
                    Spacer(Modifier.height(10.dp))

                    val exercises = progress.exercises
                    Text(
                        if (exercises.attempted > 0) {
                            "Mashqlar: ${exercises.correct}/${exercises.attempted} to'g'ri" +
                                (exercises.accuracy?.let { " · ${(it * 100).toInt()}%" } ?: "")
                        } else {
                            "Mashqlar: hali boshlanmagan"
                        },
                        fontSize = 12.sp,
                        color = InglishInk,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Lug'at: ${progress.vocabulary.reviewed}/${progress.vocabulary.total} ko'rildi" +
                            " · ${progress.vocabulary.due} ta navbatda",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )

                    if (
                        progress.writing.scoredCount == 0 &&
                        progress.speaking.scoredCount == 0 &&
                        exercises.attempted == 0
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Birinchi mashqni bajaring — taraqqiyot shu yerda ko'rinadi.",
                            fontSize = 11.sp,
                            color = InglishMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BandStat(label: String, band: Double?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = InglishMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatBand(band),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = InglishGreen,
            )
            Text(
                " band",
                fontSize = 11.sp,
                color = InglishMuted,
                modifier = Modifier.padding(bottom = 3.dp, start = 3.dp),
            )
        }
    }
}

@Composable
private fun UsageCard(
    usage: UsageDto?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onUpgrade: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Limitlar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = InglishInk,
                    modifier = Modifier.weight(1f),
                )
                usage?.let {
                    Text(
                        planLabel(it.plan),
                        fontSize = 11.sp,
                        color = InglishGreen,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error, fontSize = 12.sp, color = Color(0xFFB3261E))
                TextButton(onClick = onRetry) { Text("Qayta urinish") }
            } else if (loading && usage == null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (usage != null) {
                Spacer(Modifier.height(10.dp))
                UsageRow("Speaking", usage.speaking, "kuniga")
                Spacer(Modifier.height(10.dp))
                UsageRow("Writing", usage.writing, "oyiga")
                if (usage.plan == "FREE") {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Pro'ga o'tish")
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageRow(label: String, bucket: UsageBucketDto, period: String) {
    val progress = if (bucket.limit <= 0) 0f else {
        (bucket.used.toFloat() / bucket.limit.toFloat()).coerceIn(0f, 1f)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, color = InglishInk, modifier = Modifier.weight(1f))
            Text(
                "${bucket.used} / ${bucket.limit} $period",
                fontSize = 11.sp,
                color = InglishMuted,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (bucket.remaining == 0) Color(0xFFB3261E) else InglishGreen,
            trackColor = Color(0xFFEDE7DA),
        )
        Text(
            text = if (bucket.remaining == 0) "Limit tugagan" else "${bucket.remaining} ta qoldi",
            fontSize = 10.sp,
            color = InglishMuted,
        )
    }
}

@Composable
private fun PracticeCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFD7E8DF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.take(1),
                    fontWeight = FontWeight.Bold,
                    color = InglishGreen,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = InglishInk)
                Text(subtitle, color = InglishMuted, fontSize = 12.sp)
            }
            Text(
                text = badge,
                color = InglishGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ProfileDialog(
    user: UserDto?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, cefrLevel: String?, targetBand: Double?, examDate: String?) -> Unit,
) {
    val profile = user?.profile
    var displayName by remember { mutableStateOf(profile?.displayName.orEmpty()) }
    var cefrLevel by remember { mutableStateOf(profile?.cefrLevel) }
    var targetBand by remember { mutableStateOf(profile?.targetBand) }
    var examDate by remember { mutableStateOf(profile?.examDate.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val bands = listOf(5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0)
    val levels = listOf("A2", "B1", "B2", "C1", "C2")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maqsad va profil") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        error = null
                    },
                    label = { Text("Ismingiz") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Text("Maqsad bandi", fontSize = 12.sp, color = InglishMuted)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    bands.forEach { band ->
                        ChoiceChip(
                            label = if (band % 1.0 == 0.0) band.toInt().toString() else band.toString(),
                            selected = targetBand == band,
                            onClick = { targetBand = band },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Daraja (CEFR)", fontSize = 12.sp, color = InglishMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.forEach { level ->
                        ChoiceChip(
                            label = level,
                            selected = cefrLevel == level,
                            onClick = { cefrLevel = if (cefrLevel == level) null else level },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = examDate,
                    onValueChange = {
                        examDate = it
                        error = null
                    },
                    label = { Text("Imtihon sanasi") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Color(0xFFB3261E), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanName = displayName.trim()
                    if (cleanName.length < 2) {
                        error = "Ism kamida 2 belgidan iborat bo'lishi kerak."
                        return@TextButton
                    }
                    val parsedDate = if (examDate.isBlank()) {
                        null
                    } else {
                        runCatching { LocalDate.parse(examDate.trim()).toString() }.getOrNull()
                    }
                    if (examDate.isNotBlank() && parsedDate == null) {
                        error = "Sanani YYYY-MM-DD formatida kiriting."
                        return@TextButton
                    }
                    onSave(cleanName, cefrLevel, targetBand, parsedDate)
                },
            ) {
                Text("Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor qilish") }
        },
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    if (selected) {
        Button(onClick = onClick, shape = RoundedCornerShape(50), contentPadding = padding) {
            Text(label, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(50), contentPadding = padding) {
            Text(label, fontSize = 12.sp, color = InglishGreen)
        }
    }
}

private fun planLabel(plan: String): String = when (plan) {
    "FREE" -> "Bepul reja"
    "PRO" -> "Pro reja"
    "TEACHER" -> "Teacher reja"
    else -> plan
}
