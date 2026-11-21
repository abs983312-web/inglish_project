package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WritingTasksResponse(
    val tasks: List<WritingTaskDto> = emptyList(),
)

@Serializable
data class WritingTaskResponse(
    val task: WritingTaskDetailDto,
)

@Serializable
data class WritingSubmissionResponse(
    val submission: WritingSubmissionDto,
)

@Serializable
data class WritingSubmissionsResponse(
    val submissions: List<WritingSubmissionSummaryDto> = emptyList(),
)

@Serializable
data class WritingTaskDto(
    val id: String,
    val slug: String,
    val type: String = "",
    val title: String = "",
    val prompt: String = "",
    val imageUrl: String? = null,
    val minWords: Int = 0,
    val timeLimitMinutes: Int = 0,
)

@Serializable
data class WritingTaskDetailDto(
    val id: String,
    val slug: String,
    val type: String = "",
    val title: String = "",
    val prompt: String = "",
    val imageUrl: String? = null,
    val minWords: Int = 0,
    val timeLimitMinutes: Int = 0,
    val sampleAnswer: String? = null,
    val bandDescriptors: JsonElement? = null,
)

@Serializable
data class CreateWritingSubmissionRequest(
    val taskSlug: String,
    val text: String,
)

@Serializable
data class WritingSubmissionDto(
    val id: String,
    val taskId: String = "",
    val task: WritingTaskDto? = null,
    val text: String = "",
    val wordCount: Int = 0,
    val status: String = "DRAFT",
    val bandEstimate: Double? = null,
    val confidenceLow: Double? = null,
    val confidenceHigh: Double? = null,
    val feedback: WritingFeedbackDto? = null,
    val modelVersion: String? = null,
    val submittedAt: String? = null,
    val scoredAt: String? = null,
    val createdAt: String = "",
)

@Serializable
data class WritingSubmissionSummaryDto(
    val id: String,
    val taskId: String = "",
    val taskTitle: String = "",
    val taskSlug: String = "",
    val taskType: String = "",
    val wordCount: Int = 0,
    val status: String = "DRAFT",
    val bandEstimate: Double? = null,
    val createdAt: String = "",
    val scoredAt: String? = null,
)

@Serializable
data class WritingFeedbackDto(
    val overallBand: Double = 0.0,
    val confidenceLow: Double = 0.0,
    val confidenceHigh: Double = 0.0,
    val criteria: List<CriterionDto> = emptyList(),
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val corrections: List<CorrectionDto> = emptyList(),
    val nextStep: String = "",
    val disclaimer: String = "",
)

@Serializable
data class CriterionDto(
    val name: String = "",
    val band: Double? = null,
    val comment: String = "",
)

@Serializable
data class CorrectionDto(
    val original: String = "",
    val corrected: String = "",
    val explanation: String = "",
)
