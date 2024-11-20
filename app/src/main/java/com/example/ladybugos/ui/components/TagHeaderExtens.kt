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
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag


@Composable
fun TagHeader(
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberLazyListState()
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

// Usage in LazyStaggeredGridScope
fun LazyStaggeredGridScope.tagHeader(
    tags: List<Tag>,
    notes: List<NoteWithTags>,
    selectedTagId: Long?,
    onTagClick: (Long) -> Unit,
) {
    // Filter tags that have associated notes
    val tagsWithNotes = tags.filter { tag ->
        notes.any { noteWithTags ->
            noteWithTags.tags.any { it.id == tag.id }
        }
    }

    // Only create the tag header if there are tags with notes
    if (tagsWithNotes.isNotEmpty()) {
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
                items(tagsWithNotes) { tag ->
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
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
    labelColor: Color = Color.Unspecified
) {
    FilterChip(
        selected = isSelected,
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