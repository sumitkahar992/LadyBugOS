package com.despicable.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.despicable.core.designsystem.theme.LocalThemeProvider
import com.google.android.material.color.MaterialColors
import kotlin.math.ln


val LightNoteColors = listOf(
    Color(0xFFFFFFFF),  // White/Default

    // Core Colors
    Color(0xFFB4DDD3),  // Mint
    Color(0xFFAFCCDC),  // Sky Blue
    Color(0xFFFAAFA8),  // Salmon
    Color(0xFFD3C0DB),  // Lavender
    Color(0xFFE2F6D3),  // Sage
    Color(0xFFE9E3D4),  // Cream
    Color(0xFFE4EFA8),  // Butter
    Color(0xFFF3A077),  // Peach

    // Additional Colors
    Color(0xFFD4E4ED),  // Powder Blue
    Color(0xFFB5E2DC),  // Teal
    Color(0xFFD7BDE2),  // Lilac
    Color(0xFFF5CBA7),  // Apricot
    Color(0xFFD5F5E3),  // Mint Green
    Color(0xFFFCE4EC),  // Blush Pink
    Color(0xFFE8F8F5),  // Ice Blue
    Color(0xFFFDEBD0),  // Champagne
    Color(0xFFEAF2F8),  // Cloud Blue
    Color(0xFFCAEF96),  // Pastel Yellow
)

val DarkNoteColors = listOf(
    Color(0xFF202124),  // Dark Gray/Default

    // Core Colors
    Color(0xFF1A403A),  // Dark Mint
    Color(0xFF256377),  // Dark Sky Blue
    Color(0xFF693009),  // Dark Salmon
    Color(0xFF472E5B),  // Dark Lavender
    Color(0xFF1B5E20),  // Dark Sage
    Color(0xFF4B443A),  // Dark Cream
    Color(0xFF695009),  // Dark Butter
    Color(0xFF6D3C20),  // Dark Peach

    // Additional Colors
    Color(0xFF2A4356),  // Dark Powder Blue
    Color(0xFF0C625D),  // Dark Teal
    Color(0xFF4A235A),  // Dark Lilac
    Color(0xFF784212),  // Dark Apricot
    Color(0xFF1E4634),  // Dark Mint Green
    Color(0xFF6C394F),  // Dark Blush Pink
    Color(0xFF1F3A3D),  // Dark Ice Blue
    Color(0xFF5E4929),  // Dark Champagne
    Color(0xFF2C3E50),  // Dark Cloud Blue
    Color(0xFF4D6B30),  // Dark Pastel Yellow
)




/**
 * Stable data class for holding note colors with safe indexing
 */
@Stable
data class NoteColors(val value: List<Color> = emptyList()) {
    operator fun get(index: Int): Color {
        return value[index.coerceIn(0, value.lastIndex)]
    }

    val size: Int get() = value.size
}

// Static composition local for note colors
val LocalNoteColors = staticCompositionLocalOf { NoteColors() }

/**
 * Harmonizes a color with another color (typically theme primary)
 * Uses Material Design color harmonization algorithm
 */
fun Color.harmonize(with: Color): Color =
    Color(MaterialColors.harmonize(this.toArgb(), with.toArgb()))

/**
 * Extension property to harmonize any color with the theme's primary color
 */
val Color.harmonized: Color
    @Composable
    @ReadOnlyComposable
    get() = harmonize(MaterialTheme.colorScheme.primary)

/**
 * Adjust color based on surface elevation following Material Design principles
 */
fun adjustColorAtElevation(
    color: Color,
    elevation: Dp,
    surfaceTint: Color
): Color {
    if (elevation == 0.dp) return color
    val alpha = ((14.5f * ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(color)
}

/**
 * Access note colors from anywhere in the composition
 */
val MaterialTheme.noteColors: NoteColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNoteColors.current


/**
 * Centralized function to get the proper note color based on theme and color ID
 */
@Composable
fun rememberNoteColor(
    colorId: Int,
    isDarkTheme: Boolean = LocalThemeProvider.isDarkTheme,
    harmonizeWithPrimary: Boolean = true
): Color {
    val noteColors = MaterialTheme.noteColors
    val material = MaterialTheme.colorScheme
    val primary = material.primary

    return remember(colorId, isDarkTheme, harmonizeWithPrimary, noteColors, primary) {
        // Get base color (with fallback to default if index is invalid)
        val baseColor = when {
            colorId == 0 || colorId >= noteColors.size -> material.surface
            else -> noteColors[colorId]
        }

        // Apply harmonization if requested
        if (harmonizeWithPrimary) {
            baseColor.harmonize(primary)
        } else {
            baseColor
        }
    }
}

