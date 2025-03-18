package com.despicable.feature.home.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.despicable.core.common.navigation.NoteActionType
import com.despicable.core.designsystem.component.ReminderDialog
import com.despicable.core.designsystem.theme.GridLayout
import com.despicable.core.model.Note
import com.despicable.feature.home.HandleNoteActions
import com.despicable.feature.home.NoteItemTag
import com.despicable.feature.home.NoteListViewModel
import com.despicable.feature.home.NoteScreenContent
import com.despicable.feature.home.NoteSnackBarHandler
import com.despicable.feature.home.NoteWithTagsAndChecklist
import com.despicable.feature.home.R
import com.despicable.feature.home.SectionHeader
import com.despicable.feature.home.SwipeableSnackBarHost
import com.despicable.feature.home.bottomWindowInsetsPadding
import com.despicable.feature.home.endWindowInsetsPadding
import com.despicable.feature.home.getSearchBarHeight
import com.despicable.feature.home.startWindowInsetsPadding
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: NoteListViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
    noteId: Long?,
    actionType: NoteActionType?,
    clearNoteAction: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//    val upcomingReminders = uiState.notes.filter { !it.note.isDone && it.note.reminderDate != null && !it.note.isTrashed }
//    val completedReminders = uiState.notes.filter { it.note.isDone && it.note.reminderDate != null && !it.note.isTrashed }

    val upcomingReminders by viewModel.upcomingReminders.collectAsStateWithLifecycle(emptyList())
    val completedReminders by viewModel.completedReminders.collectAsStateWithLifecycle(emptyList())

    var isSearchMode by rememberSaveable { mutableStateOf(false) }


    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    var showReminderDialog by remember { mutableStateOf(false) }
    val snackbarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

    fun toggleSelection(note: Note) {
        selectedNotes = if (note in selectedNotes) selectedNotes - note else selectedNotes + note
    }

    // Also clear snackBar when navigating to detail
    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) {
            toggleSelection(note)
        } else {
            viewModel.clearSnackBarMessage()
            navigateToNoteDetail(note.id)
        }
    }

    fun handleAction(
        action: (List<Note>) -> Unit,
        message: String
    ) {
        action(selectedNotes.toList())
        scope.launch {
            val result = snackBarHostState.showSnackbar(message, "UNDO")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastOperation()
            }
        }
        selectedNotes = emptySet()
    }

    // Handle actions
    HandleNoteActions(
        noteId = noteId,
        actionType = actionType,
        viewModel = viewModel,
        clearAction = { clearNoteAction() },
        isSearchExpanded = isSearchExpanded,
        setSearchExpanded = { isSearchExpanded = it },
        selectedNotes = selectedNotes,
        clearSelectedNotes = { selectedNotes = emptySet() },
        isSearchMode = isSearchMode,
        setSearchMode = { isSearchMode = it }
    )

    // Handle snackBar
    NoteSnackBarHandler(snackbarMessage, snackBarHostState)

    Scaffold(
        snackbarHost = { SwipeableSnackBarHost(snackBarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            AnimatedTopBar(
                title = "Reminder",
                selectedNotes = selectedNotes,
                isSearchActive = isSearchExpanded,
                onSearchActiveChange = { isSearchExpanded = it },
                onClearSelection = { selectedNotes = emptySet() },
                onSetReminder = { showReminderDialog = true },
                onPinNotes = {
                    handleAction(
                        viewModel::pinAndUnarchiveNotes,
                        "Notes pinned"
                    )
                },
                onUnarchiveNotes = {
                    handleAction(
                        viewModel::unarchiveNotes,
                        "Notes unarchived"
                    )
                },
                onDeleteNotes = {
                    handleAction(
                        viewModel::trashNotes,
                        "${selectedNotes.size} notes moved to trash"
                    )
                },
                onMenuClick = onMenuClick,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { padding ->
        NoteScreenContent(
            paddingValues = padding,
            notes = upcomingReminders + completedReminders,
            isInitialized = uiState.isNotesInitialized,
            searchQuery = uiState.searchQuery,
            emptyIcon = R.drawable.reminders,
            emptyTitle = "No notes with reminders"
        ) {
            NoteGridTagsReminder(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                upcomingList = upcomingReminders,
                completedList = completedReminders,
                selectedNotes = selectedNotes,
                onNoteClick = ::handleNoteClick,
                onNoteLongPress = ::toggleSelection,
                gridLayout = uiState.gridLayout,
                searchHeightPadding = 0.dp,
            )
        }

        ReminderDialog(
            showDialog = showReminderDialog,
            onDismiss = { showReminderDialog = false },
            onSetReminder = { reminderDate ->
                selectedNotes.forEach { note ->
                    viewModel.updateNoteReminder(
                        noteId = note.id,
                        reminderDate = reminderDate,
                    )
                }
                selectedNotes = emptySet()
                showReminderDialog = false
            },
            initialDate = selectedNotes.firstOrNull()?.reminderDate,
            onDeleteReminder = {
                selectedNotes.forEach { note ->
                    viewModel.updateNoteReminder(
                        noteId = note.id,
                        reminderDate = null,
                    )
                }
                selectedNotes = emptySet()
                showReminderDialog = false
            }
        )
    }
}


@Composable
fun NoteGridTagsReminder(
    modifier: Modifier = Modifier,
    upcomingList: List<NoteWithTagsAndChecklist>,
    completedList: List<NoteWithTagsAndChecklist>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    gridLayout: GridLayout,
    searchHeightPadding: Dp = getSearchBarHeight(),
) {

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
        GridLayout.TwoColumns -> 0.dp
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

        if (upcomingList.isNotEmpty()) {
            item(
                span = StaggeredGridItemSpan.FullLine,
                key = "upcoming_header"
            ) {
                SectionHeader(text = "Upcoming")
            }
        }

        items(
            items = upcomingList,
            key = { it.note.id }
        ) { noteWithTags ->
            NoteItemTag(
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                noteWithTags = noteWithTags,
                gridLayout = gridLayout,
                isSelected = noteWithTags.note in selectedNotes,
                onClick = { onNoteClick(noteWithTags.note) },
                onLongPress = { onNoteLongPress(noteWithTags.note) }
            )
        }

        if (completedList.isNotEmpty()) {
            item(
                span = StaggeredGridItemSpan.FullLine,
                key = "completed_header"
            ) {
                SectionHeader(text = "Completed")
            }
        }

        items(
            items = completedList,
            key = { it.note.id }
        ) { noteWithTags ->
            NoteItemTag(
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                noteWithTags = noteWithTags,
                gridLayout = gridLayout,
                isSelected = noteWithTags.note in selectedNotes,
                onClick = { onNoteClick(noteWithTags.note) },
                onLongPress = { onNoteLongPress(noteWithTags.note) }
            )
        }
    }
}

