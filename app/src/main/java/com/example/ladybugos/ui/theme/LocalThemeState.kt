package com.example.ladybugos.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ladybugos.R

// Sealed interface for better type safety and extensibility
sealed interface ThemeConfig {
    val theme: Theme
    val dynamicColor: Boolean
}

// Grid layout enum
enum class GridLayout {
    OneColumn, TwoColumns, ThreeColumns
}

// Main settings data class
data class Settings(
    override val theme: Theme,
    override val dynamicColor: Boolean = true,
    val gridLayout: GridLayout = GridLayout.TwoColumns,
) : ThemeConfig

// Theme enum with string resources
enum class Theme(@StringRes val nameRes: Int) {
    System(R.string.label_theme_system),
    Light(R.string.label_theme_light),
    Dark(R.string.label_theme_dark),
}


// Theme CompositionLocal
val LocalThemePreferences = compositionLocalOf<ThemePreferences> {
    error("No ThemeState provided")
}

// Data class to hold theme-related state
@Stable
data class ThemePreferences(
    val isDarkTheme: Boolean,
    val colorScheme: ColorScheme,
    override val theme: Theme,
    override val dynamicColor: Boolean
) : ThemeConfig


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