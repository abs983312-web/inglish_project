package uz.inglish.app.core.network

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import uz.inglish.app.core.network.dto.BattleNicknameRequest
import uz.inglish.app.core.network.dto.BattlePackDto
import uz.inglish.app.core.network.dto.BattlePackEnvelope
import uz.inglish.app.core.network.dto.BattlePackListDto
import uz.inglish.app.core.network.dto.BattleRoomDto
import uz.inglish.app.core.network.dto.BattleRoomResponse
import uz.inglish.app.core.network.dto.CreateBattlePackRequest
import uz.inglish.app.core.network.dto.DeleteBattlePackResponse
import uz.inglish.app.core.network.dto.PlayBattlePackRequest

class BattlePackApi(private val api: ApiClient) {
    suspend fun list(
        scope: String = "popular",
        search: String? = null,
        tag: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): BattlePackListDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/battle/packs") {
                bearer(token)
                parameter("scope", scope)
                parameter("offset", offset)
                parameter("limit", limit)
                if (!search.isNullOrBlank()) parameter("search", search)
                if (!tag.isNullOrBlank()) parameter("tag", tag)
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun get(id: String): BattlePackDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/battle/packs/${id.encodeURLPathPart()}") {
                bearer(token)
            }
        }
        return response.bodyOrThrow<BattlePackEnvelope>().pack
    }

    suspend fun create(request: CreateBattlePackRequest): BattlePackDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/packs") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        return response.bodyOrThrow<BattlePackEnvelope>().pack
    }

    suspend fun update(id: String, request: CreateBattlePackRequest): BattlePackDto {
        val response = api.authorizedRequest { token ->
            api.http.put("${api.base}v1/battle/packs/${id.encodeURLPathPart()}") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        return response.bodyOrThrow<BattlePackEnvelope>().pack
    }

    suspend fun delete(id: String): Boolean {
        val response = api.authorizedRequest { token ->
            api.http.delete("${api.base}v1/battle/packs/${id.encodeURLPathPart()}") {
                bearer(token)
            }
        }
        return response.bodyOrThrow<DeleteBattlePackResponse>().deleted
    }

    suspend fun play(
        id: String,
        mode: String = "FRIEND",
        shuffle: Boolean = false,
    ): BattleRoomResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/battle/packs/${id.encodeURLPathPart()}/play") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(PlayBattlePackRequest(mode = mode, shuffle = shuffle))
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun setNickname(roomId: String, nickname: String): BattleRoomDto {
        val response = api.authorizedRequest { token ->
            api.http.post(
                "${api.base}v1/battle/rooms/${roomId.encodeURLPathPart()}/nickname",
            ) {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(BattleNicknameRequest(nickname))
            }
        }
        return response.bodyOrThrow<BattleRoomResponse>().room
    }
}
