package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class UnitsResponse(
    val units: List<ContentUnitDto> = emptyList(),
)

@Serializable
data class ContentUnitDto(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    val skill: String = "",
    val cefrLevel: String = "",
    val order: Int = 0,
    val lessonCount: Int = 0,
)

@Serializable
data class UnitDetailResponse(
    val unit: ContentUnitDetailDto,
)

@Serializable
data class ContentUnitDetailDto(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    val skill: String = "",
    val cefrLevel: String = "",
    val order: Int = 0,
    val lessons: List<ContentLessonDto> = emptyList(),
)

@Serializable
data class ContentLessonDto(
    val id: String,
    val slug: String,
    val title: String,
    val type: String = "",
    val order: Int = 0,
    val estimatedMinutes: Int = 0,
    val contentJson: LessonContentDto? = null,
    val exercises: List<ContentExerciseDto> = emptyList(),
)

@Serializable
data class LessonContentDto(
    val intro: String? = null,
    val keyTakeaway: String? = null,
    val sections: List<LessonSectionDto> = emptyList(),
    val steps: List<String> = emptyList(),
    val checklist: List<String> = emptyList(),
    val cueCard: CueCardDto? = null,
)

@Serializable
data class LessonSectionDto(
    val heading: String = "",
    val body: String = "",
)

@Serializable
data class CueCardDto(
    val topic: String = "",
    val bullets: List<String> = emptyList(),
)

@Serializable
data class ContentExerciseDto(
    val id: String,
    val type: String = "",
    val order: Int = 0,
    val prompt: String = "",
    val optionsJson: JsonElement? = null,
)

@Serializable
data class ExerciseAnswerRequest(
    val answer: JsonElement,
)

@Serializable
data class ExerciseAnswerResponse(
    val result: ExerciseAnswerResultDto,
)

@Serializable
data class ExerciseAnswerResultDto(
    val isCorrect: Boolean? = null,
    val correctAnswer: JsonElement? = null,
    val explanation: String? = null,
)

fun JsonElement?.asStringList(): List<String> {
    val array = this as? JsonArray ?: return emptyList()
    return array.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}
