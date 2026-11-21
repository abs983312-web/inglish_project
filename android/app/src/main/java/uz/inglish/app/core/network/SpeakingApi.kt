package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import uz.inglish.app.core.network.dto.CreateSpeakingSubmissionRequest
import uz.inglish.app.core.network.dto.SpeakingPromptDto
import uz.inglish.app.core.network.dto.SpeakingPromptResponse
import uz.inglish.app.core.network.dto.SpeakingPromptsResponse
import uz.inglish.app.core.network.dto.SpeakingSubmissionDto
import uz.inglish.app.core.network.dto.SpeakingSubmissionResponse
import uz.inglish.app.core.network.dto.SpeakingSubmissionSummaryDto
import uz.inglish.app.core.network.dto.SpeakingSubmissionsResponse

class SpeakingApi(private val api: ApiClient) {
    suspend fun prompts(part: String? = null): List<SpeakingPromptDto> {
        val response = api.http.get("${api.base}v1/speaking/prompts") {
            part?.let { parameter("part", it) }
        }
        return response.bodyOrThrow<SpeakingPromptsResponse>().prompts
    }

    suspend fun prompt(slug: String): SpeakingPromptDto {
        val response = api.http.get(
            "${api.base}v1/speaking/prompts/${slug.encodeURLPathPart()}",
        )
        return response.bodyOrThrow<SpeakingPromptResponse>().prompt
    }

    suspend fun createSubmission(
        promptSlug: String,
        transcript: String,
        durationSeconds: Int? = null,
    ): SpeakingSubmissionDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/speaking/submissions") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(
                    CreateSpeakingSubmissionRequest(
                        promptSlug = promptSlug,
                        transcript = transcript,
                        durationSeconds = durationSeconds,
                    ),
                )
            }
        }
        return response.bodyOrThrow<SpeakingSubmissionResponse>().submission
    }

    suspend fun submit(id: String, idempotencyKey: String? = null): SpeakingSubmissionDto {
        val response = api.authorizedRequest { token ->
            api.http.post(
                "${api.base}v1/speaking/submissions/${id.encodeURLPathPart()}/submit",
            ) {
                bearer(token)
                idempotencyKey?.let { header("Idempotency-Key", it) }
            }
        }
        return response.bodyOrThrow<SpeakingSubmissionResponse>().submission
    }

    suspend fun submissions(limit: Int = 20): List<SpeakingSubmissionSummaryDto> {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/speaking/submissions") {
                bearer(token)
                parameter("limit", limit)
            }
        }
        return response.bodyOrThrow<SpeakingSubmissionsResponse>().submissions
    }

    suspend fun submission(id: String): SpeakingSubmissionDto {
        val response = api.authorizedRequest { token ->
            api.http.get(
                "${api.base}v1/speaking/submissions/${id.encodeURLPathPart()}",
            ) { bearer(token) }
        }
        return response.bodyOrThrow<SpeakingSubmissionResponse>().submission
    }
}
