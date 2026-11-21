package uz.inglish.app.feature.speaking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import uz.inglish.app.core.voice.SpeechRecognizerController
import uz.inglish.app.core.voice.TextToSpeechController
import uz.inglish.app.core.countWords
import uz.inglish.app.core.network.dto.SpeakingPromptDto
import uz.inglish.app.core.network.dto.SpeakingSubmissionDto
import uz.inglish.app.core.network.dto.SpeakingSubmissionSummaryDto
import uz.inglish.app.core.network.dto.asSpeakingCueCard
import uz.inglish.app.core.network.dto.asSpeakingQuestions
import uz.inglish.app.ui.components.AiFeedbackCard
import uz.inglish.app.ui.components.EmptyState
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.LiveWaveform
import uz.inglish.app.ui.components.LoadingState
import uz.inglish.app.ui.components.SectionTitle
import uz.inglish.app.ui.components.StatusPill
import uz.inglish.app.ui.components.formatBand

@Composable
fun SpeakingScreen(
    state: SpeakingUiState,
    onRetry: () -> Unit,
    onSetPart: (String?) -> Unit,
    onOpenPrompt: (String) -> Unit,
    onClosePrompt: () -> Unit,
    onUpdateTranscript: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenSubmission: (String) -> Unit,
    onCloseSubmission: () -> Unit,
    onClearError: () -> Unit,
) {
    BackHandler(enabled = state.selectedPrompt != null || state.submission != null) {
        if (state.selectedPrompt != null) onClosePrompt() else onCloseSubmission()
    }

    when {
        state.loadingPrompt -> LoadingState("Savol yuklanmoqda…")
        state.selectedPrompt != null -> PromptDetail(
            state = state,
            onClosePrompt = onClosePrompt,
            onUpdateTranscript = onUpdateTranscript,
            onSubmit = onSubmit,
            onClearError = onClearError,
        )
        state.submission != null -> SubmissionDetail(
            submission = state.submission,
            onClose = onCloseSubmission,
        )
        else -> PromptList(
            state = state,
            onRetry = onRetry,
            onSetPart = onSetPart,
            onOpenPrompt = onOpenPrompt,
            onOpenSubmission = onOpenSubmission,
        )
    }
}

@Composable
private fun PromptList(
    state: SpeakingUiState,
    onRetry: () -> Unit,
    onSetPart: (String?) -> Unit,
    onOpenPrompt: (String) -> Unit,
    onOpenSubmission: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Speaking", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "Savolni tinglang, gapirib javob bering — nutqingiz transkriptga aylanadi.",
                fontSize = 13.sp,
                color = InglishMuted,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PartChip("Barchasi", state.partFilter == null) { onSetPart(null) }
                PartChip("Part 1", state.partFilter == "PART_1") { onSetPart("PART_1") }
                PartChip("Part 2", state.partFilter == "PART_2") { onSetPart("PART_2") }
                PartChip("Part 3", state.partFilter == "PART_3") { onSetPart("PART_3") }
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onRetry) }
        }

        if (state.loading) {
            item { LoadingState("Savollar yuklanmoqda…") }
        }

        if (!state.loading && state.prompts.isEmpty() && state.error == null) {
            item {
                EmptyState(
                    title = "Savollar topilmadi",
                    message = "Bu bo'lim uchun speaking savollari hali qo'shilmagan.",
                )
            }
        }

        items(state.prompts, key = { it.id }) { prompt ->
            PromptCard(prompt = prompt, onClick = { onOpenPrompt(prompt.slug) })
        }

        if (state.history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                SectionTitle("Tarix")
            }
            items(state.history, key = { it.id }) { submission ->
                HistoryCard(
                    submission = submission,
                    onClick = { onOpenSubmission(submission.id) },
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun PartChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, shape = RoundedCornerShape(50), contentPadding = chipPadding()) {
            Text(label, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(50), contentPadding = chipPadding()) {
            Text(label, fontSize = 12.sp, color = InglishGreen)
        }
    }
}

private fun chipPadding() = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)

@Composable
private fun PromptCard(prompt: SpeakingPromptDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(partLabel(prompt.part))
                Spacer(Modifier.weight(1f))
                Text("${prompt.speakTimeSeconds}s", fontSize = 11.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(prompt.topic, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InglishInk)
            val questionCount = prompt.questions.asSpeakingQuestions().size
            if (questionCount > 0) {
                Text("$questionCount savol", fontSize = 12.sp, color = InglishMuted)
            } else if (prompt.questions.asSpeakingCueCard() != null) {
                Text("Cue card · 2 daqiqa", fontSize = 12.sp, color = InglishMuted)
            }
        }
    }
}

@Composable
private fun HistoryCard(
    submission: SpeakingSubmissionSummaryDto,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(submission.topic, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    "${partLabel(submission.part)} · ${submission.createdAt.take(10)}",
                    fontSize = 11.sp,
                    color = InglishMuted,
                )
            }
            Text(
                text = formatBand(submission.bandEstimate),
                fontWeight = FontWeight.Bold,
                color = InglishGreen,
                fontSize = 15.sp,
            )
            Spacer(Modifier.width(8.dp))
            StatusPill(statusLabel(submission.status))
        }
    }
}

