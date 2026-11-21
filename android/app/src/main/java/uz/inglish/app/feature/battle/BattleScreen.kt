package uz.inglish.app.feature.battle

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.inglish.app.core.network.dto.BattleAnswerResultDto
import uz.inglish.app.core.network.dto.BattleQuestionDto
import uz.inglish.app.core.network.dto.BattleRevealAnswerDto
import uz.inglish.app.core.network.dto.BattleRevealDto
import uz.inglish.app.core.network.dto.BattleRoomDto
import uz.inglish.app.core.network.dto.BattleScoreDto
import uz.inglish.app.ui.components.CountdownArc
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGold
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.KahootOptionButton
import uz.inglish.app.ui.components.LoadingState
import uz.inglish.app.ui.components.SectionTitle
import uz.inglish.app.ui.components.StatusPill
import java.time.Instant

private val BattleCorrect = Color(0xFF2E7D5B)
private val BattleWrong = Color(0xFFB3261E)
private val BattleCorrectSurface = Color(0xFFE3F3E9)
private val BattleWrongSurface = Color(0xFFFFF1EE)
private val BattleBorder = Color(0xFFE6E0D4)
private val BattleSoftGreen = Color(0xFFD7E8DF)

@Composable
fun BattleScreen(
    state: BattleUiState,
    onCreateBot: () -> Unit,
    onCreateFriend: () -> Unit,
    onCreateClassroom: () -> Unit,
    onOpenPacks: () -> Unit,
    onJoinCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSetNickname: (String) -> Unit,
    onSetQuestionCount: (Int) -> Unit,
    onSetTimeLimit: (Int) -> Unit,
    onReady: (Boolean) -> Unit,
    onStart: () -> Unit,
    onAddBot: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onPlayAgain: () -> Unit,
    onLeave: () -> Unit,
    onClearError: () -> Unit,
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    val room = state.room
    BackHandler(enabled = room != null) { showLeaveDialog = true }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("O‘yindan chiqasizmi?") },
            text = {
                Text(
                    if (room?.status == "ACTIVE") {
                        "O‘yin davom etadi, lekin sizning javoblaringiz hisoblanmaydi."
                    } else {
                        "Xonadan chiqasizmi?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onLeave()
                    },
                ) { Text("Chiqish", color = BattleWrong) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Qolish") }
            },
        )
    }

    when (state.stage) {
        BattleStage.HOME -> BattleHome(
            state = state,
            onCreateBot = onCreateBot,
            onCreateFriend = onCreateFriend,
            onCreateClassroom = onCreateClassroom,
            onOpenPacks = onOpenPacks,
            onJoinCode = onJoinCode,
            onCodeChange = onCodeChange,
            onNicknameChange = onNicknameChange,
            onSetQuestionCount = onSetQuestionCount,
            onSetTimeLimit = onSetTimeLimit,
            onClearError = onClearError,
        )

        BattleStage.LOBBY -> BattleLobby(
            state = state,
            onReady = onReady,
            onStart = onStart,
            onAddBot = onAddBot,
            onSetNickname = onSetNickname,
            onRequestLeave = { showLeaveDialog = true },
            onClearError = onClearError,
        )

        BattleStage.GAME -> BattleGame(
            state = state,
            onSelectOption = onSelectOption,
            onSubmit = onSubmit,
            onTimeout = onTimeout,
            onRequestLeave = { showLeaveDialog = true },
            onClearError = onClearError,
        )

        BattleStage.RESULTS -> BattleResults(
            state = state,
            onPlayAgain = onPlayAgain,
            onRequestLeave = { showLeaveDialog = true },
            onClearError = onClearError,
        )
    }
}

