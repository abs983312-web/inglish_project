package uz.inglish.app.feature.battle

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.core.network.dto.BattlePackDto
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGold
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.LoadingState
import uz.inglish.app.ui.components.SectionTitle
import uz.inglish.app.ui.components.StatusPill

private val PackBorder = Color(0xFFE6E0D4)
private val PackSoftGreen = Color(0xFFE3F3E9)
private val PackWrong = Color(0xFFB3261E)

@Composable
fun BattlePacksScreen(
    state: BattlePacksUiState,
    vm: BattlePacksViewModel,
    onBack: () -> Unit,
) {
    BackHandler {
        if (state.screen == PacksScreen.LIST) onBack() else vm.backToList()
    }

    when (state.screen) {
        PacksScreen.LIST -> PackListScreen(state = state, vm = vm, onBack = onBack)
        PacksScreen.CREATE -> PackCreateScreen(state = state, vm = vm)
        PacksScreen.DETAIL -> PackDetailScreen(state = state, vm = vm)
    }
}

@Composable
private fun PackListScreen(
    state: BattlePacksUiState,
    vm: BattlePacksViewModel,
    onBack: () -> Unit,
) {
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
                        "Video darslar",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = InglishInk,
                    )
                    Text(
                        "O‘z darsingizni yarating — Kahoot kabi o‘ynang.",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                }
                TextButton(onClick = onBack) { Text("Orqaga") }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = InglishGreen),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Video + savollar = Battle Pack",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "YouTube havolasini qo‘shing, 1–50 savol yozing va 6 xonali kod bilan ulashing. Hammasi bepul.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = vm::openCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(50),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = InglishGreen,
                        ),
                    ) {
                        Text("Yangi pack yaratish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.search,
                onValueChange = vm::setSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Qidirish") },
                placeholder = { Text("IELTS, grammar, vocabulary…") },
                singleLine = true,
                trailingIcon = {
                    TextButton(onClick = vm::applySearch) { Text("Topish") }
                },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "popular" to "Mashhur",
                    "new" to "Yangi",
                    "mine" to "Mening",
                ).forEach { (scope, label) ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick = { vm.setScope(scope) },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (state.loading) {
            item { LoadingState("Packlar yuklanmoqda…") }
        }

        if (!state.loading && state.items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Hali pack yo‘q", fontWeight = FontWeight.Bold, color = InglishInk)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Birinchi video darsingizni yaratib, do‘stlaringizni bellashtiring.",
                            fontSize = 12.sp,
                            color = InglishMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        items(state.items, key = { it.id }) { pack ->
            PackCard(
                pack = pack,
                onOpen = { vm.openDetail(pack) },
                onPlay = { vm.play(pack) },
                playing = state.playing,
            )
        }

        if (state.error != null) {
            item { ErrorCard(state.error, vm::clearMessages) }
        }
        if (state.notice != null) {
            item { PackNotice(state.notice) }
        }

        item {
            Text(
                "Pack yaratish: kuniga 5 ta. Video YouTube’da ochiladi, savollar serverda saqlanadi.",
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
private fun PackCard(
    pack: BattlePackDto,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    playing: Boolean,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PackBorder),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(InglishGreen.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "YouTube video dars",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${pack.questionCount} savol · ${pack.playCount} marta o‘ynalgan",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Column(Modifier.padding(16.dp)) {
                Text(
                    pack.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = InglishInk,
                )
                if (!pack.description.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        pack.description,
                        fontSize = 12.sp,
                        color = InglishMuted,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pack.creatorName,
                        fontSize = 11.sp,
                        color = InglishMuted,
                    )
                    if (pack.isOwner) {
                        Spacer(Modifier.width(6.dp))
                        StatusPill("Sizniki", color = InglishGreen)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        pack.tags.take(2).joinToString(" · "),
                        fontSize = 10.sp,
                        color = InglishMuted,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPlay,
                    enabled = !playing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    if (playing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("O‘ynashni boshlash", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PackCreateScreen(
    state: BattlePacksUiState,
    vm: BattlePacksViewModel,
) {
    val draft = state.activeDraft
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
                        if (state.editingId == null) "Yangi pack" else "Packni tahrirlash",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = InglishInk,
                    )
                    Text(
                        "1. Video  ·  2. Savollar  ·  3. Ulashish",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                }
                TextButton(onClick = vm::backToList) { Text("Bekor") }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Video dars")
                    OutlinedTextField(
                        value = state.videoUrl,
                        onValueChange = vm::setVideoUrl,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("YouTube havolasi") },
                        placeholder = { Text("https://youtu.be/…") },
                        singleLine = true,
                    )
                    Text(
                        "Havola YouTube’da ochiladi. Video 15 daqiqagacha bo‘lsa yaxshi.",
                        fontSize = 10.sp,
                        color = InglishMuted,
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Ma’lumot")
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = vm::setTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sarlavha") },
                        placeholder = { Text("IELTS Writing Task 2: Linking words") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = vm::setDescription,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tavsif (ixtiyoriy)") },
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = state.tags,
                        onValueChange = vm::setTags,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teglar (vergul bilan)") },
                        placeholder = { Text("IELTS, grammar, writing") },
                        singleLine = true,
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Savollar (${state.questions.size})")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::addQuestion) { Text("+ Savol") }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.questions) { index, _ ->
                    FilterChip(
                        selected = state.activeQuestion == index,
                        onClick = { vm.selectQuestion(index) },
                        label = { Text("${index + 1}") },
                    )
                }
            }
        }

        if (draft != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("${state.activeQuestion + 1}-savol")
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = { vm.removeQuestion(state.activeQuestion) },
                            ) { Text("O‘chirish", color = PackWrong) }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = draft.type == "MCQ",
                                onClick = { vm.setQuestionType("MCQ") },
                                label = { Text("Variantli") },
                            )
                            FilterChip(
                                selected = draft.type == "TRUE_FALSE",
                                onClick = { vm.setQuestionType("TRUE_FALSE") },
                                label = { Text("True / False") },
                            )
                        }

                        OutlinedTextField(
                            value = draft.prompt,
                            onValueChange = vm::setQuestionPrompt,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Savol matni") },
                            minLines = 2,
                        )

                        Text(
                            "Variantlar — to‘g‘ri javobni belgilang",
                            fontSize = 11.sp,
                            color = InglishMuted,
                        )

                        val options = if (draft.isTrueFalse) listOf("True", "False") else draft.options
                        options.forEachIndexed { index, option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = draft.correctIndex == index,
                                    onClick = { vm.setCorrect(index) },
                                )
                                if (draft.isTrueFalse) {
                                    Text(
                                        option,
                                        fontSize = 14.sp,
                                        color = InglishInk,
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = option,
                                        onValueChange = { vm.setOption(index, it) },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("${('A' + index)} varianti") },
                                        singleLine = true,
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = draft.explanation,
                            onValueChange = vm::setExplanation,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Izoh (ixtiyoriy)") },
                            minLines = 2,
                        )
                    }
                }
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, vm::clearMessages) }
        }
        if (state.notice != null) {
            item { PackNotice(state.notice) }
        }

        item {
            Button(
                onClick = vm::save,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (state.editingId == null) "Packni yaratish" else "Saqlash",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun PackDetailScreen(
    state: BattlePacksUiState,
    vm: BattlePacksViewModel,
) {
    val pack = state.selected ?: return
    val context = LocalContext.current
    var confirmDelete by remember(pack.id) { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Packni o‘chirasizmi?") },
            text = { Text("Bu amalni qaytarib bo‘lmaydi. O‘ynalgan natijalar saqlanadi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.delete(pack)
                    },
                ) { Text("O‘chirish", color = PackWrong) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Bekor") }
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
                TextButton(onClick = vm::backToList) { Text("Orqaga") }
                Spacer(Modifier.weight(1f))
                if (pack.isOwner) {
                    TextButton(onClick = vm::startEdit) { Text("Tahrirlash") }
                }
            }
        }

        item {
            Text(pack.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Spacer(Modifier.height(4.dp))
            Text(
                "${pack.creatorName} · ${pack.questionCount} savol · ${pack.playCount} o‘ynalgan",
                fontSize = 12.sp,
                color = InglishMuted,
            )
            if (!pack.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(pack.description, fontSize = 13.sp, color = InglishInk)
            }
            if (pack.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pack.tags.forEach { tag -> StatusPill("#$tag", color = InglishGreen) }
                }
            }
        }

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
                        pack.videoTitle ?: "YouTube video",
                        fontSize = 13.sp,
                        color = InglishInk,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val url = pack.videoUrl.ifBlank { "https://youtu.be/${pack.videoId}" }
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                    ) { Text("Videoni YouTube’da ochish") }
                }
            }
        }

        item {
            Button(
                onClick = { vm.play(pack) },
                enabled = !state.playing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
            ) {
                if (state.playing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text("Xona yaratib o‘ynash", fontWeight = FontWeight.SemiBold)
            }
        }

        if (pack.isOwner) {
            item {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.deleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, PackWrong),
                ) { Text("Packni o‘chirish", color = PackWrong) }
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, vm::clearMessages) }
        }
        if (state.notice != null) {
            item { PackNotice(state.notice) }
        }

        item {
            SectionTitle("Savollar")
            Spacer(Modifier.height(8.dp))
        }

        if (pack.questions.isEmpty()) {
            item {
                Text(
                    "Savollar ro‘yxati yuklanmoqda yoki pack egasi savollarni yashirgan.",
                    fontSize = 12.sp,
                    color = InglishMuted,
                )
            }
        }

        items(pack.questions, key = { it.id ?: it.position.toString() }) { question ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PackBorder),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill("${question.position}", color = InglishGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (question.type == "TRUE_FALSE") "True / False" else "Variantli",
                            fontSize = 10.sp,
                            color = InglishMuted,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(question.prompt, fontSize = 14.sp, color = InglishInk)
                    Spacer(Modifier.height(6.dp))
                    question.options.forEachIndexed { index, option ->
                        val isCorrect = question.correctIndex == index
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(
                                        if (isCorrect) PackSoftGreen else Color(0xFFF4F1EA),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    ('A' + index).toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) InglishGreen else InglishMuted,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                option,
                                fontSize = 12.sp,
                                color = if (isCorrect) InglishGreen else InglishMuted,
                                fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                    if (!question.explanation.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Izoh: ${question.explanation}",
                            fontSize = 11.sp,
                            color = InglishMuted,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PackNotice(text: String) {
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
