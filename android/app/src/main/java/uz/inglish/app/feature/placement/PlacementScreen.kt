package uz.inglish.app.feature.placement

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import uz.inglish.app.core.countWords
import uz.inglish.app.core.voice.SpeechRecognizerController
import uz.inglish.app.core.network.dto.PlacementQuestionDto
import uz.inglish.app.core.network.dto.PlacementResultDto
import uz.inglish.app.core.network.dto.PlacementSectionStatDto
import uz.inglish.app.core.network.dto.PlacementSessionDto
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.SectionTitle
import uz.inglish.app.ui.components.StatusPill
import uz.inglish.app.ui.components.formatBand

private val PlacementBorder = Color(0xFFE6E0D4)

private fun sectionLabel(section: String): String = when (section) {
    "GRAMMAR" -> "Grammatika"
    "VOCABULARY" -> "Lug'at"
    "READING" -> "O'qish"
    else -> section
}

@Composable
fun PlacementScreen(
    state: PlacementUiState,
    onStart: () -> Unit,
    onSelect: (String, Int) -> Unit,
    onUpdateWriting: (String) -> Unit,
    onUpdateSpeakingTranscript: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onClearError: () -> Unit,
) {
    BackHandler { onClose() }

    val session = state.session

    when {
        session == null -> PlacementIntro(
            state = state,
            onStart = onStart,
            onClose = onClose,
            onClearError = onClearError,
        )

        state.finished -> PlacementResultScreen(
            session = session,
            error = state.error,
            onStart = onStart,
            onClose = onClose,
            onClearError = onClearError,
        )

        else -> PlacementTest(
            state = state,
            session = session,
            onSelect = onSelect,
            onUpdateWriting = onUpdateWriting,
            onUpdateSpeakingTranscript = onUpdateSpeakingTranscript,
            onSubmit = onSubmit,
            onClose = onClose,
            onClearError = onClearError,
        )
    }
}

