package uz.inglish.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.network.AuthApi
import uz.inglish.app.core.network.ProfileApi
import uz.inglish.app.core.network.SessionExpiredException
import uz.inglish.app.core.network.dto.AuthResponse
import uz.inglish.app.core.network.dto.UserDto
import uz.inglish.app.core.network.userMessage
import uz.inglish.app.core.storage.TokenStore

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: UserDto? = null,
    val isAuthenticated: Boolean = false,
    val profileSaving: Boolean = false,
    val profileError: String? = null,
    val resetStage: ResetStage = ResetStage.IDLE,
    val resetEmail: String = "",
    val resetLoading: Boolean = false,
    val resetError: String? = null,
    val resetNotice: String? = null,
    val verifyLoading: Boolean = false,
    val verifyNotice: String? = null,
    val verifyError: String? = null,
)

enum class ResetStage { IDLE, CODE_SENT, DONE }

class AuthViewModel(
    private val authApi: AuthApi,
    private val profileApi: ProfileApi,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    fun login(email: String, password: String) {
        val cleanEmail = email.trim()

        if (!isValidEmail(cleanEmail)) {
            showError("Email manzilini to'g'ri kiriting")
            return
        }
        if (password.isBlank()) {
            showError("Parolni kiriting")
            return
        }

        submit { authApi.login(cleanEmail, password) }
    }

    fun register(displayName: String, email: String, password: String) {
        val cleanName = displayName.trim()
        val cleanEmail = email.trim()

        if (cleanName.isBlank()) {
            showError("Ismingizni kiriting")
            return
        }
        if (!isValidEmail(cleanEmail)) {
            showError("Email manzilini to'g'ri kiriting")
            return
        }
        if (password.length < 8) {
            showError("Parol kamida 8 belgidan iborat bo'lishi kerak")
            return
        }

        submit { authApi.register(cleanEmail, password, cleanName) }
    }

    fun logout() {
        viewModelScope.launch {
            val refreshToken = try {
                authApi.clearSession()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            _state.value = AuthUiState()
            // Best-effort revoke after the UI has already returned to the login screen.
            authApi.revoke(refreshToken)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** Called by the shared HTTP client when the refresh token is rejected. */
    fun onSessionExpired() {
        _state.value = AuthUiState(error = "Sessiya muddati tugagan. Qaytadan kiring.")
    }

    fun updateProfile(
        displayName: String,
        cefrLevel: String?,
        targetBand: Double?,
        examDate: String?,
    ) {
        val cleanName = displayName.trim()
        if (cleanName.length < 2) {
            _state.update { it.copy(profileError = "Ism kamida 2 belgidan iborat bo'lishi kerak.") }
            return
        }
        if (_state.value.profileSaving) return

        _state.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            try {
                val user = profileApi.updateProfile(
                    displayName = cleanName,
                    cefrLevel = cefrLevel,
                    targetBand = targetBand,
                    examDate = examDate,
                    onboardingDone = true,
                )
                _state.update { it.copy(profileSaving = false, user = user, profileError = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SessionExpiredException) {
                onSessionExpired()
            } catch (error: Exception) {
                _state.update {
                    it.copy(profileSaving = false, profileError = error.userMessage())
                }
            }
        }
    }

    fun clearProfileError() {
        _state.update { it.copy(profileError = null) }
    }

    fun forgotPassword(email: String) {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            _state.update { it.copy(resetError = "Email manzilini to'g'ri kiriting") }
            return
        }
        if (_state.value.resetLoading) return

        _state.update { it.copy(resetLoading = true, resetError = null, resetNotice = null) }
        viewModelScope.launch {
            try {
                authApi.forgotPassword(cleanEmail)
                _state.update {
                    it.copy(
                        resetLoading = false,
                        resetStage = ResetStage.CODE_SENT,
                        resetEmail = cleanEmail,
                        resetNotice = "Agar bu email ro'yxatdan o'tgan bo'lsa, 6 xonali kod yuborildi.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(resetLoading = false, resetError = error.userMessage())
                }
            }
        }
    }

    fun resetPassword(code: String, password: String) {
        val cleanCode = code.trim()
        val email = _state.value.resetEmail
        if (!cleanCode.matches(Regex("\\d{6}"))) {
            _state.update { it.copy(resetError = "6 xonali kodni kiriting") }
            return
        }
        if (password.length < 8) {
            _state.update { it.copy(resetError = "Yangi parol kamida 8 belgidan iborat bo'lishi kerak") }
            return
        }
        if (_state.value.resetLoading) return

        _state.update { it.copy(resetLoading = true, resetError = null) }
        viewModelScope.launch {
            try {
                authApi.resetPassword(email, cleanCode, password)
                _state.update {
                    it.copy(
                        resetLoading = false,
                        resetStage = ResetStage.DONE,
                        resetNotice = "Parol yangilandi. Endi yangi parol bilan kiring.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(resetLoading = false, resetError = error.userMessage())
                }
            }
        }
    }

    fun dismissReset() {
        _state.update {
            it.copy(
                resetStage = ResetStage.IDLE,
                resetEmail = "",
                resetLoading = false,
                resetError = null,
                resetNotice = null,
            )
        }
    }

    fun requestEmailVerification() {
        if (_state.value.verifyLoading) return
        _state.update { it.copy(verifyLoading = true, verifyError = null, verifyNotice = null) }
        viewModelScope.launch {
            try {
                val result = authApi.requestEmailVerification()
                _state.update {
                    it.copy(
                        verifyLoading = false,
                        verifyNotice = if (result.alreadyVerified) {
                            "Email allaqachon tasdiqlangan."
                        } else {
                            "6 xonali kod emailingizga yuborildi."
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SessionExpiredException) {
                onSessionExpired()
            } catch (error: Exception) {
                _state.update {
                    it.copy(verifyLoading = false, verifyError = error.userMessage())
                }
            }
        }
    }

    fun confirmEmailVerification(code: String) {
        val cleanCode = code.trim()
        if (!cleanCode.matches(Regex("\\d{6}"))) {
            _state.update { it.copy(verifyError = "6 xonali kodni kiriting") }
            return
        }
        if (_state.value.verifyLoading) return

        _state.update { it.copy(verifyLoading = true, verifyError = null) }
        viewModelScope.launch {
            try {
                authApi.confirmEmailVerification(cleanCode)
                _state.update {
                    it.copy(
                        verifyLoading = false,
                        verifyNotice = "Email tasdiqlandi.",
                        user = it.user?.copy(emailVerified = true),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SessionExpiredException) {
                onSessionExpired()
            } catch (error: Exception) {
                _state.update {
                    it.copy(verifyLoading = false, verifyError = error.userMessage())
                }
            }
        }
    }

    fun clearVerifyMessages() {
        _state.update { it.copy(verifyNotice = null, verifyError = null) }
    }

    fun deleteAccount() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, profileError = null) }
        viewModelScope.launch {
            try {
                profileApi.deleteAccount()
                tokenStore.clear()
                _state.value = AuthUiState(error = "Hisobingiz o'chirildi.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: SessionExpiredException) {
                onSessionExpired()
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, profileError = error.userMessage())
                }
            }
        }
    }

    private fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }

    private fun submit(block: suspend () -> AuthResponse) {
        if (_state.value.loading) return

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val auth = block()
                _state.value = AuthUiState(
                    user = auth.user,
                    isAuthenticated = true,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Xatolik yuz berdi",
                    )
                }
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val accessToken = try {
                tokenStore.accessToken()
            } catch (_: Exception) {
                null
            }
            if (accessToken == null) return@launch

            _state.update { it.copy(loading = true) }

            try {
                val user = authApi.me()
                _state.value = AuthUiState(user = user, isAuthenticated = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: SessionExpiredException) {
                tokenStore.clear()
                _state.value = AuthUiState(error = "Sessiya muddati tugagan. Qaytadan kiring.")
            } catch (_: Exception) {
                // Keep the stored token on a network failure so the next app start can retry.
                _state.value = AuthUiState(
                    error = "Serverga ulanib bo'lmadi. Internetni tekshirib, qayta urinib ko'ring.",
                )
            }
        }
    }

    private fun isValidEmail(value: String): Boolean {
        return value.length in 3..254 &&
            value.contains('@') &&
            value.substringBefore('@').isNotBlank() &&
            value.substringAfter('@').contains('.')
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(container.authApi, container.profileApi, container.tokenStore)
            }
        }
    }
}
