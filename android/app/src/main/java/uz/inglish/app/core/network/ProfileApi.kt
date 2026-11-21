package uz.inglish.app.core.network

import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.inglish.app.core.network.dto.MeResponse
import uz.inglish.app.core.network.dto.UserDto

class ProfileApi(private val api: ApiClient) {
    /**
     * Partial profile update. The payload is built by hand so that explicit
     * `null` values clear optional fields (target band, exam date, CEFR level)
     * while required fields are never sent as null.
     */
    suspend fun updateProfile(
        displayName: String,
        cefrLevel: String?,
        targetBand: Double?,
        examDate: String?,
        onboardingDone: Boolean,
    ): UserDto {
        val payload = buildJsonObject {
            put("displayName", displayName)
            put("cefrLevel", cefrLevel?.let { JsonPrimitive(it) } ?: JsonNull)
            put("targetBand", targetBand?.let { JsonPrimitive(it) } ?: JsonNull)
            put("examDate", examDate?.let { JsonPrimitive(it) } ?: JsonNull)
            put("onboardingDone", JsonPrimitive(onboardingDone))
        }

        val response = api.authorizedRequest { token ->
            api.http.put("${api.base}v1/me/profile") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }
        return response.bodyOrThrow<MeResponse>().user
    }

    suspend fun deleteAccount() {
        val response = api.authorizedRequest { token ->
            api.http.delete("${api.base}v1/me") { bearer(token) }
        }
        response.throwIfError()
    }
}
