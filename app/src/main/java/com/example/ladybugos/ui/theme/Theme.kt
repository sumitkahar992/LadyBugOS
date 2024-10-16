package com.example.ladybugos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ladybugos.R

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun LadyBugOSTheme(
    theme: Theme = Theme.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemTheme = isSystemInDarkTheme()
    val isDarkTheme = remember(theme) {
        when (theme) {
            Theme.System -> systemTheme
            Theme.Light -> false
            Theme.Dark -> true
        }
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(isDarkTheme) {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !isDarkTheme
            onDispose {}
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                content = content
            )
        }
    )
}

data class Settings(
    val theme: Theme,
)


enum class Theme(@StringRes val nameRes: Int) {
    System(R.string.label_theme_system),
    Light(R.string.label_theme_light),
    Dark(R.string.label_theme_dark),
}