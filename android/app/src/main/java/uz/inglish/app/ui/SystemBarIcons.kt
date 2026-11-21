package uz.inglish.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Keeps status/navigation bar icon contrast correct when screens use different
 * background colours with edge-to-edge enabled.
 */
@Composable
fun SystemBarIcons(
    darkStatusIcons: Boolean,
    darkNavigationIcons: Boolean = true,
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = darkStatusIcons
                isAppearanceLightNavigationBars = darkNavigationIcons
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
