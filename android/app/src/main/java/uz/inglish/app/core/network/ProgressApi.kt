package uz.inglish.app.core.network

import io.ktor.client.request.get
import uz.inglish.app.core.network.dto.ProgressDto
import uz.inglish.app.core.network.dto.ProgressResponse

class ProgressApi(private val api: ApiClient) {
    suspend fun progress(): ProgressDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/progress") { bearer(token) }
        }
        return response.bodyOrThrow<ProgressResponse>().progress
    }
}
