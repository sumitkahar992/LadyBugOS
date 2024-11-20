package com.example.ladybugos.ui.drawer.note_detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ladybugos.model.Tag
import com.example.ladybugos.navigation.LocalSharedTransitionScope
import com.example.ladybugos.ui.components.TagChip
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun EditNoteContentPreview() {
    // Sample tags for preview
    val sampleTags = listOf(
        Tag(1, "Work"),
        Tag(2, "Personal"),
        Tag(3, "Important"),
        Tag(4, "Shopping"),
        Tag(5, "Grocery"),
        Tag(6, "Study"),
        Tag(7, "Meeting"),
        Tag(8, "Birthday"),
    )

    // Sample state for preview
    val previewState = NoteUiState(
        id = 1L,
        isLoading = false,
        title = "Sample Note Title",
        content = "This is a sample note content.\nIt can have multiple lines.",
        lightColor = Color.Blue.toArgb(),
        isPinned = true,
        isArchived = false,
        isTrashed = false,
        isDone = false,
        reminderDate = System.currentTimeMillis() + 86400000, // Tomorrow
        updateDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        allTags = sampleTags,
        selectedTagIds = setOf(1L, 3L) // Work and Important tags selected
    )

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            EditNoteContent(
                uiState = previewState,
                onTitleChange = {},
                onContentChange = {},
                onOpenColorPicker = {},
                onClickReminderInfo = {},
                isDone = false,
                onRemoveReminder = {}
            ) {
                LazyRow(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .width(LocalConfiguration.current.screenWidthDp.dp),
                    contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sampleTags) { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = true,
                            onClick = { },
                        )
                    }
                }
            }
        }
    }
}

// Additional preview variants
@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "Empty Note")
@Composable
fun EmptyEditNoteContentPreview() {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            EditNoteContent(
                uiState = NoteUiState(), // Default empty state
                onTitleChange = {},
                onContentChange = {},
                onOpenColorPicker = {},
                onClickReminderInfo = {},
                isDone = false,
                onRemoveReminder = {}
            ) {}
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "Long Content")
@Composable
fun LongEditNoteContentPreview() {
    val longContent = buildString {
        repeat(5) {
            appendLine("This is paragraph $it with some long content that should wrap to multiple lines.")
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            EditNoteContent(
                uiState = NoteUiState(
                    title = "Long Note Example",
                    content = longContent,
                    lightColor = Color.LightGray.toArgb()
                ),
                onTitleChange = {},
                onContentChange = {},
                onOpenColorPicker = {},
                onClickReminderInfo = {},
                isDone = false,
                onRemoveReminder = {}
            ) {}
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "With Reminder")
@Composable
fun ReminderEditNoteContentPreview() {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            EditNoteContent(
                uiState = NoteUiState(
                    title = "Note with Reminder",
                    content = "This note has a reminder set",
                    reminderDate = System.currentTimeMillis() + 86400000,
                    lightColor = Color.Yellow.toArgb()
                ),
                onTitleChange = {},
                onContentChange = {},
                onOpenColorPicker = {},
                onClickReminderInfo = {},
                isDone = false,
                onRemoveReminder = {}
            ) {}
        }
    }
}