package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import uz.inglish.app.core.network.dto.VocabularyItemDto
import uz.inglish.app.core.network.dto.VocabularyListResponse
import uz.inglish.app.core.network.dto.VocabularyReviewRequest
import uz.inglish.app.core.network.dto.VocabularyReviewResponse
import uz.inglish.app.core.network.dto.VocabularySetDto
import uz.inglish.app.core.network.dto.VocabularySetsResponse
import uz.inglish.app.core.network.dto.VocabularyStatsDto
import uz.inglish.app.core.network.dto.VocabularyStatsResponse

class VocabularyApi(private val api: ApiClient) {
    suspend fun due(limit: Int = 20): List<VocabularyItemDto> {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/vocabulary/due") {
                bearer(token)
                parameter("limit", limit)
            }
        }
        return response.bodyOrThrow<VocabularyListResponse>().items
    }

    suspend fun list(
        query: String? = null,
        tag: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): VocabularyListResponse {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/vocabulary") {
                bearer(token)
                parameter("limit", limit)
                parameter("offset", offset)
                query?.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
                tag?.takeIf { it.isNotBlank() }?.let { parameter("tag", it) }
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun sets(): List<VocabularySetDto> {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/vocabulary/sets") { bearer(token) }
        }
        return response.bodyOrThrow<VocabularySetsResponse>().sets
    }

    suspend fun stats(): VocabularyStatsDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/vocabulary/stats") { bearer(token) }
        }
        return response.bodyOrThrow<VocabularyStatsResponse>().stats
    }

    suspend fun review(id: String, rating: Int): VocabularyReviewResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/vocabulary/${id.encodeURLPathPart()}/review") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(VocabularyReviewRequest(rating = rating))
            }
        }
        return response.bodyOrThrow()
    }
}
