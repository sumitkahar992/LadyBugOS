package com.example.ladybugos.ui.components.tag

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.ladybugos.model.darken
import com.example.ladybugos.ui.theme.LocalThemeProvider


/*
    Data class to hold theme-aware colors
 */
data class ThemeColors(
    val backgroundColor: Color,
    val contentColor: Color
)

/*
    Remember colors for container backgrounds based on theme and note color
 */
@Composable
fun rememberContainerColor(noteColor: Int): Color {
    val isDarkTheme = LocalThemeProvider.isDarkTheme
    val material = MaterialTheme.colorScheme

    return remember(isDarkTheme, noteColor) {
        when {
            // When lightColor is 0 (default), use theme-specific surface colors
            noteColor == 0 -> material.surfaceContainerLow
            // For user-selected colors, maintain existing dark/light logic
            isDarkTheme -> Color(noteColor).darken(0.4f)
            else -> Color(noteColor)
        }
    }
}

/*
    Remember tag colors based on note color and style
 */
@Composable
fun rememberTagColors(noteColor: Int): ThemeColors {
    val darkTheme = LocalThemeProvider.isDarkTheme
    val material = MaterialTheme.colorScheme

    return remember(darkTheme, noteColor) {
        when {
            noteColor == 0 -> ThemeColors(
                backgroundColor = material.primaryContainer,
                contentColor = material.onPrimaryContainer
            )

            else -> if (darkTheme) {
                ThemeColors(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White.copy(alpha = 0.87f)
                )
            } else {
                ThemeColors(
                    backgroundColor = Color.White.copy(alpha = 0.85f),
                    contentColor = Color.Black.copy(alpha = 0.87f)
                )
            }
        }
    }
}