@Composable
private fun PlacementIntro(
    state: PlacementUiState,
    onStart: () -> Unit,
    onClose: () -> Unit,
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
            Text("Placement", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "Darajangizni aniqlash uchun qisqa test.",
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
                    Text("15 daqiqa · 25 savol", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Grammatika, lug'at va o'qish bo'yicha savollar. Javob kaliti serverda tekshiriladi.",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IntroStat("10", "Grammatika")
                        IntroStat("10", "Lug'at")
                        IntroStat("5", "O'qish")
                    }
                }
            }
        }

        state.latest?.result?.let { latestResult ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle("Oxirgi natija")
                        Spacer(Modifier.height(8.dp))
                        ResultSummaryRow("CEFR darajasi", latestResult.cefrLevel)
                        ResultSummaryRow("Taxminiy band", latestResult.estimatedBand.toString())
                        ResultSummaryRow("To'g'ri javoblar", "${latestResult.correctCount}/${latestResult.totalCount}")
                        ResultSummaryRow("Eng zaif ko'nikma", sectionLabel(latestResult.weakSkill))
                    }
                }
            }
        }

        if (state.error != null) {
            val error = state.error
            item { ErrorCard(error, onClearError) }
        }

        item {
            Button(
                onClick = onStart,
                enabled = !state.creating && !state.loadingLatest,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
            ) {
                if (state.creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Test yaratilmoqda…")
                } else {
                    Text("Placementni boshlash", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text("Bosh sahifaga")
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun IntroStat(value: String, label: String) {
    Column(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
    }
}

@Composable
private fun PlacementTest(
    state: PlacementUiState,
    session: PlacementSessionDto,
    onSelect: (String, Int) -> Unit,
    onUpdateWriting: (String) -> Unit,
    onUpdateSpeakingTranscript: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onClearError: () -> Unit,
) {
    val total = session.questions.size
    val answered = state.answeredCount
    val progress = if (total == 0) 0f else answered.toFloat() / total.toFloat()
    val error = state.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Placement", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InglishInk)
                Text("$answered/$total javob berildi", fontSize = 12.sp, color = InglishMuted)
            }
            OutlinedButton(onClick = onClose) { Text("Yopish") }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = InglishGreen,
            trackColor = PlacementBorder,
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(session.questions) { index, question ->
                QuestionCard(
                    number = index + 1,
                    question = question,
                    selectedIndex = state.answers[question.id],
                    onSelect = onSelect,
                )
            }

            item {
                session.writing?.let { writing ->
                    WritingTaskCard(
                        title = writing.title,
                        prompt = writing.prompt,
                        minWords = writing.minWords,
                        text = state.writingText,
                        onTextChange = onUpdateWriting,
                    )
                }
            }

            item {
                session.speaking?.let { speaking ->
                    SpeakingTaskCard(
                        topic = speaking.topic,
                        questions = speaking.questions,
                        speakTimeSeconds = speaking.speakTimeSeconds,
                        transcript = state.speakingTranscript,
                        onTranscriptChange = onUpdateSpeakingTranscript,
                    )
                }
            }

            if (error != null) {
                item { ErrorCard(error, onClearError) }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit && !state.submitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(50),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(Modifier.width(10.dp))
                Text("Baholanmoqda…")
            } else {
                Text("Natijani ko'rish", fontWeight = FontWeight.SemiBold)
            }
        }
        if (!state.canSubmit) {
            val writingWords = countWords(state.writingText)
            val speakingWords = countWords(state.speakingTranscript)
            val missing = buildList {
                if (total - answered > 0) add("${total - answered} ta savol")
                if (writingWords < (session.writing?.minWords ?: 60)) add("yozuv (${writingWords}/60 so'z)")
                if (speakingWords < 20) add("gapirish (${speakingWords}/20 so'z)")
            }
            Text(
                "Tugallanmadi: ${missing.joinToString(", ")}.",
                fontSize = 11.sp,
                color = InglishMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun WritingTaskCard(
    title: String,
    prompt: String,
    minWords: Int,
    text: String,
    onTextChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (countWords(text) >= minWords) BorderStroke(1.5.dp, InglishGreen) else null,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill("Yozuv")
                Spacer(Modifier.width(8.dp))
                Text("${countWords(text)}/$minWords so'z", fontSize = 11.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InglishInk)
            Text(prompt, fontSize = 12.sp, color = InglishMuted)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                label = { Text("Paragrafingiz") },
                placeholder = { Text("Ingliz tilida yozing…") },
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
private fun SpeakingTaskCard(
    topic: String,
    questions: List<String>,
    speakTimeSeconds: Int,
    transcript: String,
    onTranscriptChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val speech = remember { SpeechRecognizerController(context) }
    DisposableEffect(speech) {
        onDispose { speech.destroy() }
    }

    var listening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }

    val startListening = {
        voiceStatus = null
        listening = true
        speech.start(
            onPartial = { voiceStatus = it },
            onResult = { recognized ->
                listening = false
                voiceStatus = null
                val current = transcript.trim()
                onTranscriptChange(if (current.isEmpty()) recognized else "$current $recognized")
            },
            onError = { message ->
                listening = false
                voiceStatus = message
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            listening = false
            voiceStatus = "Mikrofon ruxsati berilmagan."
        }
    }

    fun toggleListening() {
        if (listening) {
            speech.stop()
            listening = false
            val partial = voiceStatus?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                val current = transcript.trim()
                onTranscriptChange(if (current.isEmpty()) partial else "$current $partial")
            }
            voiceStatus = null
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (countWords(transcript) >= 20) BorderStroke(1.5.dp, InglishGreen) else null,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill("Gapirish")
                Spacer(Modifier.width(8.dp))
                Text("$speakTimeSeconds soniya · ${countWords(transcript)}/20 so'z", fontSize = 11.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(topic, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InglishInk)
            questions.forEach { question ->
                Text("• $question", fontSize = 12.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = transcript,
                onValueChange = onTranscriptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp),
                label = { Text("Gapirish transkripti") },
                placeholder = { Text("Mikrofon bilan gapiring yoki yozing…") },
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { toggleListening() }) {
                Text(if (listening) "To'xtatish" else "Mikrofon bilan gapirish")
            }
            voiceStatus?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, fontSize = 11.sp, color = InglishMuted)
            }
        }
    }
}

@Composable
private fun QuestionCard(
    number: Int,
    question: PlacementQuestionDto,
    selectedIndex: Int?,
    onSelect: (String, Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (selectedIndex != null) BorderStroke(1.5.dp, InglishGreen) else null,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(sectionLabel(question.section))
                Spacer(Modifier.weight(1f))
                Text("$number.", fontSize = 12.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(10.dp))
            Text(question.prompt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InglishInk)

            question.passage?.let { passage ->
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F3E8), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(passage, fontSize = 12.sp, color = InglishMuted)
                }
            }

            Spacer(Modifier.height(10.dp))
            question.options.forEachIndexed { index, option ->
                OptionRow(
                    index = index,
                    text = option,
                    selected = selectedIndex == index,
                    onClick = { onSelect(question.id, index) },
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    index: Int,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFEAF3EE) else Color.White,
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) InglishGreen else PlacementBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (selected) InglishGreen else Color(0xFFD7E8DF),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ('A' + index).toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else InglishGreen,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 13.sp, color = InglishInk, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlacementResultScreen(
    session: PlacementSessionDto,
    error: String?,
    onStart: () -> Unit,
    onClose: () -> Unit,
    onClearError: () -> Unit,
) {
    val result = session.result

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Natija", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "Natija practice estimate hisoblanadi, rasmiy IELTS balli emas.",
                fontSize = 12.sp,
                color = InglishMuted,
            )
        }

        if (result != null) {
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
                        Text(result.cefrLevel, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Taxminiy band: ${formatBand(result.estimatedBand)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${result.correctCount}/${result.totalCount} · ${result.percentage}%",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
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
                    Column(Modifier.padding(18.dp)) {
                        SectionTitle("Bo'limlar bo'yicha")
                        Spacer(Modifier.height(8.dp))
                        SectionStatRow("GRAMMAR", result.sectionStats["GRAMMAR"])
                        SectionStatRow("VOCABULARY", result.sectionStats["VOCABULARY"])
                        SectionStatRow("READING", result.sectionStats["READING"])
                        session.writing?.band?.let { band ->
                            ResultSummaryRow("Yozuv bandi", formatBand(band))
                        }
                        session.speaking?.band?.let { band ->
                            ResultSummaryRow("Gapirish bandi", formatBand(band))
                        }
                        Spacer(Modifier.height(8.dp))
                        ResultSummaryRow("Eng zaif ko'nikma", sectionLabel(result.weakSkill))
                    }
                }
            }
        }

        if (error != null) {
            item { ErrorCard(error, onClearError) }
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text("Qayta topshirish", fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text("Bosh sahifaga")
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SectionStatRow(section: String, stat: PlacementSectionStatDto?) {
    val correct = stat?.correct ?: 0
    val total = stat?.total ?: 0
    ResultSummaryRow(sectionLabel(section), if (total == 0) "—" else "$correct/$total")
}

@Composable
private fun ResultSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = InglishMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InglishInk)
    }
}