@Composable
private fun PromptDetail(
    state: SpeakingUiState,
    onClosePrompt: () -> Unit,
    onUpdateTranscript: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
) {
    val prompt = state.selectedPrompt ?: return
    val words = countWords(state.transcript)
    val questions = prompt.questions.asSpeakingQuestions()
    val cueCard = prompt.questions.asSpeakingCueCard()

    val context = LocalContext.current
    val speech = remember { SpeechRecognizerController(context) }
    DisposableEffect(speech) {
        onDispose { speech.destroy() }
    }
    val tts = remember { TextToSpeechController(context) }
    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    var listening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }

    val startListening = {
        voiceStatus = null
        listening = true
        speech.start(
            onPartial = { voiceStatus = it },
            onResult = { text ->
                listening = false
                voiceStatus = null
                val current = state.transcript.trim()
                onUpdateTranscript(if (current.isEmpty()) text else "$current $text")
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
            voiceStatus = "Mikrofon ruxsati berilmagan. Sozlamalardan ruxsat bering."
        }
    }

    fun toggleListening() {
        if (listening) {
            speech.stop()
            listening = false
            // Keep whatever the recognizer already heard instead of discarding it.
            val partial = voiceStatus?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                val current = state.transcript.trim()
                onUpdateTranscript(if (current.isEmpty()) partial else "$current $partial")
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

    val promptSpeech = remember(prompt) {
        buildString {
            append(prompt.topic).append(". ")
            cueCard?.bullets?.forEach { append(it).append(". ") }
            questions.forEach { append(it).append(". ") }
        }.trim()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TextButton(onClick = onClosePrompt) { Text("← Speaking") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(partLabel(prompt.part))
                Spacer(Modifier.width(8.dp))
                Text("${prompt.speakTimeSeconds} soniya", fontSize = 11.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(6.dp))
            Text(prompt.topic, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InglishInk)

            if (cueCard != null) {
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        cueCard.bullets.forEach { bullet ->
                            Text("• $bullet", fontSize = 13.sp, color = InglishInk)
                        }
                    }
                }
            }

            if (questions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                questions.forEachIndexed { index, question ->
                    Text("${index + 1}. $question", fontSize = 13.sp, color = InglishInk)
                }
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }

        if (state.submission == null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { toggleListening() },
                                enabled = !state.submitting,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (listening) Color(0xFFB3261E) else InglishGreen,
                                ),
                            ) {
                                Text(
                                    if (listening) "To'xtatish" else "Gapirib yozish",
                                    fontSize = 13.sp,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { tts.speak(promptSpeech) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("Tinglash", fontSize = 13.sp, color = InglishGreen)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (listening) {
                            LiveWaveform(
                                isRecording = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            text = voiceStatus ?: if (listening) {
                                "Tinglanmoqda… Ingliz tilida gapiring."
                            } else {
                                "Mikrofon orqali gapiring — nutqingiz transkriptga aylanadi. " +
                                    "Keyin matnni tahrirlashingiz mumkin."
                            },
                            fontSize = 11.sp,
                            color = if (voiceStatus != null && !listening) Color(0xFFB3261E) else InglishInk,
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.transcript,
                    onValueChange = onUpdateTranscript,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Javobingiz (transkript)") },
                    minLines = 8,
                    maxLines = 18,
                )
                Text(
                    text = "$words so'z",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    color = InglishMuted,
                )
            }
            item {
                Button(
                    onClick = onSubmit,
                    enabled = !state.submitting && words >= 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (state.submitting) "AI baholamoqda…" else "AI baholashni olish")
                }
            }
        }

        state.submission?.let { submission ->
            item {
                Spacer(Modifier.height(6.dp))
                SectionTitle("Natija")
            }
            item { FeedbackBlock(submission) }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Transkript", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(submission.transcript, fontSize = 12.sp, color = InglishMuted)
                    }
                }
            }
            item { TextButton(onClick = onClosePrompt) { Text("Yangi savol tanlash") } }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SubmissionDetail(
    submission: SpeakingSubmissionDto,
    onClose: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TextButton(onClick = onClose) { Text("← Speaking") }
            Text(
                submission.prompt?.topic ?: "Speaking javobi",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = InglishInk,
            )
            Text(
                "${submission.createdAt.take(10)}",
                fontSize = 12.sp,
                color = InglishMuted,
            )
            Spacer(Modifier.height(6.dp))
        }
        item { FeedbackBlock(submission) }
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Transkript", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(submission.transcript, fontSize = 12.sp, color = InglishMuted)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun FeedbackBlock(submission: SpeakingSubmissionDto) {
    val feedback = submission.feedback
    if (feedback == null) {
        EmptyState(
            title = "Baholash tugallanmagan",
            message = when (submission.status) {
                "SCORING" -> "AI hali baholamoqda. Bir ozdan keyin qayta oching."
                "FAILED" -> "Baholashda xatolik yuz berdi. Qayta yuborib ko'ring."
                else -> "Bu javob hali baholanmagan."
            },
        )
        return
    }

    AiFeedbackCard(
        band = submission.bandEstimate ?: feedback.overallBand,
        confidenceLow = submission.confidenceLow ?: feedback.confidenceLow,
        confidenceHigh = submission.confidenceHigh ?: feedback.confidenceHigh,
        criteria = feedback.criteria,
        strengths = feedback.strengths,
        improvements = feedback.improvements,
        corrections = feedback.corrections,
        nextStep = feedback.nextStep,
        pronunciationNote = feedback.pronunciationNote,
        disclaimer = feedback.disclaimer,
    )
}

private fun partLabel(part: String): String = when (part) {
    "PART_1" -> "Part 1"
    "PART_2" -> "Part 2"
    "PART_3" -> "Part 3"
    else -> part
}

private fun statusLabel(status: String): String = when (status) {
    "SCORED" -> "Baholangan"
    "SCORING" -> "Baholanmoqda"
    "FAILED" -> "Xatolik"
    "SUBMITTED" -> "Yuborilgan"
    else -> "Qoralama"
}