@Composable
private fun BattleHome(
    state: BattleUiState,
    onCreateBot: () -> Unit,
    onCreateFriend: () -> Unit,
    onCreateClassroom: () -> Unit,
    onOpenPacks: () -> Unit,
    onJoinCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSetQuestionCount: (Int) -> Unit,
    onSetTimeLimit: (Int) -> Unit,
    onClearError: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Battle", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "Bot, do‘stlar yoki butun sinf bilan bellashing.",
                fontSize = 13.sp,
                color = InglishMuted,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = InglishGreen),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Bot bilan 1v1",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Darhol boshlanadi · serverda hisoblanadi",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LobbyStat("500", "to‘g‘ri javob", Modifier.weight(1f))
                        LobbyStat("+500", "tezlik bonusi", Modifier.weight(1f))
                        LobbyStat("+100", "seriya", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onCreateBot,
                        enabled = !state.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(50),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = InglishGreen,
                        ),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = InglishGreen,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text("Botga qarshi o‘ynash", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("O‘yin sozlamalari")
                    Spacer(Modifier.height(10.dp))
                    Text("Savollar soni", fontSize = 12.sp, color = InglishMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 20).forEach { count ->
                            FilterChip(
                                selected = state.questionCount == count,
                                onClick = { onSetQuestionCount(count) },
                                label = { Text("$count savol") },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Har bir savolga vaqt", fontSize = 12.sp, color = InglishMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10_000, 20_000, 30_000).forEach { limit ->
                            FilterChip(
                                selected = state.timeLimitMs == limit,
                                onClick = { onSetTimeLimit(limit) },
                                label = { Text("${limit / 1000} soniya") },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("Do‘stlar bilan")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Xona yarating, 6 xonali kodni ulashing va birga o‘ynang. Kamida 2 o‘yinchi.",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onCreateFriend,
                        enabled = !state.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text("Do‘stlar xonasini yaratish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("Sinf bilan")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "O‘qituvchi xonasi: 30 o‘yinchigacha. Kodni doskada ko‘rsating.",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCreateClassroom,
                        enabled = !state.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text("Sinf xonasini yaratish")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("Video darslar · Kahoot uslubi")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "O‘z YouTube darsingizni qo‘shing, savollar tuzing va 6 xonali kod bilan do‘stlaringizni bellashtiring. Bepul.",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onOpenPacks,
                        enabled = !state.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text("Video darslar bo‘limi", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("Kod bilan qo‘shilish")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.codeInput,
                        onValueChange = onCodeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("6 xonali kod") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = onNicknameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Taxallus (ixtiyoriy)") },
                        placeholder = { Text("SwiftTiger42") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onJoinCode,
                        enabled = !state.loading && state.codeInput.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text("Xonaga qo‘shilish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }
        if (state.notice != null) {
            item { NoticeCard(state.notice) }
        }

        item {
            Text(
                "Javoblar serverda tekshiriladi. Har bir savol uchun vaqt serverda hisoblanadi.",
                fontSize = 11.sp,
                color = InglishMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun BattleLobby(
    state: BattleUiState,
    onReady: (Boolean) -> Unit,
    onStart: () -> Unit,
    onAddBot: () -> Unit,
    onSetNickname: (String) -> Unit,
    onRequestLeave: () -> Unit,
    onClearError: () -> Unit,
) {
    val room = state.room ?: return
    val me = room.scores.firstOrNull { it.id == state.participantId }
    val isHost = state.participantId == room.hostParticipantId
    val nonHostHumans = room.scores.filter { !it.isBot && !it.isHost }
    val isPackRoom = room.pack != null
    val canStart = if (isPackRoom) {
        room.scores.isNotEmpty()
    } else {
        room.scores.size >= 2 && nonHostHumans.all { it.isReady }
    }
    val context = LocalContext.current
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameDraft by remember(me?.name) { mutableStateOf(me?.name.orEmpty()) }

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Taxallusni o‘zgartirish") },
            text = {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { nicknameDraft = it.take(20) },
                    label = { Text("Taxallus") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNicknameDialog = false
                        onSetNickname(nicknameDraft)
                    },
                ) { Text("Saqlash") }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text("Bekor") }
            },
        )
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
                        room.pack?.title
                            ?: if (room.mode == "CLASSROOM") "Sinf xonasi" else "Do‘stlar xonasi",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = InglishInk,
                    )
                    Text(
                        "${room.scores.size}/${room.maxPlayers} o‘yinchi · ${room.questionCount} savol",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                }
                TextButton(onClick = { showNicknameDialog = true }) {
                    Text("Taxallus")
                }
                TextButton(onClick = onRequestLeave) { Text("Chiqish", color = BattleWrong) }
            }
        }

        if (!state.connected) {
            item { ConnectionBanner("Ulanmoqda…") }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = InglishGreen),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Xona kodi",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        room.code ?: "—",
                        color = InglishGold,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Inglish Battle — kod: ${room.code}\n" +
                                        "inglish://battle/${room.code}",
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(share, "Xona kodini ulashish"),
                            )
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    ) {
                        Text("Kodni ulashish", color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("O‘yinchilar")
                    Spacer(Modifier.height(8.dp))
                    room.scores.forEach { player ->
                        PlayerRow(
                            player = player,
                            isSelf = player.id == state.participantId,
                        )
                    }
                }
            }
        }

        if (room.pack != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionTitle("Video dars")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            room.pack.title,
                            fontSize = 13.sp,
                            color = InglishInk,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(room.pack.videoUrl),
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                        ) { Text("Videoni YouTube’da ochish") }
                    }
                }
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }
        if (state.notice != null) {
            item { NoticeCard(state.notice) }
        }

        if (isHost) {
            item {
                OutlinedButton(
                    onClick = onAddBot,
                    enabled = !state.loading && room.scores.size < room.maxPlayers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(50),
                ) { Text("Bot qo‘shish") }
            }
            item {
                Button(
                    onClick = onStart,
                    enabled = !state.loading && canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        if (isPackRoom) "Darsni boshlash" else "O‘yinni boshlash",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (!canStart) {
                item {
                    Text(
                        if (isPackRoom) {
                            "Xona tayyorlanmoqda…"
                        } else if (room.scores.size < 2) {
                            "Boshlash uchun kamida 2 o‘yinchi kerak."
                        } else {
                            "Barcha o‘yinchilar tayyor bo‘lishini kuting."
                        },
                        fontSize = 12.sp,
                        color = InglishMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            item {
                Button(
                    onClick = { onReady(!(me?.isReady ?: false)) },
                    enabled = !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        if (me?.isReady == true) "Tayyor emasman" else "Tayyorman",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item {
            Text(
                "Xona kodi bilan do‘stlaringizni taklif qiling. Do‘stlar xonasida 15 soniyadan keyin bo‘sh joyga bot qo‘shiladi.",
                fontSize = 11.sp,
                color = InglishMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun BattleGame(
    state: BattleUiState,
    onSelectOption: (Int) -> Unit,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onRequestLeave: () -> Unit,
    onClearError: () -> Unit,
) {
    val room = state.room ?: return
    val question = room.currentQuestion
    if (question == null) {
        LoadingState("Savol yuklanmoqda…")
        return
    }

    val reveal = room.reveal
    val me = room.scores.firstOrNull { it.id == state.participantId }
    val myReveal = reveal?.answers?.firstOrNull { it.participantId == state.participantId }
    val answered = state.answeredRoundId == question.id
    val phase = room.phase
    val timeLimit = room.timeLimitMs.coerceAtLeast(1)
    val endsAtMs = remember(question.id, question.endsAt) {
        parseInstant(question.endsAt)
            ?: (System.currentTimeMillis() + timeLimit)
    }

    var remainingMs by remember(question.id, phase) {
        mutableStateOf((endsAtMs - (System.currentTimeMillis() + state.serverOffsetMs)).coerceAtLeast(0))
    }

    LaunchedEffect(question.id, phase, state.serverOffsetMs, endsAtMs) {
        if (phase != "QUESTION") return@LaunchedEffect
        while (true) {
            val left = (endsAtMs - (System.currentTimeMillis() + state.serverOffsetMs))
                .coerceAtLeast(0)
            remainingMs = left
            if (left <= 0L) {
                onTimeout()
                break
            }
            delay(100)
        }
    }

    var revealRemainingMs by remember(reveal?.roundId) { mutableStateOf(0L) }
    LaunchedEffect(reveal?.roundId, reveal?.endsAt, state.serverOffsetMs) {
        val revealEndsAt = parseInstant(reveal?.endsAt) ?: return@LaunchedEffect
        while (true) {
            val left = (revealEndsAt - (System.currentTimeMillis() + state.serverOffsetMs))
                .coerceAtLeast(0)
            revealRemainingMs = left
            if (left <= 0L) break
            delay(200)
        }
    }

    val secondsLeft = ((remainingMs + 999) / 1000).toInt()
    val revealSeconds = ((revealRemainingMs + 999) / 1000).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Savol ${question.position}/${room.questionCount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InglishInk,
                )
                Text(
                    if (state.connected) "Onlayn" else "Qayta ulanmoqda…",
                    fontSize = 10.sp,
                    color = if (state.connected) BattleCorrect else BattleWrong,
                )
            }
            if (phase == "QUESTION") {
                KahootTimer(
                    remainingMs = remainingMs,
                    timeLimitMs = timeLimit,
                    secondsLeft = secondsLeft,
                )
                Spacer(Modifier.width(8.dp))
            }
            TextButton(onClick = onRequestLeave) { Text("Chiqish", color = BattleWrong) }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = {
                if (phase == "QUESTION") {
                    (remainingMs.toFloat() / timeLimit.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (secondsLeft <= 5 && phase == "QUESTION") BattleWrong else InglishGreen,
            trackColor = BattleBorder,
        )
        Spacer(Modifier.height(10.dp))

        LeaderboardStrip(room = room, selfId = state.participantId)
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { QuestionCard(question) }

            itemsIndexed(question.options) { index, option ->
                OptionCard(
                    index = index,
                    text = option,
                    selected = state.selectedIndex == index,
                    reveal = reveal,
                    selfAnswerIndex = myReveal?.answerIndex,
                    enabled = phase == "QUESTION" && !answered && !state.answering,
                    onClick = { onSelectOption(index) },
                )
            }

            if (answered && reveal == null) {
                item {
                    WaitingCard(
                        answered = room.scores.count { it.answeredCurrent },
                        total = room.scores.size,
                    )
                }
            }

            if (reveal != null) {
                item {
                    FeedbackCard(
                        reveal = reveal,
                        question = question,
                        myAnswer = myReveal,
                        revealSeconds = revealSeconds,
                    )
                }
            }

            if (state.notice != null) {
                item { NoticeCard(state.notice) }
            }
            if (state.error != null) {
                item { ErrorCard(state.error, onClearError) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSubmit,
            enabled = when {
                reveal != null -> false
                answered -> false
                state.answering -> false
                else -> state.selectedIndex != null
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(50),
        ) {
            when {
                state.answering -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Yuborilmoqda…")
                }
                reveal != null -> Text(
                    if (revealSeconds > 0) "Keyingi savol ${revealSeconds}s…" else "Keyingi savol…",
                    fontWeight = FontWeight.SemiBold,
                )
                answered -> Text("Javob qabul qilindi", fontWeight = FontWeight.SemiBold)
                else -> Text("Javob berish", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LeaderboardStrip(room: BattleRoomDto, selfId: String?) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(room.scores.take(6)) { player ->
            val isSelf = player.id == selfId
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelf) BattleSoftGreen else Color.White,
                ),
                border = if (isSelf) BorderStroke(2.dp, InglishGreen) else null,
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "#${player.rank ?: "—"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = InglishMuted,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isSelf) "Siz" else player.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InglishInk,
                            maxLines = 1,
                        )
                    }
                    Text(
                        "${player.score}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InglishGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(question: BattleQuestionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(18.dp)) {
            StatusPill(
                text = when (question.source) {
                    "VOCABULARY" -> "Lug‘at"
                    "PACK" -> "Video dars"
                    else -> "Mashq"
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                question.prompt,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = InglishInk,
            )
        }
    }
}

@Composable
private fun KahootTimer(
    remainingMs: Long,
    timeLimitMs: Int,
    secondsLeft: Int,
) {
    val totalSec = (timeLimitMs / 1000).coerceAtLeast(1).toFloat()
    val remainingSec = (remainingMs.toFloat() / 1000f).coerceIn(0f, totalSec)
    CountdownArc(
        remainingSeconds = remainingSec,
        totalSeconds = totalSec,
        size = 54.dp,
        strokeWidth = 6.dp,
    )
}

@Composable
private fun OptionCard(
    index: Int,
    text: String,
    selected: Boolean,
    reveal: BattleRevealDto?,
    selfAnswerIndex: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isCorrectOption = reveal != null && index == reveal.correctIndex
    val isWrongSelection = reveal != null && index == selfAnswerIndex && !isCorrectOption

    KahootOptionButton(
        index = index,
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        isCorrectReveal = if (reveal != null) isCorrectOption else null,
        isWrongReveal = isWrongSelection,
        enabled = enabled,
    )
}

@Composable
private fun WaitingCard(answered: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BattleSoftGreen),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Javob qabul qilindi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InglishGreen,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Boshqa o‘yinchilar kutilmoqda ($answered/$total).",
                fontSize = 12.sp,
                color = InglishMuted,
            )
        }
    }
}

@Composable
private fun RevealDistribution(
    distribution: List<Int>,
    options: List<String>,
    correctIndex: Int,
) {
    val total = distribution.sum().coerceAtLeast(1)
    options.forEachIndexed { index, option ->
        val count = distribution.getOrNull(index) ?: 0
        val fraction = count.toFloat() / total.toFloat()
        val color = if (index == correctIndex) BattleCorrect else InglishMuted
        Column(Modifier.padding(vertical = 3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${('A' + index)}. $option",
                    fontSize = 11.sp,
                    color = InglishInk,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(BattleBorder, RoundedCornerShape(50)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(7.dp)
                        .background(color, RoundedCornerShape(50)),
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    reveal: BattleRevealDto,
    question: BattleQuestionDto,
    myAnswer: BattleRevealAnswerDto?,
    revealSeconds: Int,
) {
    val timedOut = myAnswer == null || myAnswer.answerIndex < 0
    val correct = myAnswer?.isCorrect == true
    val correctText = question.options.getOrNull(reveal.correctIndex) ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (correct) BattleCorrectSurface else BattleWrongSurface,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        correct -> "To‘g‘ri!"
                        timedOut -> "Vaqt tugadi"
                        else -> "Noto‘g‘ri"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (correct) BattleCorrect else BattleWrong,
                )
                Spacer(Modifier.weight(1f))
                if ((myAnswer?.scoreGained ?: 0) > 0) {
                    Text(
                        "+${myAnswer?.scoreGained} ball",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InglishGreen,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "To‘g‘ri javob: $correctText",
                fontSize = 13.sp,
                color = InglishInk,
            )

            val distribution = if (reveal.distribution.isNotEmpty()) {
                reveal.distribution
            } else {
                val counts = MutableList(question.options.size) { 0 }
                reveal.answers.forEach { answer ->
                    if (answer.answerIndex in counts.indices) {
                        counts[answer.answerIndex] = counts[answer.answerIndex] + 1
                    }
                }
                counts
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Javoblar taqsimoti",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = InglishMuted,
            )
            Spacer(Modifier.height(6.dp))
            RevealDistribution(
                distribution = distribution,
                options = question.options,
                correctIndex = reveal.correctIndex,
            )

            if (!reveal.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(reveal.explanation, fontSize = 12.sp, color = InglishMuted)
            }

            if (
                myAnswer != null &&
                !timedOut &&
                myAnswer.answerIndex != reveal.correctIndex
            ) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sizning javobingiz: ${question.options.getOrNull(myAnswer.answerIndex) ?: "—"}",
                    fontSize = 12.sp,
                    color = InglishMuted,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (revealSeconds > 0) {
                    "Keyingi savol ${revealSeconds} soniyadan keyin…"
                } else {
                    "Keyingi savol yuklanmoqda…"
                },
                fontSize = 11.sp,
                color = InglishMuted,
            )
        }
    }
}

@Composable
private fun BattleResults(
    state: BattleUiState,
    onPlayAgain: () -> Unit,
    onRequestLeave: () -> Unit,
    onClearError: () -> Unit,
) {
    val room = state.room ?: return
    val results = room.results ?: room.scores
    val me = results.firstOrNull { it.id == state.participantId }
    val isHost = state.participantId == room.hostParticipantId
    val headline = when (me?.rank) {
        1 -> "Siz yutdingiz!"
        2, 3 -> "Podiumdasiz!"
        else -> "O‘yin tugadi"
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
                    Text("Natija", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
                    Text(
                        "${room.questionCount} savollik battle yakunlandi.",
                        fontSize = 13.sp,
                        color = InglishMuted,
                    )
                }
                TextButton(onClick = onRequestLeave) { Text("Chiqish", color = BattleWrong) }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = InglishGreen),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        headline,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${me?.score ?: 0}",
                        color = InglishGold,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (me?.rank != null) "#${me.rank} o‘rin · ${me.correctCount}/${room.questionCount} to‘g‘ri" else "",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        item {
            PodiumCard(results = results, selfId = state.participantId)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionTitle("Yakuniy jadval")
                    Spacer(Modifier.height(8.dp))
                    results.forEach { player ->
                        ResultPlayerRow(
                            player = player,
                            isSelf = player.id == state.participantId,
                        )
                    }
                }
            }
        }

        if (state.notice != null) {
            item { NoticeCard(state.notice) }
        }
        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }

        item {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    if (room.mode == "BOT" || isHost) "Yana o‘ynash" else "Yangi o‘yin",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onRequestLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
            ) { Text("Lobbyga qaytish") }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun PodiumCard(results: List<BattleScoreDto>, selfId: String?) {
    if (results.isEmpty()) return
    val top = results.take(3)
    val displayOrder = buildList {
        top.getOrNull(1)?.let { add(it) }
        top.getOrNull(0)?.let { add(it) }
        top.getOrNull(2)?.let { add(it) }
    }
    val medals = mapOf(1 to "🥇", 2 to "🥈", 3 to "🥉")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F4D3D)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Podium",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                displayOrder.forEach { player ->
                    val place = top.indexOf(player) + 1
                    val height = when (place) {
                        1 -> 112.dp
                        2 -> 92.dp
                        else -> 76.dp
                    }
                    val container = when (place) {
                        1 -> InglishGold
                        2 -> Color(0xFFD9D9D9)
                        else -> Color(0xFFE8B88A)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(medals[place] ?: "🎖", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (player.id == selfId) "Siz" else player.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            "${player.score}",
                            color = InglishGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height)
                                .background(container, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$place",
                                color = Color(0xFF1F2A24),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPlayerRow(player: BattleScoreDto, isSelf: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    when (player.rank) {
                        1 -> InglishGold.copy(alpha = 0.22f)
                        2 -> Color(0xFFE8E8E8)
                        3 -> Color(0xFFF3E0D0)
                        else -> BattleSoftGreen
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                player.rank?.toString() ?: "—",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = InglishInk,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isSelf) "${player.name} (siz)" else player.name,
                fontSize = 13.sp,
                fontWeight = if (isSelf) FontWeight.Bold else FontWeight.SemiBold,
                color = InglishInk,
                maxLines = 1,
            )
            Text(
                "${player.correctCount} to‘g‘ri · seriya ${player.bestStreak}",
                fontSize = 10.sp,
                color = InglishMuted,
            )
        }
        Text(
            "${player.score}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = InglishGreen,
        )
    }
}

@Composable
private fun PlayerRow(player: BattleScoreDto, isSelf: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(BattleSoftGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                player.name.take(1).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = InglishGreen,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    player.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelf) FontWeight.Bold else FontWeight.SemiBold,
                    color = InglishInk,
                    maxLines = 1,
                )
                if (isSelf) {
                    Spacer(Modifier.width(4.dp))
                    Text("(siz)", fontSize = 10.sp, color = InglishMuted)
                }
                if (player.isHost) {
                    Spacer(Modifier.width(4.dp))
                    Text("👑", fontSize = 11.sp)
                }
                if (player.isBot) {
                    Spacer(Modifier.width(4.dp))
                    StatusPill("Bot", color = InglishMuted)
                }
            }
            Text(
                when {
                    player.isBot -> "AI raqib"
                    player.isOnline -> "Onlayn"
                    else -> "Offline"
                },
                fontSize = 10.sp,
                color = if (player.isBot || player.isOnline) BattleCorrect else InglishMuted,
            )
        }
        StatusPill(
            text = if (player.isReady) "Tayyor" else "Kutilyapti",
            color = if (player.isReady) BattleCorrect else InglishMuted,
        )
    }
}

@Composable
private fun ConnectionBanner(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E5)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            fontSize = 12.sp,
            color = Color(0xFF8A5A00),
        )
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            fontSize = 12.sp,
            color = Color(0xFF2B4A8B),
        )
    }
}

@Composable
private fun LobbyStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(value, color = InglishGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
    }
}

private fun parseInstant(value: String?): Long? =
    value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
