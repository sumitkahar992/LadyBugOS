package com.despicable.feature.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.InvertColorsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.despicable.core.designsystem.harmonize
import com.despicable.core.designsystem.noteColors
import com.despicable.core.designsystem.theme.LadyBugOSTheme


/*
@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

}


@Composable
fun ColorPickerDialog(
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    // Track whether the color list is expanded
    var isExpanded by remember { mutableStateOf(false) }
    val isDarkTheme = LocalThemeProvider.isDarkTheme

    // Select appropriate color palette based on theme
    val colorPalette = if (isDarkTheme) keepDarkColorPalette else keepColorPalette

    // Initial display shows fewer colors unless expanded
    val displayedColors = remember(isExpanded, isDarkTheme) {
        if (isExpanded) colorPalette else colorPalette.take(8)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            if (!isExpanded) {
                Text("Note color")
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display the regular colors
                items(displayedColors) { color ->
                    ColorItem(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = {
                            onColorSelected(color)
                        }
                    )
                }

                // Add the "Expand" button as the last item
                if (!isExpanded) {
                    item {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { isExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
*/

@Composable
fun ColorPickerDialog(
    currentColorId: Int = 0,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    // Get the appropriate color palette from the theme
    val noteColors = MaterialTheme.noteColors
    val primary = MaterialTheme.colorScheme.primary

    // Pre-compute harmonized colors for better performance
    val harmonizedColors = remember(noteColors.value, primary) {
        noteColors.value.map { it.harmonize(primary) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Note color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display all palette colors
                itemsIndexed(harmonizedColors) { index, harmonizedColor ->
                    ColorItem(
                        color = harmonizedColor,
                        isSelected = currentColorId == index,
                        isDefaultColor = index == 0,
                        onClick = { onColorSelected(index) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ColorItem(
    color: Color,
    isSelected: Boolean,
    isDefaultColor: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    // Outer box to maintain perfect circle shape
    Box(
        modifier = Modifier
            .size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner box for the actual color circle
        Box(
            modifier = Modifier
                .size(58.dp) // Fixed size for the color circle
                .clip(CircleShape) // Clip first
                .background(color) // Then background
                .border( // Then border
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable(onClick = onClick), // Finally clickable
            contentAlignment = Alignment.Center
        ) {

                if (isSelected) {
                    val iconTint = if (color.luminance() > 0.5f) Color.Black else Color.White

                    if (isDefaultColor) {
                        // Special icon for the default color (index 0)
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Default color selected",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (isDefaultColor) {
                    // Show a special indicator for the default color even when not selected
                    val iconTint = if (color.luminance() > 0.5f)
                        Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)

                    Icon(
                        imageVector = Icons.Outlined.InvertColorsOff, // You could use another icon if preferred
                        contentDescription = "Default color",
                        tint = iconTint,
                        modifier = Modifier.size(32.dp) // Slightly smaller when not selected
                    )
                }
            }
        }
    }




@PreviewLightDark
@Composable
fun ColorPickerDialogPreview() {
    var selectedColor by remember { mutableIntStateOf(4) } // Example selected color

    LadyBugOSTheme {
        ColorPickerDialog(
            currentColorId = selectedColor,
            onColorSelected = { color ->
                selectedColor = color // Update the selected color
            },
            onDismissRequest = {
/* Handle dismiss action */
            }
        )
    }

}
