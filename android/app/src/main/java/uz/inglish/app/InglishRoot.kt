package uz.inglish.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.inglish.app.core.AppContainer
import uz.inglish.app.feature.auth.AuthScreen
import uz.inglish.app.feature.auth.AuthViewModel
import uz.inglish.app.ui.MainScreen
import uz.inglish.app.ui.theme.InglishTheme

@Composable
fun InglishRoot(
    container: AppContainer,
    initialBattleCode: String? = null,
    onBattleCodeConsumed: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val state by authViewModel.state.collectAsStateWithLifecycle()

    // A rejected refresh token must return the learner to the login screen even
    // when the failing request came from a feature ViewModel.
    LaunchedEffect(container.apiClient) {
        container.apiClient.sessionExpired.collect { expired ->
            if (expired) authViewModel.onSessionExpired()
        }
    }

    InglishTheme {
        if (state.isAuthenticated) {
            MainScreen(
                container = container,
                user = state.user,
                profileError = state.profileError,
                verifyLoading = state.verifyLoading,
                verifyNotice = state.verifyNotice,
                verifyError = state.verifyError,
                onUpdateProfile = authViewModel::updateProfile,
                onClearProfileError = authViewModel::clearProfileError,
                onDeleteAccount = authViewModel::deleteAccount,
                onRequestEmailVerification = authViewModel::requestEmailVerification,
                onConfirmEmailVerification = authViewModel::confirmEmailVerification,
                onClearVerifyMessages = authViewModel::clearVerifyMessages,
                onLogout = authViewModel::logout,
                initialBattleCode = initialBattleCode,
                onBattleCodeConsumed = onBattleCodeConsumed,
            )
        } else {
            AuthScreen(
                state = state,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onForgotPassword = authViewModel::forgotPassword,
                onResetPassword = authViewModel::resetPassword,
                onDismissReset = authViewModel::dismissReset,
                onClearError = authViewModel::clearError,
            )
        }
    }
}
