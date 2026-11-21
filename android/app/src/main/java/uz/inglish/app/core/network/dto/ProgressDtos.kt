package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProgressResponse(
    val progress: ProgressDto,
)

@Serializable
data class ProgressDto(
    val examDate: String? = null,
    val daysToExam: Int? = null,
    val targetBand: Double? = null,
    val currentBand: Double? = null,
    val bandGap: Double? = null,
    val streakDays: Int = 0,
    val writing: BandHistoryDto = BandHistoryDto(),
    val speaking: BandHistoryDto = BandHistoryDto(),
    val vocabulary: VocabularyProgressDto = VocabularyProgressDto(),
    val exercises: ExerciseProgressDto = ExerciseProgressDto(),
)

@Serializable
data class BandHistoryDto(
    val scoredCount: Int = 0,
    val latestBand: Double? = null,
    val bestBand: Double? = null,
    val recent: List<BandPointDto> = emptyList(),
)

@Serializable
data class BandPointDto(
    val id: String,
    val band: Double,
    val scoredAt: String,
)

@Serializable
data class VocabularyProgressDto(
    val total: Int = 0,
    val reviewed: Int = 0,
    val due: Int = 0,
    val learned: Int = 0,
)

@Serializable
data class ExerciseProgressDto(
    val attempted: Int = 0,
    val correct: Int = 0,
    val accuracy: Double? = null,
)
