package com.example.ladybugos.ui.screens.note_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.ui.theme.LadyBugOSTheme


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
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit,
) {
    // Track whether the color list is expanded
    var isExpanded by remember { mutableStateOf(false) }

    // Limit to show initially 12 items
    val displayedColors = if (isExpanded) colorPalette else colorPalette.take(11)

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


@PreviewLightDark
@Composable
fun ColorPickerDialogPreview() {
    var selectedColor by remember { mutableStateOf(Color.Red) } // Example selected color

    LadyBugOSTheme {
        ColorPickerDialog(
            selectedColor = selectedColor,
            onColorSelected = { color ->
                selectedColor = color // Update the selected color
            },
            onDismissRequest = { /* Handle dismiss action */ }
        )
    }

}