package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BattleRoomResponse(
    val room: BattleRoomDto,
    val participantId: String? = null,
)

@Serializable
data class CreateBattleRoomRequest(
    val mode: String = "BOT",
    val questionCount: Int = 10,
    val timeLimitMs: Int = 20_000,
    val autoBot: Boolean = true,
)

@Serializable
data class JoinBattleRoomRequest(
    val code: String,
    val nickname: String? = null,
)

@Serializable
data class BattleNicknameRequest(
    val nickname: String,
)

@Serializable
data class BattleReadyRequest(
    val ready: Boolean,
)

@Serializable
data class BattleAnswerRequest(
    val roundId: String,
    val answerIndex: Int,
)

@Serializable
data class BattleAnswerResponse(
    val room: BattleRoomDto,
    val participantId: String? = null,
    val result: BattleAnswerResultDto? = null,
)

@Serializable
data class BattleRoomDto(
    val id: String,
    val code: String? = null,
    val mode: String = "BOT",
    val status: String = "LOBBY",
    val phase: String = "LOBBY",
    val questionCount: Int = 10,
    val currentPosition: Int = 1,
    val timeLimitMs: Int = 20_000,
    val revealMs: Int = 4_000,
    val maxPlayers: Int = 10,
    val hostParticipantId: String? = null,
    val serverNow: Long = 0L,
    val startedAt: String? = null,
    val pack: BattlePackRefDto? = null,
    val scores: List<BattleScoreDto> = emptyList(),
    val currentQuestion: BattleQuestionDto? = null,
    val reveal: BattleRevealDto? = null,
    val finished: Boolean = false,
    val results: List<BattleScoreDto>? = null,
)

@Serializable
data class BattleQuestionDto(
    val id: String,
    val position: Int = 1,
    val source: String = "",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val endsAt: String? = null,
)

@Serializable
data class BattlePackRefDto(
    val id: String,
    val title: String = "",
    val videoId: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
)

@Serializable
data class BattleScoreDto(
    val id: String,
    val name: String = "",
    val isBot: Boolean = false,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val isOnline: Boolean = false,
    val score: Int = 0,
    val correctCount: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val answeredCurrent: Boolean = false,
    val rank: Int? = null,
)

@Serializable
data class BattleRevealDto(
    val roundId: String,
    val position: Int = 1,
    val correctIndex: Int = 0,
    val explanation: String? = null,
    val endsAt: String? = null,
    val distribution: List<Int> = emptyList(),
    val answers: List<BattleRevealAnswerDto> = emptyList(),
)

@Serializable
data class BattleRevealAnswerDto(
    val participantId: String,
    val answerIndex: Int = -1,
    val isCorrect: Boolean = false,
    val responseMs: Int = 0,
    val scoreGained: Int = 0,
)

@Serializable
data class BattleAnswerResultDto(
    val roundId: String,
    val isCorrect: Boolean,
    val correctIndex: Int = 0,
    val explanation: String? = null,
    val scoreGained: Int = 0,
    val responseMs: Int = 0,
    val answerIndex: Int = -1,
)

@Serializable
data class BattleSocketMessage(
    val type: String = "",
    val room: BattleRoomDto? = null,
    val code: String? = null,
    val message: String? = null,
)
