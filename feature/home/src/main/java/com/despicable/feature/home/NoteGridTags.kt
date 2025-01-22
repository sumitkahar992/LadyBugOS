package com.despicable.feature.home

import androidx.annotation.Keep
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.despicable.core.common.navigation.LocalNavAnimatedVisibilityScope
import com.despicable.core.common.navigation.LocalSharedTransitionScope
import com.despicable.core.common.navigation.NoteSharedElementKey
import com.despicable.core.common.navigation.NoteSharedElementType
import com.despicable.core.model.Checklist
import com.despicable.core.designsystem.component.ReminderInfo
import com.despicable.core.designsystem.component.rememberContainerColor
import com.despicable.core.designsystem.component.rememberTagColors
import com.despicable.core.designsystem.theme.GridLayout
import com.despicable.core.model.Note
import com.despicable.core.model.Tag


@Composable
fun NoteGridTags(
    modifier: Modifier = Modifier,
    notes: List<NoteWithTagsAndChecklist>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    gridLayout: GridLayout,
    pinnedHeader: Boolean = true,
    searchHeightPadding: Dp = getSearchBarHeight(),
    gridContent: LazyStaggeredGridScope.() -> Unit = {},
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
            .fillMaxSize(),
//            .animateContentSize(animationSpec = tween(durationMillis = 300)),
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
                modifier = Modifier.animateItem(),
                noteWithTags = noteWithTags,
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
                modifier = Modifier.animateItem(),
                noteWithTags = noteWithTags,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NoteItemTag(
    modifier: Modifier = Modifier,
    noteWithTags: NoteWithTagsAndChecklist,
    gridLayout: GridLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val note = noteWithTags.note
    val tags = noteWithTags.tags
    val hapticFeedback = LocalHapticFeedback.current

    // Memoize derived values
    val (titleSize, contentSize) = remember(gridLayout) {
        when (gridLayout) {
            GridLayout.OneColumn -> 18.sp to 14.sp
            GridLayout.TwoColumns -> 16.sp to 14.sp
            GridLayout.ThreeColumns -> 14.sp to 12.sp
        }
    }

    val borderAnimationSpec = remember {
        spring<Color>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = null
        )
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "borderColor",
        animationSpec = borderAnimationSpec
    )

    val surfaceColor = rememberContainerColor(note.lightColor)
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val borderStroke = if (note.lightColor == 0) {
        BorderStroke(0.7.dp, outlineVariant)
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }

    val border = if (isSelected) {
        BorderStroke(2.dp, borderColor)
    } else {
        borderStroke
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No Scope found")
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
        ?: throw IllegalStateException("No Scope found")

    val roundedCornerAnimation by animatedVisibilityScope.transition.animateDp(
        label = "Rounded corner",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) {
        if (it == EnterExitState.Visible) 12.dp else 0.dp
    }

    with(sharedTransitionScope) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(4.dp)
                .skipToLookaheadSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = NoteSharedElementKey(note.id, NoteSharedElementType.Bounds)
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(
                        RoundedCornerShape(roundedCornerAnimation)
                    )
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                ),
            border = border,
            color = surfaceColor,
            shape = RoundedCornerShape(roundedCornerAnimation),
            shadowElevation = if (isSelected) 2.dp else 0.dp,

            ) {
            NoteContent(
                skipModifier = Modifier.skipToLookaheadSize(),
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = NoteSharedElementKey(note.id, NoteSharedElementType.Content)
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(
                        RoundedCornerShape(roundedCornerAnimation)
                    ),
                ),
                note = note,
                tags = tags,
                checkList = noteWithTags.checklistItems,
                gridLayout = gridLayout,
                titleSize = titleSize,
                contentSize = contentSize
            )
        }
    }
}

@Composable
private fun NoteContent(
    modifier: Modifier = Modifier,
    skipModifier: Modifier = Modifier,
    note: Note,
    tags: List<Tag>,
    checkList: List<Checklist>,
    gridLayout: GridLayout,
    titleSize: TextUnit,
    contentSize: TextUnit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        // Title Section
        if (note.title.isNotBlank()) {
            Text(
                modifier = skipModifier,
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (gridLayout == GridLayout.OneColumn) 1 else 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = titleSize
            )
        }

        Spacer(modifier = Modifier.height(8.dp))


        // Content Section with max lines based on grid
        if (note.isChecklist) {
            ChecklistContent(
                modifier = skipModifier,
                list = checkList,
                contentSize = contentSize,
                gridLayout = gridLayout
            )
        } else if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = when (gridLayout) {
                    GridLayout.OneColumn -> 8
                    GridLayout.TwoColumns -> 16
                    GridLayout.ThreeColumns -> 12
                },
                fontSize = contentSize,
                modifier = skipModifier,
            )
        }

        // Bottom Section
        BottomSection(
            modifier = skipModifier,
            reminderDate = note.reminderDate,
            isDone = note.isDone,
            tags = tags,
            noteColor = note.lightColor
        )
    }

}


@Composable
private fun ChecklistContent(
    modifier: Modifier,
    list: List<Checklist>,
    contentSize: TextUnit,
    gridLayout: GridLayout
) {
    val maxItems = when (gridLayout) {
        GridLayout.OneColumn -> 3
        GridLayout.TwoColumns -> 16
        GridLayout.ThreeColumns -> 5
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        list.take(maxItems).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (item.isChecked) {
                        Icons.Rounded.CheckBox
                    } else {
                        Icons.Rounded.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
//                    tint = MaterialTheme.colorScheme.onSurface,
                    tint = if (item.isChecked) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = contentSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
//                    color = MaterialTheme.colorScheme.onSurface
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (item.isChecked) 0.6f else 1f
                    ),
                )
            }
        }

        if (list.size > maxItems) {
            Text(
                text = " + ${list.size - maxItems} more items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun BottomSection(
    modifier: Modifier = Modifier,
    reminderDate: Long?,
    isDone: Boolean,
    tags: List<Tag>,
    noteColor: Int
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Reminder
        if (reminderDate != null) {
            ReminderInfo(
                reminderDate = reminderDate,
                isDone = isDone,
                isClickable = false
            )
        }

        // Tags
        if (tags.isNotEmpty()) {
            TagList(tags = tags, noteColor = noteColor)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagList(tags: List<Tag>, noteColor: Int) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        maxItemsInEachRow = 3
    ) {
        tags.take(2).forEach { tag ->
            TagChip(
                tag = tag,
                noteColor = noteColor
            )
        }

        if (tags.size > 2) {
            TagChip(
                text = "+${tags.size - 2}",
                noteColor = noteColor
            )
        }
    }
}

@Composable
private fun TagChip(
    tag: Tag? = null,
    text: String = tag?.name ?: "",
    noteColor: Int = 0
) {
    // Calculate colors based on theme and noteColor
    val tagColors = rememberTagColors(noteColor)

    Surface(
        color = tagColors.backgroundColor,
        contentColor = tagColors.contentColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
        )
    }
}


/*
 val formattedDate = remember(reminderDate) {
       DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
           .format(Date(reminderDate))
   }
   val newFormattedDate = remember(reminderDate) {
        val instant = Instant.ofEpochMilli(reminderDate)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
        dateTime.format(formatter)
    }
    */


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
            -> Icons.Default.AutoRenew to "Daily Reminder"

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
