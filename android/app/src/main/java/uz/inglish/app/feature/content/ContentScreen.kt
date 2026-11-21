package uz.inglish.app.feature.content

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import uz.inglish.app.core.network.dto.ContentExerciseDto
import uz.inglish.app.core.network.dto.ContentLessonDto
import uz.inglish.app.core.network.dto.ContentUnitDto
import uz.inglish.app.core.network.dto.ExerciseAnswerResultDto
import uz.inglish.app.core.network.dto.asStringList
import uz.inglish.app.ui.components.EmptyState
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.LoadingState
import uz.inglish.app.ui.components.SectionTitle
import uz.inglish.app.ui.components.StatusPill

@Composable
fun ContentScreen(
    state: ContentUiState,
    onRetry: () -> Unit,
    onOpenUnit: (String) -> Unit,
    onCloseUnit: () -> Unit,
    onCheckAnswer: (String, JsonElement) -> Unit,
    onClearError: () -> Unit,
) {
    BackHandler(enabled = state.selectedUnit != null) { onCloseUnit() }

    if (state.selectedUnit != null) {
        UnitDetail(
            state = state,
            onCloseUnit = onCloseUnit,
            onCheckAnswer = onCheckAnswer,
            onClearError = onClearError,
        )
    } else {
        UnitList(state = state, onRetry = onRetry, onOpenUnit = onOpenUnit)
    }
}

@Composable
private fun UnitList(
    state: ContentUiState,
    onRetry: () -> Unit,
    onOpenUnit: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Darslar", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "Nazariya va mashqlar bilan bosqichma-bosqich o'rganing.",
                fontSize = 13.sp,
                color = InglishMuted,
            )
            Spacer(Modifier.height(6.dp))
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onRetry) }
        }

        if (state.loading) {
            item { LoadingState("Darslar yuklanmoqda…") }
        }

        if (!state.loading && state.units.isEmpty() && state.error == null) {
            item {
                EmptyState(
                    title = "Hozircha darslar yo'q",
                    message = "Kontent qo'shilgach, bu yerda IELTS bo'limlari paydo bo'ladi.",
                )
            }
        }

        items(state.units, key = { it.id }) { unit ->
            UnitCard(unit = unit, onClick = { onOpenUnit(unit.slug) })
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun UnitCard(unit: ContentUnitDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFD7E8DF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = unit.skill.take(1),
                        fontWeight = FontWeight.Bold,
                        color = InglishGreen,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(unit.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        text = "${unit.cefrLevel} · ${unit.lessonCount} dars",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                }
                StatusPill(skillLabel(unit.skill))
            }
            if (!unit.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(unit.description, fontSize = 12.sp, color = InglishMuted)
            }
        }
    }
}

