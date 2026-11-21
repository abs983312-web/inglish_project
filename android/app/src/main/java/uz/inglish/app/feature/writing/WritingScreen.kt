package uz.inglish.app.feature.writing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.core.countWords
import uz.inglish.app.core.network.dto.WritingSubmissionDto
import uz.inglish.app.core.network.dto.WritingSubmissionSummaryDto
import uz.inglish.app.core.network.dto.WritingTaskDto
import uz.inglish.app.ui.components.AiFeedbackCard
import uz.inglish.app.ui.components.EmptyState
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.LoadingState
import uz.inglish.app.ui.components.SectionTitle
import uz.inglish.app.ui.components.StatusPill
import uz.inglish.app.ui.components.formatBand

@Composable
fun WritingScreen(
    state: WritingUiState,
    onRetry: () -> Unit,
    onOpenTask: (String) -> Unit,
    onCloseTask: () -> Unit,
    onUpdateDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenSubmission: (String) -> Unit,
    onCloseSubmission: () -> Unit,
    onClearError: () -> Unit,
) {
    BackHandler(enabled = state.selectedTask != null || state.submission != null) {
        if (state.selectedTask != null) onCloseTask() else onCloseSubmission()
    }

    when {
        state.loadingTask -> LoadingState("Vazifa yuklanmoqda…")
        state.selectedTask != null -> TaskEditor(
            state = state,
            onCloseTask = onCloseTask,
            onUpdateDraft = onUpdateDraft,
            onSubmit = onSubmit,
            onClearError = onClearError,
        )
        state.submission != null -> SubmissionDetail(
            submission = state.submission,
            onClose = onCloseSubmission,
        )
        else -> TaskList(
            state = state,
            onRetry = onRetry,
            onOpenTask = onOpenTask,
            onOpenSubmission = onOpenSubmission,
        )
    }
}

@Composable
private fun TaskList(
    state: WritingUiState,
    onRetry: () -> Unit,
    onOpenTask: (String) -> Unit,
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
            Text("Writing", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "IELTS insho topshiriqlari va AI fikr-mulohazasi.",
                fontSize = 13.sp,
                color = InglishMuted,
            )
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onRetry) }
        }

        if (state.loading) {
            item { LoadingState("Topshiriqlar yuklanmoqda…") }
        }

        if (!state.loading && state.tasks.isEmpty() && state.error == null) {
            item {
                EmptyState(
                    title = "Topshiriqlar topilmadi",
                    message = "Writing topshiriqlari qo'shilgach shu yerda ko'rinadi.",
                )
            }
        }

        items(state.tasks, key = { it.id }) { task ->
            TaskCard(task = task, onClick = { onOpenTask(task.slug) })
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
private fun TaskCard(task: WritingTaskDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(if (task.type == "TASK_1") "Task 1" else "Task 2")
                Spacer(Modifier.weight(1f))
                Text("${task.minWords} so'z · ${task.timeLimitMinutes} daq", fontSize = 11.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(task.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InglishInk)
            Text(
                task.prompt,
                fontSize = 12.sp,
                color = InglishMuted,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun HistoryCard(
    submission: WritingSubmissionSummaryDto,
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
                Text(submission.taskTitle, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    "${submission.wordCount} so'z · ${submission.createdAt.take(10)}",
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
private fun TaskEditor(
    state: WritingUiState,
    onCloseTask: () -> Unit,
    onUpdateDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
) {
    val task = state.selectedTask ?: return
    val words = countWords(state.draft)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TextButton(onClick = onCloseTask) { Text("← Topshiriqlar") }
            Text(task.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(if (task.type == "TASK_1") "Task 1" else "Task 2")
                Spacer(Modifier.width(8.dp))
                Text(
                    "Kamida ${task.minWords} so'z · ${task.timeLimitMinutes} daqiqa",
                    fontSize = 11.sp,
                    color = InglishMuted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
            ) {
                Text(task.prompt, modifier = Modifier.padding(14.dp), fontSize = 13.sp, color = InglishInk)
            }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }

        if (state.submission == null) {
            item {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onUpdateDraft,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Javobingiz") },
                    minLines = 10,
                    maxLines = 20,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$words so'z",
                        fontSize = 12.sp,
                        color = if (words < 20) Color(0xFFB3261E) else InglishMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "AI baholash 1–2 daqiqa olishi mumkin",
                        fontSize = 10.sp,
                        color = InglishMuted,
                    )
                }
            }

            item {
                Button(
                    onClick = onSubmit,
                    enabled = !state.submitting && words >= 20,
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
            item {
                FeedbackBlock(submission)
            }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Sizning matningiz", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(submission.text, fontSize = 12.sp, color = InglishMuted)
                    }
                }
            }
            item {
                TextButton(onClick = onCloseTask) { Text("Yangi topshiriq tanlash") }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SubmissionDetail(
    submission: WritingSubmissionDto,
    onClose: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TextButton(onClick = onClose) { Text("← Writing") }
            Text(
                submission.task?.title ?: "Writing javobi",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = InglishInk,
            )
            Text(
                "${submission.wordCount} so'z · ${submission.createdAt.take(10)}",
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
                    Text("Matn", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(submission.text, fontSize = 12.sp, color = InglishMuted)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun FeedbackBlock(submission: WritingSubmissionDto) {
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
        disclaimer = feedback.disclaimer,
    )
}

private fun statusLabel(status: String): String = when (status) {
    "SCORED" -> "Baholangan"
    "SCORING" -> "Baholanmoqda"
    "FAILED" -> "Xatolik"
    "SUBMITTED" -> "Yuborilgan"
    else -> "Qoralama"
}
