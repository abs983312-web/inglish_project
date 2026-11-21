package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import uz.inglish.app.core.network.dto.CreateWritingSubmissionRequest
import uz.inglish.app.core.network.dto.WritingSubmissionDto
import uz.inglish.app.core.network.dto.WritingSubmissionResponse
import uz.inglish.app.core.network.dto.WritingSubmissionSummaryDto
import uz.inglish.app.core.network.dto.WritingSubmissionsResponse
import uz.inglish.app.core.network.dto.WritingTaskDetailDto
import uz.inglish.app.core.network.dto.WritingTaskDto
import uz.inglish.app.core.network.dto.WritingTaskResponse
import uz.inglish.app.core.network.dto.WritingTasksResponse

class WritingApi(private val api: ApiClient) {
    suspend fun tasks(): List<WritingTaskDto> {
        val response = api.http.get("${api.base}v1/writing/tasks")
        return response.bodyOrThrow<WritingTasksResponse>().tasks
    }

    suspend fun task(slug: String): WritingTaskDetailDto {
        val response = api.http.get(
            "${api.base}v1/writing/tasks/${slug.encodeURLPathPart()}",
        )
        return response.bodyOrThrow<WritingTaskResponse>().task
    }

    suspend fun createSubmission(taskSlug: String, text: String): WritingSubmissionDto {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/writing/submissions") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(CreateWritingSubmissionRequest(taskSlug = taskSlug, text = text))
            }
        }
        return response.bodyOrThrow<WritingSubmissionResponse>().submission
    }

    suspend fun submit(id: String, idempotencyKey: String? = null): WritingSubmissionDto {
        val response = api.authorizedRequest { token ->
            api.http.post(
                "${api.base}v1/writing/submissions/${id.encodeURLPathPart()}/submit",
            ) {
                bearer(token)
                idempotencyKey?.let { header("Idempotency-Key", it) }
            }
        }
        return response.bodyOrThrow<WritingSubmissionResponse>().submission
    }

    suspend fun submissions(limit: Int = 20): List<WritingSubmissionSummaryDto> {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/writing/submissions") {
                bearer(token)
                parameter("limit", limit)
            }
        }
        return response.bodyOrThrow<WritingSubmissionsResponse>().submissions
    }

    suspend fun submission(id: String): WritingSubmissionDto {
        val response = api.authorizedRequest { token ->
            api.http.get(
                "${api.base}v1/writing/submissions/${id.encodeURLPathPart()}",
            ) { bearer(token) }
        }
        return response.bodyOrThrow<WritingSubmissionResponse>().submission
    }
}
