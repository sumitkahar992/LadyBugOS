package com.despicable.core.designsystem.component


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.despicable.core.designsystem.adjustColorAtElevation
import com.despicable.core.designsystem.rememberNoteColor
import com.despicable.core.designsystem.theme.LocalThemeProvider


data class ThemeColors(
    val surfaceColor: Color,
    val onSurfaceColor: Color
)


private data class ExpandedThemeColors(
    val containerColor: Color,
    val tagBackgroundColor: Color,
    val tagContentColor: Color
)


@Composable
private fun rememberAllColors(colorId: Int, elevation: Dp = 0.dp): ExpandedThemeColors {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = rememberNoteColor(colorId)
    val darkTheme = LocalThemeProvider.isDarkTheme

    return remember(containerColor, elevation, colorId, darkTheme) {
        // Apply elevation if needed
        val elevatedColor = if (elevation > 0.dp) {
            adjustColorAtElevation(
                containerColor,
                elevation,
                colorScheme.surfaceTint
            )
        } else {
            containerColor
        }

        // Tag background and content colors based on colorId and theme
        val tagBackground = when {
            colorId == 0 -> colorScheme.surfaceVariant
            darkTheme -> Color.White.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.7f)
        }

        // Text color for tags
        val tagContent = when {
            colorId == 0 -> colorScheme.onSurfaceVariant
            darkTheme -> Color.White.copy(alpha = 0.87f)
            else -> Color.Black.copy(alpha = 0.87f)
        }

        ExpandedThemeColors(
            containerColor = elevatedColor,
            tagBackgroundColor = tagBackground,
            tagContentColor = tagContent
        )
    }
}


@Composable
fun rememberContainerColor(noteColor: Int): Color {
    return rememberAllColors(noteColor).containerColor
}


@Composable
fun rememberTagColors(noteColor: Int): ThemeColors {
    val colors = rememberAllColors(noteColor)
    return ThemeColors(
        surfaceColor = colors.tagBackgroundColor,
        onSurfaceColor = colors.tagContentColor
    )
}

