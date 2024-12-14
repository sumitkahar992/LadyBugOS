package com.example.ladybugos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.ladybugos.model.Tag

// Usage in LazyStaggeredGridScope
fun LazyStaggeredGridScope.tagHeader(
    tags: List<Tag>,
    activeTagIds: Set<Long>,
    selectedTagId: Long?,
    onTagClick: (Long) -> Unit,
) {
    // Only show tags that have active notes
    val activeTags = tags.filter { it.id in activeTagIds }

    if (activeTags.isNotEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            val scroll = rememberLazyListState()
            LazyRow(
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .width(LocalConfiguration.current.screenWidthDp.dp),
                state = scroll,
                contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = activeTags,
                    key = { it.id } // Add key for better performance
                ) { tag ->
                    TagChip(
                        tag = tag,
                        isSelected = tag.id == selectedTagId,
                        onClick = { onTagClick(tag.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun TagHeader(
    tags: List<Tag>,
    selectedTagId: Long?,
    activeTagIds: Set<Long>,
    onTagClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show tags that have active notes
    val activeTags = tags.filter { it.id in activeTagIds }
    val scroll = rememberLazyListState()
    if (activeTags.isNotEmpty()) {

        LazyRow(
            modifier = modifier
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
                )
            }
        }
    }
}

@Composable
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
    labelColor: Color = Color.Unspecified
) {
    FilterChip(
        selected = isSelected,
        enabled = enabled,
        label = { Text(text = tag.name) },
        modifier = Modifier
            .padding(end = 6.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = containerColor,
            selectedLabelColor = labelColor
        ),
        onClick = onClick,
    )
}