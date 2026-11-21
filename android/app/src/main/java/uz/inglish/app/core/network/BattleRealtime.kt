package uz.inglish.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import uz.inglish.app.core.network.dto.BattleRoomDto
import uz.inglish.app.core.network.dto.BattleSocketMessage

/**
 * Subscribes to a battle room over WebSocket. Game actions stay on REST; this
 * stream only carries authoritative room snapshots (question changes, reveal,
 * scoreboard, presence).
 */
class BattleRealtime(
    private val client: HttpClient,
    private val api: ApiClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun updates(roomId: String): Flow<BattleRoomDto> = flow {
        val token = api.accessToken() ?: throw SessionExpiredException()
        val wsBase = api.base
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        val url = "${wsBase}v1/battle/rooms/$roomId/ws?token=${token.encodeURLParameter()}"

        client.webSocket(urlString = url) {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val message = json.decodeFromString<BattleSocketMessage>(frame.readText())
                when (message.type) {
                    "room" -> message.room?.let { emit(it) }
                    "room-closed" -> throw ApiException(
                        message = "Xona yopildi.",
                        code = "ROOM_CLOSED",
                        status = 410,
                    )
                    "error" -> throw ApiException(
                        message = message.message ?: "Battle ulanishida xatolik.",
                        code = message.code,
                        status = 401,
                    )
                }
            }
        }
    }
}
