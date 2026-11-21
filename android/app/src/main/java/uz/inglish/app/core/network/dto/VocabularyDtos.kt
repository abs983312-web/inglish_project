package uz.inglish.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VocabularyListResponse(
    val items: List<VocabularyItemDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class VocabularySetsResponse(
    val sets: List<VocabularySetDto> = emptyList(),
)

@Serializable
data class VocabularySetDto(
    val slug: String,
    val title: String,
    val description: String = "",
    val tag: String,
    val count: Int = 0,
)

@Serializable
data class VocabularyItemDto(
    val id: String,
    val word: String,
    val pos: String? = null,
    val cefr: String? = null,
    val definitionUz: String = "",
    val definitionEn: String? = null,
    val exampleEn: String? = null,
    val exampleUz: String? = null,
    val audioUrl: String? = null,
    val tags: List<String> = emptyList(),
    val state: VocabularyStateDto = VocabularyStateDto(),
)

@Serializable
data class VocabularyStateDto(
    val state: String = "NEW",
    val dueAt: String = "",
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
)

@Serializable
data class VocabularyStatsResponse(
    val stats: VocabularyStatsDto = VocabularyStatsDto(),
)

@Serializable
data class VocabularyStatsDto(
    val total: Int = 0,
    @SerialName("new") val newCount: Int = 0,
    val due: Int = 0,
    val learning: Int = 0,
    val review: Int = 0,
)

@Serializable
data class VocabularyReviewRequest(
    val rating: Int,
)

@Serializable
data class VocabularyReviewResponse(
    val review: VocabularyReviewDto,
    val item: VocabularyReviewItemDto,
)

@Serializable
data class VocabularyReviewDto(
    val vocabularyId: String = "",
    val state: String = "NEW",
    val dueAt: String = "",
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val intervalDays: Double = 0.0,
)

@Serializable
data class VocabularyReviewItemDto(
    val id: String = "",
    val word: String = "",
    val definitionUz: String = "",
)
