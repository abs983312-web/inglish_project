package uz.inglish.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary seed: #1A6FE8 (Confident Blue — IELTS focus)
val PrimaryBlue = Color(0xFF1A6FE8)
val OnPrimaryBlue = Color(0xFFFFFFFF)
val PrimaryContainerBlue = Color(0xFFD6E4FF)
val OnPrimaryContainerBlue = Color(0xFF001849)

val SuccessGreen = Color(0xFF2ECC71)
val SuccessContainerGreen = Color(0xFFC8F7DC)
val WarningOrange = Color(0xFFF39C12)
val WarningContainerOrange = Color(0xFFFEF3CD)
val ErrorRed = Color(0xFFE53935)
val ErrorContainerRed = Color(0xFFFFCDD2)

// Light Color Scheme
private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = SuccessGreen,
    onSecondary = Color.White,
    secondaryContainer = SuccessContainerGreen,
    tertiary = WarningOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarningContainerOrange,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerRed,
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEEF2FF),
    onSurfaceVariant = Color(0xFF44474E),
    background = Color(0xFFFAFBFF),
    onBackground = Color(0xFF1A1C1E),
    outline = Color(0xFFB0BEC5),
    outlineVariant = Color(0xFFE8EDF5),
)

// Dark Color Scheme
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF004BA0),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF6FD99A),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    tertiary = Color(0xFFFFD740),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
)

@Composable
fun InglishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
