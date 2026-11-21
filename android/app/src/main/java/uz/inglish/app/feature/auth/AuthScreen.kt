package uz.inglish.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.ui.SystemBarIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onResetPassword: (code: String, password: String) -> Unit,
    onDismissReset: () -> Unit,
    onClearError: () -> Unit,
) {
    var isLogin by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var resetMode by remember { mutableStateOf(false) }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }

    SystemBarIcons(darkStatusIcons = false, darkNavigationIcons = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F3D2E),
                        Color(0xFF1E6B52),
                        Color(0xFFF7F3EA),
                    ),
                ),
            )
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Inglish",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "IELTS 5.5 → 6.5 AI murabbiy",
                color = Color(0xFFE8F3ED),
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (resetMode) {
                        PasswordResetContent(
                            state = state,
                            email = email,
                            onEmailChange = {
                                email = it
                                onClearError()
                            },
                            code = resetCode,
                            onCodeChange = { resetCode = it },
                            newPassword = newPassword,
                            onNewPasswordChange = { newPassword = it },
                            newPasswordVisible = newPasswordVisible,
                            onToggleNewPassword = { newPasswordVisible = !newPasswordVisible },
                            onForgotPassword = { onForgotPassword(email) },
                            onResetPassword = { onResetPassword(resetCode, newPassword) },
                            onBack = {
                                resetMode = false
                                resetCode = ""
                                newPassword = ""
                                onDismissReset()
                            },
                        )
                        return@Column
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEDE7DA), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AuthTab(
                            text = "Kirish",
                            selected = isLogin,
                            modifier = Modifier.weight(1f),
                        ) {
                            isLogin = true
                            passwordVisible = false
                            onClearError()
                        }

                        AuthTab(
                            text = "Ro'yxatdan o'tish",
                            selected = !isLogin,
                            modifier = Modifier.weight(1f),
                        ) {
                            isLogin = false
                            passwordVisible = false
                            onClearError()
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    if (!isLogin) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                onClearError()
                            },
                            label = { Text("Ismingiz") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearError()
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text("Parol") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.height(48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = if (passwordVisible) "Yashirish" else "Ko'rsatish",
                                    fontSize = 11.sp,
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )

                    if (!isLogin) {
                        Text(
                            text = "Kamida 8 belgi",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isLogin) {
                        TextButton(
                            onClick = {
                                resetMode = true
                                resetCode = ""
                                newPassword = ""
                                onClearError()
                                onDismissReset()
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) {
                            Text("Parolni unutdingizmi?", fontSize = 12.sp)
                        }
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (isLogin) {
                                onLogin(email, password)
                            } else {
                                onRegister(displayName, email, password)
                            }
                        },
                        enabled = !state.loading &&
                            email.isNotBlank() &&
                            password.isNotBlank() &&
                            (isLogin || displayName.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = if (isLogin) "Kirish" else "Boshlash",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Davom etish orqali siz foydalanish shartlari va maxfiylik siyosatiga rozilik bildirasiz.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Bepul: kuniga 3 ta Speaking · oyiga 1 ta Writing",
                color = Color(0xFF2C4A3D),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PasswordResetContent(
    state: AuthUiState,
    email: String,
    onEmailChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    newPasswordVisible: Boolean,
    onToggleNewPassword: () -> Unit,
    onForgotPassword: () -> Unit,
    onResetPassword: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = "Parolni tiklash",
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(6.dp))

    when (state.resetStage) {
        ResetStage.IDLE -> {
            Text(
                text = "Emailingizni kiriting — 6 xonali tasdiqlash kodini yuboramiz.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onForgotPassword,
                enabled = !state.resetLoading && email.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (state.resetLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Kod yuborish", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        ResetStage.CODE_SENT -> {
            Text(
                text = state.resetNotice ?: "Kod yuborildi.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { value ->
                    onCodeChange(value.filter { it.isDigit() }.take(6))
                },
                label = { Text("6 xonali kod") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("Yangi parol") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (newPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(
                        onClick = onToggleNewPassword,
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = if (newPasswordVisible) "Yashirish" else "Ko'rsatish",
                            fontSize = 11.sp,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )
            Text(
                text = "Kamida 8 belgi",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onResetPassword,
                enabled = !state.resetLoading && code.length == 6 && newPassword.length >= 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (state.resetLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Parolni yangilash", fontWeight = FontWeight.SemiBold)
                }
            }
            TextButton(
                onClick = onForgotPassword,
                enabled = !state.resetLoading,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text("Kodni qayta yuborish", fontSize = 12.sp)
            }
        }

        ResetStage.DONE -> {
            Text(
                text = state.resetNotice ?: "Parol yangilandi.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Kirishga qaytish", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (state.resetError != null) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.resetError,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    if (state.resetStage != ResetStage.DONE) {
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Text("Orqaga", fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.AuthTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFF0F3D2E) else Color(0xFF6B756F),
        )
    }
}
