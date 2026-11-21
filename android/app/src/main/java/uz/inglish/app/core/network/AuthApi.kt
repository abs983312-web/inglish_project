package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import uz.inglish.app.core.network.dto.AuthResponse
import uz.inglish.app.core.network.dto.CodeRequest
import uz.inglish.app.core.network.dto.ForgotPasswordRequest
import uz.inglish.app.core.network.dto.LoginRequest
import uz.inglish.app.core.network.dto.MeResponse
import uz.inglish.app.core.network.dto.RegisterRequest
import uz.inglish.app.core.network.dto.ResetPasswordRequest
import uz.inglish.app.core.network.dto.UserDto
import uz.inglish.app.core.network.dto.VerifyEmailConfirmResponse
import uz.inglish.app.core.network.dto.VerifyEmailRequestResponse

class AuthApi(private val api: ApiClient) {
    suspend fun register(email: String, password: String, displayName: String): AuthResponse {
        val response = api.http.post("${api.base}v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email = email, password = password, displayName = displayName))
        }
        val auth = response.bodyOrThrow<AuthResponse>()
        api.saveSession(auth)
        return auth
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response = api.http.post("${api.base}v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password))
        }
        val auth = response.bodyOrThrow<AuthResponse>()
        api.saveSession(auth)
        return auth
    }

    suspend fun me(): UserDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/me") { bearer(token) }
        }
        return response.bodyOrThrow<MeResponse>().user
    }

    /** Sends (or re-sends) the 6-digit email verification code. */
    suspend fun requestEmailVerification(): VerifyEmailRequestResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/auth/verify-email/request") { bearer(token) }
        }
        return response.bodyOrThrow()
    }

    suspend fun confirmEmailVerification(code: String): VerifyEmailConfirmResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/auth/verify-email/confirm") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(CodeRequest(code = code))
            }
        }
        return response.bodyOrThrow()
    }

    /** Always succeeds from the client's point of view (no account enumeration). */
    suspend fun forgotPassword(email: String) {
        val response = api.http.post("${api.base}v1/auth/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email = email))
        }
        response.throwIfError()
    }

    suspend fun resetPassword(email: String, code: String, password: String) {
        val response = api.http.post("${api.base}v1/auth/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email = email, code = code, password = password))
        }
        response.throwIfError()
    }

    suspend fun clearSession(): String? = api.clearSession()

    suspend fun revoke(refreshToken: String?) = api.revoke(refreshToken)

    suspend fun logout() = api.logout()
}