@Composable
private fun UnitDetail(
    state: ContentUiState,
    onCloseUnit: () -> Unit,
    onCheckAnswer: (String, JsonElement) -> Unit,
    onClearError: () -> Unit,
) {
    val unit = state.selectedUnit ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onCloseUnit) { Text("← Darslar") }
            Text(unit.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            if (!unit.description.isNullOrBlank()) {
                Text(unit.description, fontSize = 13.sp, color = InglishMuted)
            }
            Spacer(Modifier.height(4.dp))
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }

        if (state.loadingUnit) {
            item { LoadingState("Dars yuklanmoqda…") }
        }

        items(unit.lessons, key = { it.id }) { lesson ->
            LessonCard(
                lesson = lesson,
                answers = state.answers,
                checkingId = state.checkingId,
                onCheckAnswer = onCheckAnswer,
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun LessonCard(
    lesson: ContentLessonDto,
    answers: Map<String, ExerciseAnswerResultDto>,
    checkingId: String?,
    onCheckAnswer: (String, JsonElement) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(lesson.title, Modifier.weight(1f))
                StatusPill(
                    text = if (lesson.type == "PRACTICE") "Amaliyot" else "Nazariya",
                    color = if (lesson.type == "PRACTICE") Color(0xFF9A6B00) else InglishGreen,
                )
            }
            Text(
                text = "${lesson.estimatedMinutes} daqiqa",
                fontSize = 11.sp,
                color = InglishMuted,
            )

            val content = lesson.contentJson
            if (content != null) {
                if (!content.intro.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(content.intro, fontSize = 13.sp, color = InglishInk)
                }

                content.sections.forEach { section ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        section.heading,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = InglishGreen,
                    )
                    Text(section.body, fontSize = 13.sp, color = InglishMuted)
                }

                if (content.steps.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Qadamlar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishGreen)
                    content.steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", fontSize = 13.sp, color = InglishMuted)
                    }
                }

                if (content.checklist.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Tekshiruv ro'yxati", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishGreen)
                    content.checklist.forEach { item ->
                        Text("☐ $item", fontSize = 13.sp, color = InglishMuted)
                    }
                }

                content.cueCard?.let { cue ->
                    Spacer(Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(cue.topic, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishGreen)
                            cue.bullets.forEach { bullet ->
                                Text("• $bullet", fontSize = 12.sp, color = InglishInk)
                            }
                        }
                    }
                }

                if (!content.keyTakeaway.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Asosiy fikr: ${content.keyTakeaway}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = InglishGreen,
                    )
                }
            }

            if (lesson.exercises.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Mashqlar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InglishInk)
            }

            lesson.exercises.forEach { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    result = answers[exercise.id],
                    checking = checkingId == exercise.id,
                    onCheck = { answer -> onCheckAnswer(exercise.id, answer) },
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ContentExerciseDto,
    result: ExerciseAnswerResultDto?,
    checking: Boolean,
    onCheck: (JsonElement) -> Unit,
) {
    val options = exercise.optionsJson.asStringList()
    var text by rememberSaveable(exercise.id) { mutableStateOf("") }
    val correctIndex = (result?.correctAnswer as? JsonPrimitive)
        ?.content
        ?.toIntOrNull()

    Column(Modifier.padding(top = 12.dp)) {
        Text(exercise.prompt, fontSize = 13.sp, color = InglishInk)

        if (options.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { index, option ->
                val isCorrectOption = result != null && correctIndex == index
                Card(
                    onClick = { onCheck(JsonPrimitive(index)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCorrectOption -> Color(0xFFDDF1E5)
                            else -> Color(0xFFF7F3EA)
                        },
                    ),
                ) {
                    Text(
                        text = "${'A' + index}. $option",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = InglishInk,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Javobingiz") },
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onCheck(JsonPrimitive(text.trim())) },
                enabled = text.isNotBlank() && !checking,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (checking) "Tekshirilmoqda…" else "Tekshirish")
            }
        }

        result?.let { checked ->
            Spacer(Modifier.height(8.dp))
            when (checked.isCorrect) {
                true -> Text("✓ To'g'ri!", color = InglishGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                false -> Text("✗ Noto'g'ri. Yana urinib ko'ring.", color = Color(0xFFB3261E), fontSize = 13.sp)
                null -> Text(
                    "O'zingizni tekshiring — bu javob avtomatik baholanmaydi.",
                    color = InglishMuted,
                    fontSize = 12.sp,
                )
            }

            val answerText = when (val correct = checked.correctAnswer) {
                null -> null
                is JsonPrimitive -> correct.contentOrNull
                else -> correct.toString()
            }
            if (answerText != null && checked.isCorrect != true) {
                val shown = if (correctIndex != null) {
                    options.getOrNull(correctIndex) ?: answerText
                } else {
                    answerText
                }
                Text("Namuna javob: $shown", fontSize = 12.sp, color = InglishGreen)
            }

            if (!checked.explanation.isNullOrBlank()) {
                Text(checked.explanation, fontSize = 12.sp, color = InglishMuted)
            }
        }
    }
}

private fun skillLabel(skill: String): String = when (skill) {
    "WRITING" -> "Writing"
    "SPEAKING" -> "Speaking"
    "LISTENING" -> "Listening"
    "READING" -> "Reading"
    "VOCABULARY" -> "Lug'at"
    "GRAMMAR" -> "Grammatika"
    else -> skill.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
