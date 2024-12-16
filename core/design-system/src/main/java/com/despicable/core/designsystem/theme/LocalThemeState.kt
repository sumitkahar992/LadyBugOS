package com.despicable.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.despicable.model.Theme
import com.despicable.model.ThemeConfig
import com.despicable.model.ThemePreferences


// Theme CompositionLocal
val LocalThemePreferences = compositionLocalOf<ThemePreferences> {
    error("No ThemeState provided")
}


// Theme utils object for accessing theme state
object LocalThemeProvider {
    val current: ThemePreferences
        @Composable
        get() = LocalThemePreferences.current

    val theme: Theme
        @Composable
        get() = current.theme

    val isDarkTheme: Boolean
        @Composable
        get() = current.isDarkTheme

    val colorScheme: ColorScheme
        @Composable
        get() = current.colorScheme

    val dynamicColor: Boolean
        @Composable
        get() = current.dynamicColor
}


@Composable
fun rememberThemeStateProvider(
    themeConfig: ThemeConfig,
    systemInDarkTheme: Boolean = isSystemInDarkTheme()
): ThemePreferences {
    val isDarkTheme by remember(themeConfig.theme, systemInDarkTheme) {
        derivedStateOf {
            when (themeConfig.theme) {
                Theme.System -> systemInDarkTheme
                Theme.Light -> false
                Theme.Dark -> true
            }
        }
    }

    val context = LocalContext.current
    val colorScheme = remember(isDarkTheme, themeConfig.dynamicColor) {
        when {
            themeConfig.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }

            isDarkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    return remember(isDarkTheme, colorScheme, themeConfig) {
        ThemePreferences(
            isDarkTheme = isDarkTheme,
            colorScheme = colorScheme,
            theme = themeConfig.theme,
            dynamicColor = themeConfig.dynamicColor
        )
    }
}