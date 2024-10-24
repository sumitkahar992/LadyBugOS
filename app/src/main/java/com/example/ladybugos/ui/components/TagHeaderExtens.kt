package com.example.ladybugos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.ladybugos.model.Tag
import kotlinx.coroutines.delay


fun LazyStaggeredGridScope.tagHeader(
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagClick: (Long) -> Unit,
    onTagLongClick: (Tag) -> Unit,
    onAddTagClick: () -> Unit
) {

//    if (tags.isNotEmpty()) {   }
    item(span = StaggeredGridItemSpan.FullLine) {
        val scroll = rememberLazyListState()

        LazyRow(
            modifier = Modifier
                .animateItem()
                .wrapContentSize(unbounded = true)
                .width(LocalConfiguration.current.screenWidthDp.dp),
            state = scroll,
            contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tags) { tag ->
                TagChip(
                    tag = tag,
                    isSelected = tag.id == selectedTagId,
                    onClick = { onTagClick(tag.id) },
                    onLongClick = { onTagLongClick(tag) }
                )
            }
            item {
                AddTagChip(onClick = onAddTagClick)
            }
        }

    }


}


@Composable
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var longPressActive by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            longPressActive = false
            delay(500L)
            longPressActive = true
            onLongClick()

        }
    }

    FilterChip(
        selected = isSelected,
        label = { Text(text = tag.name) },
        modifier = Modifier
            .padding(end = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(tag.color),
            selectedLabelColor = Color.Black,
        ),
        onClick = {
            if (!longPressActive) {
                onClick()
            }
        },
        interactionSource = interactionSource
    )
}


@Composable
fun AddTagChip(onClick: () -> Unit) {
    // Consistent with other chips, but styled differently to indicate 'Add'
    FilterChip(
        selected = false, // 'Add' button shouldn't have a selected state
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Tag",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Add Tag",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        modifier = Modifier.padding(end = 8.dp),
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,  // Matches background of unselected tag
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,   // Matches text color for unselected tag
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ) // Similar border style as unselected tags
    )
}