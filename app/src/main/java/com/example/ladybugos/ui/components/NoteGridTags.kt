package com.example.ladybugos.ui.components

import androidx.annotation.Keep
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.darken
import com.example.ladybugos.ui.drawer.home.GridLayout
import com.example.ladybugos.ui.theme.Theme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun NoteGridTags(
    modifier: Modifier = Modifier,
    notes: List<NoteWithTags>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    gridLayout: GridLayout,
    theme: Theme,
    pinnedHeader: Boolean = true,
    searchHeightPadding: Dp = getSearchBarHeight(),
    gridContent: LazyStaggeredGridScope.() -> Unit,
) {
    val pinnedNotes = remember(notes) { notes.filter { it.note.isPinned } }
    val otherNotes = remember(notes) { notes.filter { !it.note.isPinned } }

    val columns = when (gridLayout) {
        GridLayout.OneColumn -> StaggeredGridCells.Fixed(1)
        GridLayout.TwoColumns -> StaggeredGridCells.Fixed(2)
        GridLayout.ThreeColumns -> StaggeredGridCells.Fixed(3)
    }

    val (start, end) = when (gridLayout) {
        GridLayout.OneColumn -> 6.dp to 6.dp
        GridLayout.TwoColumns -> 4.dp to 4.dp
        GridLayout.ThreeColumns -> 2.dp to 2.dp
    }
    val spacing = when (gridLayout) {
        GridLayout.OneColumn -> 4.dp
        GridLayout.TwoColumns -> 2.dp
        GridLayout.ThreeColumns -> 1.dp
    }


    LazyVerticalStaggeredGrid(
        columns = columns,
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = PaddingValues(
            start = start + startWindowInsetsPadding(),
            top = searchHeightPadding + 10.dp,
            end = end + endWindowInsetsPadding(),
            bottom = 60.dp + bottomWindowInsetsPadding()
        )
    ) {
        gridContent()

        if (pinnedHeader && pinnedNotes.isNotEmpty()) {
            item(
                span = StaggeredGridItemSpan.FullLine,
                key = "pinned_header"
            ) {
                SectionHeader(text = "Pinned")
            }
        }

        items(
            items = pinnedNotes,
            key = { it.note.id }
        ) { noteWithTags ->
            NoteItemTag(
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                noteWithTags = noteWithTags,
                theme = theme,
                isSelected = noteWithTags.note in selectedNotes,
                onClick = { onNoteClick(noteWithTags.note) },
                onLongPress = { onNoteLongPress(noteWithTags.note) },
                gridLayout = gridLayout
            )
        }

        if (pinnedHeader && otherNotes.isNotEmpty()) {
            item(
                span = StaggeredGridItemSpan.FullLine,
                key = "other_header"
            ) {
                SectionHeader(text = "Other")
            }
        }

        items(
            items = otherNotes,
            key = { it.note.id }
        ) { noteWithTags ->
            NoteItemTag(
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                noteWithTags = noteWithTags,
                theme = theme,
                isSelected = noteWithTags.note in selectedNotes,
                onClick = { onNoteClick(noteWithTags.note) },
                onLongPress = { onNoteLongPress(noteWithTags.note) },
                gridLayout = gridLayout
            )
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemTag(
    modifier: Modifier = Modifier,
    noteWithTags: NoteWithTags,
    theme: Theme,
    gridLayout: GridLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val note = noteWithTags.note
    val tags = noteWithTags.tags

    val darkTheme = when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }

    val hapticFeedback = LocalHapticFeedback.current

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "borderColor"
    )
    val shape by animateDpAsState(targetValue = if (isSelected) 16.dp else 12.dp, label = "shape")

    val surfaceColor = remember(darkTheme, note.lightColor) {
        if (darkTheme) {
            Color(note.lightColor).darken(0.4f)
        } else {
            Color(note.lightColor)
        }
    }

//    val height = remember(note.title, note.content) {
//        calculateHeight(note)
//    }

    val height = remember(note.title, note.content, gridLayout) {
        when (gridLayout) {
            GridLayout.OneColumn -> calculateHeight(note)
            GridLayout.TwoColumns -> calculateHeight(note)
            GridLayout.ThreeColumns -> calculateHeightCompact(note)
        }
    }

    val (titleSize, contentSize) = when (gridLayout) {
        GridLayout.OneColumn -> 18.sp to 14.sp
        GridLayout.TwoColumns -> 16.sp to 14.sp
        GridLayout.ThreeColumns -> 14.sp to 12.sp
    }

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
                    maxLines = if (gridLayout == GridLayout.OneColumn) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = titleSize
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            note.content.takeIf { it.isNotEmpty() }?.let { content ->
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
//                    maxLines = when (gridLayout) {
//                        GridLayout.OneColumn -> 6
//                        GridLayout.TwoColumns -> 5
//                        GridLayout.ThreeColumns -> 3
//                    },
                    fontSize = contentSize,
                    modifier = Modifier.weight(1f)
                )
            }
            note.reminderDate?.let { reminderDate ->
                Spacer(modifier = Modifier.height(8.dp))
                ReminderInfo(
                    reminderDate = reminderDate,
                    isDone = note.isDone,
                    onClick = {}
                )
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                TagList(tags = tags)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagList(tags: List<Tag>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tags.take(3).forEach { tag ->
            TagChip(tag = tag)
        }

        if (tags.size > 3) {
            TagChip(
                text = "+${tags.size - 3}",
                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun TagChip(
    tag: Tag? = null,
    text: String = tag?.name ?: "",
    backgroundColor: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
    contentColor: Color = contentColorFor(backgroundColor)
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
        )
    }
}

@Composable
fun ReminderInfo(
    reminderDate: Long,
    isDone: Boolean,
    onClick: () -> Unit
) {
    /*   val formattedDate = remember(reminderDate) {
           DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
               .format(Date(reminderDate))
       }*/
    /*    val newFormattedDate = remember(reminderDate) {
            val instant = Instant.ofEpochMilli(reminderDate)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            dateTime.format(formatter)
        }*/
    val formattedDate = remember(reminderDate) {
        formatReminderDate(reminderDate)
    }

    // Add interaction state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()



    Surface(
        color = when {
            isPressed -> MaterialTheme.colorScheme.background.copy(0.5f)
            isDone -> MaterialTheme.colorScheme.background.copy(0.2f)
            else -> MaterialTheme.colorScheme.background.copy(0.3f)
        },
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(6.dp)
                .wrapContentWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


private fun formatReminderDate(reminderDate: Long): String {
    val now = LocalDateTime.now()
    val reminderDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(reminderDate),
        ZoneId.systemDefault()
    )

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    return when {
        // Same day
        reminderDateTime.toLocalDate() == now.toLocalDate() ->
            "Today, ${reminderDateTime.format(timeFormatter)}"

        // Next day
        reminderDateTime.toLocalDate() == now.toLocalDate().plusDays(1) ->
            "Tomorrow, ${reminderDateTime.format(timeFormatter)}"

        // Same year - don't show year
        reminderDateTime.year == now.year ->
            "${reminderDateTime.format(dateFormatter)}, ${reminderDateTime.format(timeFormatter)}"

        // Different year - show year
        else ->
            "${reminderDateTime.format(dateFormatter)}, ${reminderDateTime.year}, ${
                reminderDateTime.format(timeFormatter)
            }"
    }
}


fun calculateHeight(note: Note): Dp {
    return when {
        note.title.isEmpty() || note.content.isEmpty() -> 64.dp // Minimum height for empty notes
        note.title.isEmpty() -> if (note.content.length < 80) 75.dp else 140.dp
        note.content.isEmpty() -> if (note.title.length < 40) 75.dp else 120.dp
        else -> when {
            note.content.length < 40 -> 150.dp
            note.content.length < 80 -> 170.dp
            note.content.length < 120 -> 190.dp
            note.content.length < 170 -> 220.dp
            note.content.length < 240 -> 260.dp
            note.content.length < 480 -> 280.dp
            note.content.length < 555 -> 330.dp
            note.content.length < 666 -> 380.dp
            else -> 420.dp
        }
    }
}


private fun calculateHeightCompact(note: Note): Dp {
    return when {
        // Empty or minimal content cases
        note.title.isEmpty() && note.content.isEmpty() -> 100.dp  // Minimum height for empty notes

        // Only title
        note.content.isEmpty() -> when (note.title.length) {
            in 0..20 -> 100.dp
            in 21..40 -> 120.dp
            else -> 140.dp
        }

        // Only content
        note.title.isEmpty() -> when (note.content.length) {
            in 0..40 -> 80.dp
            in 41..80 -> 140.dp
            else -> 180.dp
        }

        // Both title and content
        else -> when (note.content.length) {
            in 0..30 -> 120.dp    // Very short content
            in 31..60 -> 160.dp   // Short content
            in 61..90 -> 200.dp   // Medium content
            in 91..120 -> 240.dp  // Medium-long content
            else -> 260.dp        // Maximum height for extra long content
        }
    }
}


@Keep
const val SearchBarHeight = 64

@Composable
fun getSearchBarHeight() = SearchBarHeight.dp + topWindowInsetsPadding()

@Composable
fun startWindowInsetsPadding(): Dp {
    val layoutDirection = LocalLayoutDirection.current

    return WindowInsets.safeDrawing.asPaddingValues().calculateLeftPadding(layoutDirection)
}

@Composable
fun topWindowInsetsPadding(): Dp {
    return WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
}

@Composable
fun bottomWindowInsetsPadding(): Dp {
    return WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
}

@Composable
fun endWindowInsetsPadding(): Dp {
    val layoutDirection = LocalLayoutDirection.current

    return WindowInsets.safeDrawing.asPaddingValues().calculateRightPadding(layoutDirection)
}

fun Modifier.scrollConnectionToProvideVisibility(visible: MutableState<Boolean>) =
    composed {
        this.nestedScroll(
            remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (consumed.y < -30) {
                            visible.value = false
                        }
                        if (consumed.y > 30) {
                            visible.value = true
                        }
                        if (available.y > 0) {
                            visible.value = true
                        }

                        return super.onPostScroll(
                            consumed,
                            available,
                            source
                        )
                    }
                }
            }
        )
    }


