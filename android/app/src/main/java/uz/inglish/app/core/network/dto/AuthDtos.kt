package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class CodeRequest(
    val code: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val password: String,
)

@Serializable
data class VerifyEmailRequestResponse(
    val alreadyVerified: Boolean = false,
    val expiresAt: String? = null,
)

@Serializable
data class VerifyEmailConfirmResponse(
    val emailVerified: Boolean = false,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val emailVerified: Boolean = false,
    val createdAt: String = "",
    val profile: ProfileDto? = null,
)

@Serializable
data class ProfileDto(
    val displayName: String,
    val uiLang: String = "uz",
    val sourceLang: String = "uz",
    val targetLang: String = "en",
    val cefrLevel: String? = null,
    val targetBand: Double? = null,
    val examDate: String? = null,
    val onboardingDone: Boolean = false,
)

@Serializable
data class MeResponse(
    val user: UserDto,
)

@Serializable
data class ApiError(
    val code: String = "ERROR",
    val message: String = "Xatolik yuz berdi",
)
