package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BattlePackQuestionDto(
    val id: String? = null,
    val position: Int = 1,
    val type: String = "MCQ",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int? = null,
    val explanation: String? = null,
    val timeLimitMs: Int = 20_000,
)

@Serializable
data class BattlePackDto(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val videoId: String = "",
    val videoUrl: String = "",
    val videoTitle: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Int? = null,
    val tags: List<String> = emptyList(),
    val questionCount: Int = 0,
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val isPublic: Boolean = true,
    val isOwner: Boolean = false,
    val creatorName: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val questions: List<BattlePackQuestionDto> = emptyList(),
)

@Serializable
data class BattlePackListDto(
    val items: List<BattlePackDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
)

@Serializable
data class BattlePackEnvelope(
    val pack: BattlePackDto,
)

@Serializable
data class BattlePackQuestionInput(
    val type: String = "MCQ",
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String? = null,
    val timeLimitMs: Int? = null,
)

@Serializable
data class CreateBattlePackRequest(
    val title: String,
    val description: String? = null,
    val videoUrl: String,
    val tags: List<String> = emptyList(),
    val questions: List<BattlePackQuestionInput>,
)

@Serializable
data class PlayBattlePackRequest(
    val mode: String = "FRIEND",
    val shuffle: Boolean = false,
)

@Serializable
data class DeleteBattlePackResponse(
    val deleted: Boolean = false,
)
