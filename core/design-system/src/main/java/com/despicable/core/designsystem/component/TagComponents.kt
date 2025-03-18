package com.despicable.core.designsystem.component


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.despicable.core.designsystem.darken
import com.despicable.core.designsystem.theme.LocalThemeProvider

/*
    Data class to hold theme-aware colors
 */
data class ThemeColors(
    val surfaceColor: Color,
    val onSurfaceColor: Color
)

/**
 * Internal class with expanded color information
 */
 private data class ExpandedThemeColors(
    val containerColor: Color,
    val tagBackgroundColor: Color,
    val tagContentColor: Color
)

/**
 * Remembers all theme-aware colors for a note based on its color and current theme
 */
@Composable
 private fun rememberAllColors(noteColor: Int): ExpandedThemeColors {
    val isDarkTheme = LocalThemeProvider.isDarkTheme
    val material = MaterialTheme.colorScheme

    return remember(isDarkTheme, noteColor) {
        // Container background color
        val containerBackground = when {
            // Default theme color when note has no color
            noteColor == 0 -> material.background
            // For custom colors, handle dark/light differently
            isDarkTheme -> Color(noteColor).darken(0.4f)
            else -> Color(noteColor)
        }


        // Tag colors - background with alpha based on theme
        val tagBackground = when {
            noteColor == 0 -> if (isDarkTheme) {
                Color(0xFFE0E0E0).copy(alpha = 0.15f)
            } else {
                Color(0xFFE0E0E0)
            }

            isDarkTheme -> Color.White.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.85f)
        }

        // Tag text color - based on theme
        val tagContent = if (isDarkTheme) {
            Color.White.copy(alpha = 0.87f)
        } else {
            Color.Black.copy(alpha = 0.87f)
        }

        ExpandedThemeColors(
            containerColor = containerBackground,
            tagBackgroundColor = tagBackground,
            tagContentColor = tagContent
        )
    }
}

/*
    Remember colors for container backgrounds based on theme and note color
 */
@Composable
fun rememberContainerColor(noteColor: Int): Color {
    return rememberAllColors(noteColor).containerColor
}

/*
    Remember tag colors based on note color and style
 */
@Composable
fun rememberTagColors(noteColor: Int): ThemeColors {
    val colors = rememberAllColors(noteColor)
    return ThemeColors(
        surfaceColor = colors.tagBackgroundColor,
        onSurfaceColor = colors.tagContentColor
    )
}

