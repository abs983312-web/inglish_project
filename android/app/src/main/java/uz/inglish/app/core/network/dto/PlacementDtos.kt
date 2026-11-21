package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PlacementSessionResponse(
    val session: PlacementSessionDto,
)

@Serializable
data class PlacementLatestResponse(
    val session: PlacementSessionDto? = null,
)

@Serializable
data class PlacementSubmitRequest(
    val answers: List<PlacementAnswerRequest>,
    val writingText: String,
    val speakingTranscript: String,
)

@Serializable
data class PlacementAnswerRequest(
    val questionId: String,
    val answerIndex: Int,
)

@Serializable
data class PlacementSubmitResponse(
    val session: PlacementSessionDto,
)

@Serializable
data class PlacementSessionDto(
    val id: String,
    val status: String = "ACTIVE",
    val startedAt: String = "",
    val finishedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val questions: List<PlacementQuestionDto> = emptyList(),
    val writing: PlacementWritingDto? = null,
    val speaking: PlacementSpeakingDto? = null,
    val result: PlacementResultDto? = null,
)

@Serializable
data class PlacementWritingDto(
    val slug: String = "",
    val title: String = "",
    val prompt: String = "",
    val minWords: Int = 60,
    val timeLimitMinutes: Int = 5,
    val text: String? = null,
    val band: Double? = null,
    val feedback: JsonElement? = null,
)

@Serializable
data class PlacementSpeakingDto(
    val slug: String = "",
    val part: String = "PART_1",
    val topic: String = "",
    val questions: List<String> = emptyList(),
    val prepTimeSeconds: Int = 0,
    val speakTimeSeconds: Int = 60,
    val transcript: String? = null,
    val band: Double? = null,
    val feedback: JsonElement? = null,
)

@Serializable
data class PlacementQuestionDto(
    val id: String,
    val section: String = "",
    val prompt: String = "",
    val passage: String? = null,
    val options: List<String> = emptyList(),
)

@Serializable
data class PlacementResultDto(
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val percentage: Double = 0.0,
    val cefrLevel: String = "",
    val estimatedBand: Double = 0.0,
    val weakSkill: String = "",
    val sectionStats: Map<String, PlacementSectionStatDto> = emptyMap(),
    val answers: JsonElement? = null,
)

@Serializable
data class PlacementSectionStatDto(
    val correct: Int = 0,
    val total: Int = 0,
)
