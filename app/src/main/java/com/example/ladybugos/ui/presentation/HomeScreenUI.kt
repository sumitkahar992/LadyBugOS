package com.example.ladybugos.ui.presentation

import androidx.annotation.Keep
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.RepeatInterval
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.darken
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.ui.drawer.ArchivedScreen
import com.example.ladybugos.ui.drawer.TrashScreen
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun HomeContent(
    navController: NavHostController,
    noteId: Long
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val selectedItem = remember { mutableStateOf(getDrawerItems().first()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                drawerState = drawerState,
                selectedItem = selectedItem
            )
        }
    ) {
        AppNavigation(
            navController = navController,
            drawerState = drawerState,
            noteId = noteId
        )
    }
}


@Composable
fun AppNavigation(
    navController: NavHostController,
    noteId: Long,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()

    fun navigateToNoteDetail(noteId: Long) {
        navController.navigate(Screen.NoteDetail(id = noteId)) {
            launchSingleTop = true
        }
    }

    val startDestination = remember(noteId) {
        if (noteId == -1L) Screen.NoteList() else Screen.NoteDetail(noteId)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.NoteList> {
            NoteListScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
                navigateToNoteDetail = ::navigateToNoteDetail,
            )
        }

        composable<Screen.Archive> {
            ArchivedScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
                navigateToNoteDetail = ::navigateToNoteDetail,
            )
        }

        composable<Screen.Trash> {
            TrashScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
                navigateToNoteDetail = ::navigateToNoteDetail,
            )
        }


        composable<Screen.NoteDetail> { backStackEntry ->

            val noteID = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val reminderAction = backStackEntry.arguments?.getString("reminderAction")

            EditNoteScreen(
                onBack = navController::popBackStack,
                onDelete = { deletedId ->
                    navController.navigate(Screen.NoteList(deletedId = deletedId)) {
                        popUpTo<Screen.NoteList> { inclusive = true }
                    }
                },
            )
        }

    }
}


private fun NavHostController.popBackStackOnResume() {
    if (lifecycleState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

private val NavHostController.lifecycleState: Lifecycle.State?
    get() = currentBackStackEntry?.lifecycle?.currentState


@Composable
fun NoteGrid(
    modifier: Modifier = Modifier,
    notes: List<Note>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    theme: Theme,
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
                theme = theme,
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
                theme = theme,
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
    theme: Theme,
    isSelected: Boolean,
    note: Note,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {

    val hapticFeedback = LocalHapticFeedback.current

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val shape by animateDpAsState(targetValue = if (isSelected) 16.dp else 12.dp, label = "")

    val darkTheme = when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }

    val surfaceColor = if (darkTheme) {
        Color(note.lightColor).darken(0.4f)
    } else {
        Color(note.lightColor)
    }

    // Calculate height based on title and content presence
    val height = calculateHeight(note)

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NoteGridTags(
    modifier: Modifier = Modifier,
    notes: List<NoteWithTags>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    theme: Theme,
    pinnedHeader: Boolean = true,
    searchHeightPadding: Dp = getSearchBarHeight(),
    gridContent: LazyStaggeredGridScope.() -> Unit
) {
    val pinnedNotes = remember(notes) { notes.filter { it.note.isPinned } }
    val otherNotes = remember(notes) { notes.filter { !it.note.isPinned } }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(
            start = 4.dp + startWindowInsetsPadding(),
            top = searchHeightPadding + 10.dp,
            end = 4.dp + endWindowInsetsPadding(),
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
                onLongPress = { onNoteLongPress(noteWithTags.note) }
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
                onLongPress = { onNoteLongPress(noteWithTags.note) }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
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

    val height = remember(note.title, note.content) {
        calculateHeight(note)
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            note.content.takeIf { it.isNotEmpty() }?.let { content ->
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            note.reminderDate?.let { reminderDate ->
                Spacer(modifier = Modifier.height(8.dp))
                ReminderInfo(
                    reminderDate = reminderDate,
                    onClick = {},
                    isDone = note.isDone
                )
            }

//            if (tags.isNotEmpty()) {
//                Spacer(modifier = Modifier.height(8.dp))
//                TagList(tags = tags)
//            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagList(tags: List<Tag>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        tags.take(3).forEach { tag ->
            TagChipNew(
                tag = tag, isSelected = false,
                onClick = {},
            )
        }
        if (tags.size > 3) {
            Text(
                text = "+${tags.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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



    Surface(
        color = MaterialTheme.colorScheme.background.copy(0.3f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
      /*      if (isDone) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }*/
        }
    }
}

// Helper function to get appropriate icon and description
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


private fun calculateHeight(note: Note): Dp {
    return when {
        note.title.isEmpty() || note.content.isEmpty() -> 64.dp // Minimum height for empty notes
        note.title.isEmpty() -> if (note.content.length < 80) 75.dp else 140.dp
        note.content.isEmpty() -> if (note.title.length < 40) 75.dp else 120.dp
        else -> when {
            note.content.length < 40 -> 100.dp
            note.content.length < 80 -> 140.dp
            note.content.length < 120 -> 180.dp
            note.content.length < 170 -> 220.dp
            note.content.length < 240 -> 260.dp
            note.content.length < 480 -> 280.dp
            note.content.length < 555 -> 300.dp
            note.content.length < 666 -> 360.dp
            else -> 390.dp
        }
    }
}


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


/*@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNoteListScreen() {
    MaterialTheme {
        NoteListScreen(
            notes = dummyNotes,
            snackbarHostState = remember { SnackbarHostState() },
            selectedTheme = Theme.Light,
            onNavigateToCreate = {},
            onNavigateToEdit = {},
            topBarContent = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Notes") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            },
            selectedNotes = emptySet()
        )
    }
}
private val dummyNotes = listOf(
    Note(
        id = 1,
        title = "Sample Note 1",
        content = "This is a sample note content.",
        isPinned = true
    ),
    Note(id = 2, title = "Sample Note 2 sumit", content = ""),
    Note(id = 2, title = "", content = "Another sample note with some content."),
    Note(id = 2, title = "Sample Note 2", content = "Another sample note with some content."),
    Note(id = 3, title = "Pinned Note", content = "This note is pinned.", isPinned = true),
)
*/
