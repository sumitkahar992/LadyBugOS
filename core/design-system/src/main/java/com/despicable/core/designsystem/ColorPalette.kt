package com.despicable.core.designsystem

import androidx.compose.ui.graphics.Color

val colorPalette = listOf(
    Color(0xFFFFDAC1), Color(0xFFC5E2D2), Color(0xFFB2EBF2),
    Color(0xFFFFE082), Color(0xFFD7CCC8), Color(0xFFDCD3FF), Color(0xFFFFE5C0),
    Color(0xFFF1E0FF), Color(0xFFF48FB1), Color(0xFFFFF289), Color(0xFFADD8E6),
    Color(0xFFE6F3E3), Color(0xFFC7CEEA), Color(0xFFD1C4E9), Color(0xFFFADAD9),
    Color(0xFFE6D2AA), Color(0xFFC8E6C9), Color(0xFFFFECB3), Color(0xFF78E9DA),
    Color(0xFFBFE3D3), Color(0xFFB5EAD7), Color(0xFFF7D1BA), Color(0xFFFF9AA2),
    Color(0xFFFFB7B2), Color(0xFFFDFFB6), Color(0xFFBDB2FF),
    Color(0xFFA0E7E5), Color(0xFFF0DEFD), Color(0xFFE2F0CB), Color(0xFFD5F4E6),
    Color(0xFFF09EFF), Color(0xFFB8F6EA), Color(0xFFFFF1C9), Color(0xFFFFCFDF),
    Color(0xFFE1F8DC), Color(0xFFFFE6E6), Color(0xFFC9F3E4), Color(0xFFF8E1A6),
    Color(0xFFE8D3EF), Color(0xFFE3F2FD), Color(0xFFF0F4E3), Color(0xFFFDE2E4),
    Color(0xFFD4E9DA), Color(0xFFE4F3EA), Color(0xFFA5D6A7), Color(0xFF81D4FA),
    Color(0xFF60E3F3), Color(0xFFCE93D8), Color(0xFFFFAB91), Color(0xFFE6EE9C),
    Color(0xFFC5E1A5), Color(0xFFD0E0E3), Color(0xFFBED2E6),
)


// Darken extension function for Dark Theme
fun Color.darken(factor: Float): Color {
    return this.copy(
        red = (this.red * factor).coerceIn(0f, 1f),
        green = (this.green * factor).coerceIn(0f, 1f),
        blue = (this.blue * factor).coerceIn(0f, 1f)
    )
}