/*

if (isDone) {
    Spacer(modifier = Modifier.width(4.dp))
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Done",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun reminderIcon(repeatInterval: RepeatInterval?): Pair<ImageVector, String> {
    return when (repeatInterval) {
        RepeatInterval.DAILY, RepeatInterval.WEEKLY, RepeatInterval.MONTHLY,
            -> Icons.Default.Autorenew to "Daily Reminder"

        RepeatInterval.YEARLY -> Icons.Outlined.EventRepeat to "Yearly Reminder"
        RepeatInterval.CUSTOM -> Icons.Outlined.EventRepeat to "Custom Repeat Reminder"
        null -> Icons.Default.Alarm to "One-time Reminder"
    }
}

private fun formatReminderDate(reminderDate: Long): String {
    val now = LocalDateTime.now()
    val reminderDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(reminderDate), ZoneId.systemDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    return when {
        reminderDateTime.toLocalDate() == now.toLocalDate() ->
            "Today, ${reminderDateTime.format(timeFormatter)}"

        reminderDateTime.toLocalDate() == now.toLocalDate().plusDays(1) ->
            "Tomorrow, ${reminderDateTime.format(timeFormatter)}"

        reminderDateTime.year == now.year ->
            "${reminderDateTime.format(dateFormatter)}, ${reminderDateTime.format(timeFormatter)}"

        else ->
            "${reminderDateTime.format(dateFormatter)}, ${reminderDateTime.year}, ${
                reminderDateTime.format(
                    timeFormatter
                )
            }"
    }
}
*/

/*
    val relativeTime = formatUpdateDate(note.updateDate)
    val relativeTime = getRelativeTimeAgo(note.updateDate)

 Text(
             text = "Edited: $relativeTime",
             style = TextStyle(
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Muted timestamp color for subtlety
             )
         )
*/
