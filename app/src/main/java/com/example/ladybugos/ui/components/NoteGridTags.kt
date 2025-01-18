package com.example.ladybugos.ui.components

import androidx.annotation.Keep
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.navigation.FastOutSlowInEasing
import com.example.ladybugos.navigation.LocalNavAnimatedVisibilityScope
import com.example.ladybugos.navigation.LocalSharedTransitionScope
import com.example.ladybugos.navigation.NoteSharedElementKey
import com.example.ladybugos.navigation.NoteSharedElementType
import com.example.ladybugos.ui.components.tag.rememberContainerColor
import com.example.ladybugos.ui.components.tag.rememberTagColors
import com.example.ladybugos.ui.theme.GridLayout
import com.example.ladybugos.ui.theme.LocalThemeProvider
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
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
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
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
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
    noteWithTags: NoteWithTags,
    gridLayout: GridLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    // De-structure note and tags at composition time
    val note = noteWithTags.note
    val tags = noteWithTags.tags


    // Memorize haptic feedback
    val hapticFeedback = LocalHapticFeedback.current

    // Optimize border animation with custom spring spec
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
        animationSpec = borderAnimationSpec,
        finishedListener = { // Optional: Clean up animation resources
//            if (!isSelected) {
            // Any cleanup needed when animation finishes
//            }
        }
    )

    val shapeAnimationSpec = remember {
        spring<Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    val shape by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 12.dp,
        label = "shape",
        animationSpec = shapeAnimationSpec
    )

    val surfaceColor = rememberContainerColor(note.lightColor)

    val height = calculateNoteHeight(
        note = note,
        hasReminder = note.reminderDate != null,
        hasTags = tags.isNotEmpty(),
        gridLayout = gridLayout
    )

    val (titleSize, contentSize) = remember(gridLayout) {
        when (gridLayout) {
            GridLayout.OneColumn -> 18.sp to 14.sp
            GridLayout.TwoColumns -> 16.sp to 14.sp
            GridLayout.ThreeColumns -> 14.sp to 12.sp
        }
    }

    // Get transition scopes
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No Scope found")
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
        ?: throw IllegalStateException("No Scope found")

    // Optimize corner animation
    val roundedCornerAnimation by animatedVisibilityScope.transition.animateDp(label = "Rounded corner") {
        if (it == EnterExitState.Visible) 12.dp else 0.dp
    }

    val borderColors =
        if (note.lightColor == 0) MaterialTheme.colorScheme.outlineVariant else Color.Transparent


    with(sharedTransitionScope) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .padding(4.dp)
                .border(0.7.dp, borderColors, RoundedCornerShape(shape))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(shape)
                        )
                    } else {
                        Modifier
                    }
                )
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
            color = surfaceColor,
            shape = RoundedCornerShape(roundedCornerAnimation),
            shadowElevation = if (isSelected) 2.dp else 1.dp
        ) {
            NoteContent(
                skipModifier = Modifier.skipToLookaheadSize(),
                modifier = Modifier
                    .sharedBounds(
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
                gridLayout = gridLayout,
                titleSize = titleSize,
                contentSize = contentSize,
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
    gridLayout: GridLayout,
    titleSize: TextUnit,
    contentSize: TextUnit,
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .fillMaxWidth()
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
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Content Section
        if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                fontSize = contentSize,
                modifier = skipModifier.weight(1f, fill = false)
            )
        }

        // Bottom Section
        BottomSection(
            modifier =skipModifier,
            reminderDate = note.reminderDate,
            isDone = note.isDone,
            tags = tags,
            noteColor = note.lightColor
        )
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
@Composable
fun ReminderInfo(
    modifier: Modifier = Modifier,
    reminderDate: Long,
    isDone: Boolean,
    onClick: () -> Unit = {},
    isClickable: Boolean = false,
) {

    val formattedDate = remember(reminderDate) {
        formatReminderDate(reminderDate)
    }

    val darkTheme = LocalThemeProvider.isDarkTheme
    val surfaceColor = if (darkTheme) {
        Color.White.copy(alpha = 0.15f) // Semi-transparent white for dark theme
    } else {
        Color.White.copy(alpha = 0.85f) // More opaque white for light theme
    }

    val contentColor = if (darkTheme) {
        Color.White.copy(alpha = 0.87f)
    } else {
        Color.Black.copy(alpha = 0.87f)
    }


    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Haptic feedback
    val haptic = LocalHapticFeedback.current

    // Animation for flash effect
    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) surfaceColor.copy(alpha = 0.5f) else surfaceColor,
        animationSpec = tween(
            durationMillis = if (isPressed) 50 else 200,
            easing = FastOutSlowInEasing
        ),
        label = "surface color animation"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = animatedColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(6.dp),
            modifier = modifier
                .height(27.dp)
                .then(
                    if (isClickable) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                            }
                        )
                    } else {
                        Modifier
                    }
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
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else null
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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


// Helper class to manage note dimensions
private object NoteDimensions {
    val MIN_HEIGHT = 70.dp
    val MAX_HEIGHT = 420.dp

    // Height steps for different content lengths
    val contentHeightRanges = listOf(
        30 to 100.dp,
        40 to 150.dp,
        80 to 170.dp,
        120 to 190.dp,
        170 to 220.dp,
        240 to 260.dp,
        480 to 280.dp,
        555 to 330.dp,
        666 to 380.dp
    )
}

@Composable
fun calculateNoteHeight(
    note: Note,
    hasReminder: Boolean,
    hasTags: Boolean,
    gridLayout: GridLayout
): Dp {
    return remember(note, hasReminder, hasTags, gridLayout) {
        when (gridLayout) {
            GridLayout.ThreeColumns -> calculateCompactHeight(note, hasReminder, hasTags)
            else -> calculateExpandedHeight(note, hasReminder, hasTags)
        }
    }
}

private fun calculateExpandedHeight(
    note: Note,
    hasReminder: Boolean,
    hasTags: Boolean
): Dp {
    val baseHeight = when {
        note.title.isEmpty() && note.content.isEmpty() -> NoteDimensions.MIN_HEIGHT
        note.content.isEmpty() -> (note.title.length / 40f * 45.dp + 75.dp).coerceIn(75.dp, 120.dp)
        note.title.isEmpty() -> (note.content.length / 80f * 65.dp + 75.dp).coerceIn(75.dp, 140.dp)
        else -> {
            val range = NoteDimensions.contentHeightRanges.find { (length, _) ->
                note.content.length < length
            } ?: (Int.MAX_VALUE to NoteDimensions.MAX_HEIGHT)
            range.second
        }
    }

    // Add extra height for reminder and tags
    return baseHeight +
            (if (hasReminder) 40.dp else 0.dp) +
            (if (hasTags) 32.dp else 0.dp)
}

private fun calculateCompactHeight(
    note: Note,
    hasReminder: Boolean,
    hasTags: Boolean
): Dp {
    val baseHeight = when {
        note.title.isEmpty() && note.content.isEmpty() -> NoteDimensions.MIN_HEIGHT
        note.content.isEmpty() -> (note.title.length / 20f * 20.dp + 100.dp).coerceIn(
            100.dp,
            140.dp
        )

        note.title.isEmpty() -> (note.content.length / 40f * 50.dp + 80.dp).coerceIn(80.dp, 180.dp)
        else -> (note.content.length / 30f * 40.dp + 120.dp).coerceIn(120.dp, 260.dp)
    }

    return baseHeight +
            (if (hasReminder) 32.dp else 0.dp) +
            (if (hasTags) 28.dp else 0.dp)
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
