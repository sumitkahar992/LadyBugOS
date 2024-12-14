package com.example.ladybugos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.darken
import com.example.ladybugos.model.tags
import com.example.ladybugos.ui.theme.GridLayout
import com.example.ladybugos.ui.theme.LocalThemeProvider

@Composable
fun NoteGrid(
    modifier: Modifier = Modifier,
    notes: List<Note>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    pinnedHeader: Boolean = true,
    searchHeightPadding: Dp = getSearchBarHeight()
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(durationMillis = 500)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(
            start = 4.dp + startWindowInsetsPadding(),
            top = searchHeightPadding + 10.dp,
            end = 4.dp + endWindowInsetsPadding(),
            bottom = 60.dp + bottomWindowInsetsPadding()
        )
    ) {
        val pinnedNotes = notes.filter { it.isPinned }
        val otherNotes = notes.filter { !it.isPinned }

        if (pinnedHeader && pinnedNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = "Pinned",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        // Pinned Notes
        itemsIndexed(pinnedNotes) { _, pinned ->
            NoteItem(
                modifier = Modifier.animateItem(),
                note = pinned,
                isSelected = pinned in selectedNotes,
                onClick = { onNoteClick(pinned) },
                onLongPress = { onNoteLongPress(pinned) }
            )
        }

        if (pinnedHeader && otherNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = "Other",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        // Other Notes
        itemsIndexed(otherNotes) { _, note ->
            NoteItem(
                modifier = Modifier.animateItem(),
                note = note,
                isSelected = note in selectedNotes,
                onClick = { onNoteClick(note) },
                onLongPress = { onNoteLongPress(note) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    note: Note,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {

    val hapticFeedback = LocalHapticFeedback.current

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val shape by animateDpAsState(targetValue = if (isSelected) 16.dp else 12.dp, label = "")

    val darkTheme = LocalThemeProvider.isDarkTheme

    val surfaceColor = if (darkTheme) {
        Color(note.lightColor).darken(0.7f)
    } else {
        Color(note.lightColor)
    }

    // Calculate height based on title and content presence
    val height = calculateNoteHeight(
        note = note,
        hasReminder = note.reminderDate != null,
        hasTags = tags.isNotEmpty(),
        gridLayout = GridLayout.OneColumn
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(4.dp)
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(shape))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            ),
        color = surfaceColor,
        shape = RoundedCornerShape(shape),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            note.title.takeIf { it.isNotEmpty() }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            note.content.takeIf { it.isNotEmpty() }?.let { content ->
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
