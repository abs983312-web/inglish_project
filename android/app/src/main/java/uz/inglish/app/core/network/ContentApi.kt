package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.JsonElement
import uz.inglish.app.core.network.dto.ContentUnitDetailDto
import uz.inglish.app.core.network.dto.ContentUnitDto
import uz.inglish.app.core.network.dto.ExerciseAnswerRequest
import uz.inglish.app.core.network.dto.ExerciseAnswerResponse
import uz.inglish.app.core.network.dto.ExerciseAnswerResultDto
import uz.inglish.app.core.network.dto.UnitDetailResponse
import uz.inglish.app.core.network.dto.UnitsResponse

class ContentApi(private val api: ApiClient) {
    suspend fun units(skill: String? = null, level: String? = null): List<ContentUnitDto> {
        val response = api.http.get("${api.base}v1/content/units") {
            skill?.let { parameter("skill", it) }
            level?.let { parameter("level", it) }
        }
        return response.bodyOrThrow<UnitsResponse>().units
    }

    suspend fun unit(slug: String): ContentUnitDetailDto {
        val response = api.http.get("${api.base}v1/content/units/${slug.encodeURLPathPart()}")
        return response.bodyOrThrow<UnitDetailResponse>().unit
    }

    suspend fun checkAnswer(exerciseId: String, answer: JsonElement): ExerciseAnswerResultDto {
        val response = api.authorizedRequest { token ->
            api.http.post(
                "${api.base}v1/content/exercises/${exerciseId.encodeURLPathPart()}/answer",
            ) {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(ExerciseAnswerRequest(answer = answer))
            }
        }
        return response.bodyOrThrow<ExerciseAnswerResponse>().result
    }
}
