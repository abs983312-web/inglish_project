package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import uz.inglish.app.core.network.dto.PlacementAnswerRequest
import uz.inglish.app.core.network.dto.PlacementLatestResponse
import uz.inglish.app.core.network.dto.PlacementSessionDto
import uz.inglish.app.core.network.dto.PlacementSessionResponse
import uz.inglish.app.core.network.dto.PlacementSubmitRequest
import uz.inglish.app.core.network.dto.PlacementSubmitResponse

class PlacementApi(private val api: ApiClient) {
    suspend fun create(): PlacementSessionDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/placement/sessions") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
        }
        return response.bodyOrThrow<PlacementSessionResponse>().session
    }

    suspend fun latest(): PlacementSessionDto? {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/placement/latest") {
                bearer(token)
            }
        }
        return response.bodyOrThrow<PlacementLatestResponse>().session
    }

    suspend fun submit(
        sessionId: String,
        answers: List<PlacementAnswerRequest>,
        writingText: String,
        speakingTranscript: String,
        idempotencyKey: String? = null,
    ): PlacementSessionDto {
        val response = api.authorizedRequest { token ->
            api.http.post(
                "${api.base}v1/placement/sessions/${sessionId.encodeURLPathPart()}/submit",
            ) {
                bearer(token)
                idempotencyKey?.let { header("Idempotency-Key", it) }
                contentType(ContentType.Application.Json)
                setBody(PlacementSubmitRequest(answers, writingText, speakingTranscript))
            }
        }
        return response.bodyOrThrow<PlacementSubmitResponse>().session
    }
}
