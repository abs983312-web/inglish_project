package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import uz.inglish.app.core.network.dto.BattleAnswerRequest
import uz.inglish.app.core.network.dto.BattleAnswerResponse
import uz.inglish.app.core.network.dto.BattleReadyRequest
import uz.inglish.app.core.network.dto.BattleRoomDto
import uz.inglish.app.core.network.dto.BattleRoomResponse
import uz.inglish.app.core.network.dto.CreateBattleRoomRequest
import uz.inglish.app.core.network.dto.JoinBattleRoomRequest

class BattleApi(private val api: ApiClient) {
    suspend fun createRoom(
        mode: String = "BOT",
        questionCount: Int = 10,
        timeLimitMs: Int = 20_000,
        autoBot: Boolean = true,
    ): BattleRoomResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/rooms") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(
                    CreateBattleRoomRequest(
                        mode = mode,
                        questionCount = questionCount,
                        timeLimitMs = timeLimitMs,
                        autoBot = autoBot,
                    ),
                )
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun join(code: String, nickname: String? = null): BattleRoomResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/rooms/join") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(
                    JoinBattleRoomRequest(
                        code = code,
                        nickname = nickname?.trim()?.takeIf { it.length >= 2 },
                    ),
                )
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun room(id: String): BattleRoomResponse {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/battle/rooms/${id.encodeURLPathPart()}") {
                bearer(token)
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun setReady(id: String, ready: Boolean): BattleRoomDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/rooms/${id.encodeURLPathPart()}/ready") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(BattleReadyRequest(ready))
            }
        }
        return response.bodyOrThrow<BattleRoomResponse>().room
    }

    suspend fun start(id: String): BattleRoomDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/rooms/${id.encodeURLPathPart()}/start") {
                bearer(token)
            }
        }
        return response.bodyOrThrow<BattleRoomResponse>().room
    }

    suspend fun addBot(id: String): BattleRoomDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/rooms/${id.encodeURLPathPart()}/bots") {
                bearer(token)
            }
        }
        return response.bodyOrThrow<BattleRoomResponse>().room
    }

    suspend fun answer(
        roomId: String,
        roundId: String,
        answerIndex: Int,
    ): BattleAnswerResponse {
        val response = api.authorizedRequest { token ->
            api.http.post(
                "${api.base}v1/battle/rooms/${roomId.encodeURLPathPart()}/answer",
            ) {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(
                    BattleAnswerRequest(
                        roundId = roundId,
                        answerIndex = answerIndex,
                    ),
                )
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun leave(id: String): Boolean {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/rooms/${id.encodeURLPathPart()}/leave") {
                bearer(token)
            }
        }
        return response.bodyOrThrow<BattleLeaveResponse>().closed
    }
}

@kotlinx.serialization.Serializable
private data class BattleLeaveResponse(
    val closed: Boolean = false,
)
