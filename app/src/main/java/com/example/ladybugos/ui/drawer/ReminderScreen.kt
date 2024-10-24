package com.example.ladybugos.ui.drawer

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: NotesViewModel = koinViewModel(),
    drawerState: DrawerState? = null,
    onSearch: () -> Unit,
    selectedTheme: Theme,
    handleNoteClick: (Note) -> Unit,
    toggleSelection: (Note) -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val archivedNotes = notes.filter { it.isArchived }
    val scope = rememberCoroutineScope()
    val selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()



    NoteAppScaffold(
        scrollBehavior = scrollBehavior,
        title = "Reminders",
        onMenuClick = { scope.launch { drawerState?.open() } },
        onSearch = onSearch,
        content = {
            if (archivedNotes.isEmpty()) {
                Text(
                    text = "Notes with upcoming reminder appear here",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize()
                )
            } else {
                NoteGrid(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    notes = archivedNotes,
                    selectedNotes = selectedNotes,
                    onNoteClick = { handleNoteClick(it) },
                    onNoteLongPress = { toggleSelection(it) },
                    theme = selectedTheme,
                    searchHeightPadding = 0.dp,
                    pinnedHeader = false
                )
            }
        },
    )
}*/

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.ui.components.SwipeToDismissContentSnack
import com.example.ladybugos.ui.drawer.home.GridLayout
import com.example.ladybugos.ui.components.NoteItemTag
import com.example.ladybugos.ui.drawer.home.NoteListViewModel
import com.example.ladybugos.ui.components.SectionHeader
import com.example.ladybugos.ui.components.bottomWindowInsetsPadding
import com.example.ladybugos.ui.components.endWindowInsetsPadding
import com.example.ladybugos.ui.components.getSearchBarHeight
import com.example.ladybugos.ui.components.startWindowInsetsPadding
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: NoteListViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
) {
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val completedReminders by viewModel.completedReminders.collectAsState()

    // grid layout
    val gridLayout by viewModel.gridLayout.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    fun toggleSelection(note: Note) {
        selectedNotes = if (note in selectedNotes) selectedNotes - note else selectedNotes + note
    }

    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) toggleSelection(note)
        else navigateToNoteDetail(note.id)
    }

    fun handleAction(action: (List<Note>) -> Unit, message: String) {
        action(selectedNotes.toList())
        scope.launch {
            val result = snackBarHostState.showSnackbar(message, "UNDO")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastOperation()
            }
        }
        selectedNotes = emptySet()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                SwipeToDismissContentSnack(
                    onSwipeToDismiss = { snackBarHostState.currentSnackbarData?.dismiss() },
                    content = { Snackbar(snackbarData = data) }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedTopBar(
                selectedNotes = selectedNotes,
                isSearchActive = isSearchActive,
                onClearSelection = { selectedNotes = emptySet() },
                onPinNotes = {},
                onUnarchiveNotes = {},
                onDeleteNotes = {
                    handleAction(
                        viewModel::trashNotes,
                        "${selectedNotes.size} notes moved to trash",
                    )
                },
                title = "Reminders",
                onMenuClick = onMenuClick,
                onSearchActiveChange = { isSearchActive = it },
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                scrollBehavior = scrollBehavior,
                onSearchClosed = {
                    isSearchActive = false
                    viewModel.updateSearchQuery("")
                },
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (upcomingReminders.isEmpty() && completedReminders.isEmpty()) {
                    Text(
                        text = if (searchQuery.isBlank()) "No notes with reminders"
                        else "No notes found",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize()
                    )
                } else {
                    NoteGridTagsReminder(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        upcomingList = upcomingReminders,
                        completedList = completedReminders,
                        selectedNotes = selectedNotes,
                        onNoteClick = ::handleNoteClick,
                        onNoteLongPress = ::toggleSelection,
                        theme = theme,
                        searchHeightPadding = 0.dp,
                        gridLayout = gridLayout
                    )
                }
            }
        }
    )
}


@Composable
fun NoteGridTagsReminder(
    modifier: Modifier = Modifier,
    upcomingList: List<NoteWithTags>,
    completedList: List<NoteWithTags>,
    selectedNotes: Set<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note) -> Unit,
    gridLayout: GridLayout,
    theme: Theme,
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
                theme = theme,
                isSelected = noteWithTags.note in selectedNotes,
                onClick = { onNoteClick(noteWithTags.note) },
                onLongPress = { onNoteLongPress(noteWithTags.note) },
                gridLayout = gridLayout
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
                theme = theme,
                isSelected = noteWithTags.note in selectedNotes,
                onClick = { onNoteClick(noteWithTags.note) },
                onLongPress = { onNoteLongPress(noteWithTags.note) },
                gridLayout = gridLayout
            )
        }
    }
}

