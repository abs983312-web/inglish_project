package uz.inglish.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.inglish.app.BuildConfig
import uz.inglish.app.core.network.dto.ApiError
import uz.inglish.app.core.network.dto.AuthResponse
import uz.inglish.app.core.network.dto.RefreshRequest
import uz.inglish.app.core.storage.TokenStore

class ApiException(
    message: String,
    val code: String? = null,
    val status: Int = 0,
) : Exception(message)

class SessionExpiredException : Exception("Sessiya muddati tugagan. Qaytadan kiring.")

/**
 * Owns the HTTP client, the base URL, and the refresh-token rotation.
 *
 * Feature APIs build their own typed requests but delegate the "attach the
 * access token and retry once after a refresh" logic to [authorizedRequest],
 * so token rotation exists in exactly one place.
 */
class ApiClient(
    val http: HttpClient,
    private val tokenStore: TokenStore,
) {
    val base: String = BuildConfig.API_BASE_URL

    private val refreshMutex = Mutex()

    // StateFlow rather than SharedFlow: expiry must survive the gap between a
    // request failing and the root composable subscribing its collector.
    private val _sessionExpired = MutableStateFlow(false)

    /** True when the server rejected the session and local tokens were cleared. */
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    suspend fun saveSession(auth: AuthResponse) {
        tokenStore.save(auth.accessToken, auth.refreshToken)
        _sessionExpired.value = false
    }

    suspend fun accessToken(): String? = tokenStore.accessToken()

    suspend fun authorizedRequest(
        build: suspend (accessToken: String) -> HttpResponse,
    ): HttpResponse {
        val token = tokenStore.accessToken() ?: expireSession()
        val first = build(token)
        if (first.status != HttpStatusCode.Unauthorized) return first

        val refreshed = refresh(staleAccessToken = token)
        val retry = build(refreshed)
        if (retry.status == HttpStatusCode.Unauthorized) {
            tokenStore.clear()
            expireSession()
        }
        return retry
    }

    suspend fun refresh(staleAccessToken: String? = null): String = refreshMutex.withLock {
        val currentAccessToken = tokenStore.accessToken()

        // Another request already refreshed while this one waited for the lock.
        if (
            staleAccessToken != null &&
            currentAccessToken != null &&
            currentAccessToken != staleAccessToken
        ) {
            return@withLock currentAccessToken
        }

        val refreshToken = tokenStore.refreshToken() ?: expireSession()

        val response = http.request("${base}v1/auth/refresh") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }

        if (
            response.status == HttpStatusCode.Unauthorized ||
            response.status == HttpStatusCode.BadRequest
        ) {
            tokenStore.clear()
            expireSession()
        }

        val auth = response.bodyOrThrow<AuthResponse>()
        tokenStore.save(auth.accessToken, auth.refreshToken)
        auth.accessToken
    }

    /** Clears the local session before any network call, so logout cannot hang the UI. */
    suspend fun clearSession(): String? {
        val refreshToken = tokenStore.refreshToken()
        tokenStore.clear()
        return refreshToken
    }

    /** Best-effort server-side revoke. The local session is already gone. */
    suspend fun revoke(refreshToken: String?) {
        if (refreshToken == null) return

        runCatching {
            http.request("${base}v1/auth/logout") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }
        }
    }

    suspend fun logout() {
        revoke(clearSession())
    }

    private suspend fun expireSession(): Nothing {
        tokenStore.clear()
        _sessionExpired.value = true
        throw SessionExpiredException()
    }
}

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    if (status.isSuccess()) return body()

    val error = try {
        body<ApiError>()
    } catch (_: Exception) {
        null
    }

    throw ApiException(
        message = error?.message ?: "Xatolik yuz berdi (${status.value})",
        code = error?.code,
        status = status.value,
    )
}

internal suspend fun HttpResponse.throwIfError() {
    if (status.isSuccess()) return

    val error = try {
        body<ApiError>()
    } catch (_: Exception) {
        null
    }

    throw ApiException(
        message = error?.message ?: "Xatolik yuz berdi (${status.value})",
        code = error?.code,
        status = status.value,
    )
}

internal fun HttpRequestBuilder.bearer(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}
