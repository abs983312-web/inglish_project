package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class SpeakingPromptsResponse(
    val prompts: List<SpeakingPromptDto> = emptyList(),
)

@Serializable
data class SpeakingPromptResponse(
    val prompt: SpeakingPromptDto,
)

@Serializable
data class SpeakingSubmissionResponse(
    val submission: SpeakingSubmissionDto,
)

@Serializable
data class SpeakingSubmissionsResponse(
    val submissions: List<SpeakingSubmissionSummaryDto> = emptyList(),
)

@Serializable
data class SpeakingPromptDto(
    val id: String,
    val slug: String,
    val part: String = "",
    val topic: String = "",
    val questions: JsonElement? = null,
    val prepTimeSeconds: Int = 0,
    val speakTimeSeconds: Int = 0,
)

@Serializable
data class CreateSpeakingSubmissionRequest(
    val promptSlug: String,
    val transcript: String,
    val durationSeconds: Int? = null,
)

@Serializable
data class SpeakingSubmissionDto(
    val id: String,
    val promptId: String = "",
    val prompt: SpeakingPromptDto? = null,
    val transcript: String = "",
    val durationSeconds: Int? = null,
    val status: String = "DRAFT",
    val bandEstimate: Double? = null,
    val confidenceLow: Double? = null,
    val confidenceHigh: Double? = null,
    val feedback: SpeakingFeedbackDto? = null,
    val modelVersion: String? = null,
    val submittedAt: String? = null,
    val scoredAt: String? = null,
    val createdAt: String = "",
)

@Serializable
data class SpeakingSubmissionSummaryDto(
    val id: String,
    val promptId: String = "",
    val topic: String = "",
    val promptSlug: String = "",
    val part: String = "",
    val durationSeconds: Int? = null,
    val status: String = "DRAFT",
    val bandEstimate: Double? = null,
    val createdAt: String = "",
    val scoredAt: String? = null,
)

@Serializable
data class SpeakingFeedbackDto(
    val overallBand: Double = 0.0,
    val confidenceLow: Double = 0.0,
    val confidenceHigh: Double = 0.0,
    val criteria: List<CriterionDto> = emptyList(),
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val corrections: List<CorrectionDto> = emptyList(),
    val nextStep: String = "",
    val pronunciationNote: String = "",
    val disclaimer: String = "",
)

/** Part 1/3 prompts store a `questions` array inside the JSON column. */
fun JsonElement?.asSpeakingQuestions(): List<String> {
    val obj = this as? JsonObject ?: return emptyList()
    val array = obj["questions"] as? JsonArray ?: return emptyList()
    return array.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/** Part 2 prompts store a cue card inside the JSON column. */
fun JsonElement?.asSpeakingCueCard(): CueCardDto? {
    val obj = this as? JsonObject ?: return null
    val cue = obj["cueCard"] as? JsonObject ?: return null
    val topic = (cue["topic"] as? JsonPrimitive)?.contentOrNull ?: ""
    val bullets = (cue["bullets"] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        .orEmpty()
    return CueCardDto(topic = topic, bullets = bullets)
}
