package com.example.ladybugos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


@Composable
fun LadyBugOSTheme(
    themeConfig: ThemeConfig = Settings(theme = Theme.System),
    content: @Composable () -> Unit
) {
    val themeState = rememberThemeStateProvider(themeConfig)

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(themeState.isDarkTheme) {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !themeState.isDarkTheme
            onDispose {}
        }
    }

    CompositionLocalProvider(
        LocalThemePreferences provides themeState
    ) {
        MaterialTheme(
            colorScheme = themeState.colorScheme,
            typography = Typography,
            content = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    content = content
                )
            }
        )
    }
}

