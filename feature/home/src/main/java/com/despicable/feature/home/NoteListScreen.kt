package com.despicable.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.common.navigation.NoteActionType
import com.despicable.core.designsystem.component.ReminderDialog
import com.despicable.core.designsystem.component.SwipeToDismissContentSnack
import com.despicable.core.designsystem.component.tagHeader
import com.despicable.core.designsystem.theme.LocalThemeProvider
import com.despicable.core.model.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteListScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = koinViewModel(),
    navigateToDetail: (Long) -> Unit,
    onMenuClick: () -> Unit,
    noteId: Long?,
    actionType: NoteActionType?,
    clearNoteAction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val notes = uiState.notes.filter { !it.note.isArchived && !it.note.isTrashed }

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val isSearchBarVisible = remember { mutableStateOf(true) }

    var showReminderDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    // grid layout
    var showLayoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val theme = LocalThemeProvider.theme
//    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current


    var isSearchMode by rememberSaveable { mutableStateOf(false) }

    val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()


    fun handleAction(
        action: (Set<Note>) -> Unit,
        message: String,
        restoreAction: () -> Unit
    ) {
        action(selectedNotes)
        scope.launch {
            val result = snackBarHostState.showSnackbar(message, "UNDO")
            if (result == SnackbarResult.ActionPerformed) {
                restoreAction()
            }
        }
        selectedNotes = emptySet()
    }

    val handlePinNotes: () -> Unit = {
        val allPinned = selectedNotes.all { it.isPinned }
        viewModel.run { if (allPinned) ::unpinNotes else ::pinNotes }(selectedNotes.toList())
        selectedNotes = emptySet()
    }

    fun toggleSelection(note: Note) {
        selectedNotes = selectedNotes.toMutableSet().apply {
            if (contains(note)) remove(note) else add(note)
        }
    }

    // Also clear snackBar when navigating to detail
    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) {
            toggleSelection(note)
        } else {
            keyboardController?.hide() // Dismiss keyboard UI
//            focusManager.clearFocus() // Clear any active text field focus
            navigateToDetail(note.id)
        }
    }

    // Handle actions, clear snackBar and backHandler
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
    NoteSnackBarHandler(snackBarMessage, snackBarHostState)


    Scaffold(
        snackbarHost = { SwipeableSnackBarHost(snackBarHostState) },
        modifier = modifier
            .fillMaxSize()
            .scrollConnectionToProvideVisibility(isSearchBarVisible),
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Reset window insets
//        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        floatingActionButton = { NoteFAB(navigateToDetail) },
    ) { padding ->

        Box(Modifier.fillMaxSize()) {

            NoteScreenContent(
                modifier = modifier,
                paddingValues = padding,
                notes = notes,
                isInitialized = uiState.isNotesInitialized,
                searchQuery = uiState.searchQuery,
                emptyIcon = R.drawable.notes,
                emptyTitle = "Notes you add appear here"
            ) {
                NoteGridTags(
                    modifier = modifier,
                    notes = notes,
                    selectedNotes = selectedNotes,
                    onNoteClick = ::handleNoteClick,
                    onNoteLongPress = ::toggleSelection,
                    gridContent = {
                        tagHeader(
                            tags = uiState.tagState.availableTags,
                            activeTagIds = uiState.tagState.activeTagIds,
                            selectedTagId = uiState.tagState.selectedTagId,
                            onTagClick = { tagId -> viewModel.toggleTag(tagId) },
                        )
                    },
                    gridLayout = uiState.gridLayout,
                )
            }
            // Status bar background (MUST COME AFTER CONTENT TO LAYER CORRECTLY)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.systemBars)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            )

            /*
                NoteListSearchUI(
                    onMenuClick = onMenuClick,
                    searchQuery = uiState.searchQuery,
                    onGridLayoutClick = { showLayoutDialog = true },
                    onSearchQueryChanged = viewModel::updateSearchQuery,
                    selectedNotes = selectedNotes,
                    onClearSelection = { selectedNotes = emptySet() },
                    onPinNotes = { handlePinNotes() },
                    onUnPinNotes = { handlePinNotes() },
                    onArchiveNotes = { selectedNotes ->
                        handleAction(
                            action = { viewModel.archiveNotes(selectedNotes.toList()) },
                            message = "${selectedNotes.size} notes archived and unpinned",
                            restoreAction = { viewModel.undoLastOperation() }
                        )
                    },
                    onDeleteNotes = { selectedNotes ->
                        handleAction(
                            action = { viewModel.trashNotes(selectedNotes.toList()) },
                            message = "${selectedNotes.size} notes moved to trash and unpinned",
                            restoreAction = { viewModel.undoLastOperation() }
                        )
                    },
                    isSearchBarVisible = isSearchBarVisible.value,
                    onSetReminder = {
                        showReminderDialog = true
                    },
                    isSearchActive = isSearchExpanded,
                    onSearchActiveChange = { expanded ->
                        isSearchExpanded = expanded
                    },
                    onThemeClick = {
                        showThemeDialog = true
                    },
                    screenType = ScreenType.List,
                    scrollBehavior = scrollBehavior
                )*/

            SearchBarWithActions(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onMenuClick = onMenuClick,
                onLayoutClick = { showLayoutDialog = true },
                isSearchMode = isSearchMode || uiState.searchQuery.isNotEmpty(),
                onBackClick = {
                    isSearchMode = false  // Reset search mode
                    viewModel.updateSearchQuery("")  // Clear search query
                },
                isVisible = isSearchBarVisible.value,
                selectedNotes = selectedNotes,
                onClearSelection = { selectedNotes = emptySet() },
                onPinNotes = { handlePinNotes() },
                onUnPinNotes = { handlePinNotes() },
                onArchiveNotes = { selectedNotes ->
                    handleAction(
                        action = { viewModel.archiveNotes(selectedNotes.toList()) },
                        message = "${selectedNotes.size} notes archived and unpinned",
                        restoreAction = { viewModel.undoLastOperation() }
                    )
                },
                onDeleteNotes = { selectedNotes ->
                    handleAction(
                        action = { viewModel.trashNotes(selectedNotes.toList()) },
                        message = "${selectedNotes.size} notes moved to trash and unpinned",
                        restoreAction = { viewModel.undoLastOperation() }
                    )
                },
                onSetReminder = {
                    showReminderDialog = true
                },
            )
        }
    }

    val initialDate = selectedNotes.firstOrNull()?.reminderDate

    /*    NoteeDialog(
            enabled = showThemeDialog,
            title = "Choose Theme",
            onDismiss = { showThemeDialog = false },
            description = {
                Column(Modifier.selectableGroup()) {
                    com.despicable.feature.settings.ThemeOption(Theme.Light, theme) {
                        viewModel.updateTheme(Theme.Light)
                        showThemeDialog = false
                    }
                    com.despicable.feature.settings.ThemeOption(Theme.Dark, theme) {
                        viewModel.updateTheme(Theme.Dark)
                        showThemeDialog = false
                    }
                    com.despicable.feature.settings.ThemeOption(Theme.System, theme) {
                        viewModel.updateTheme(Theme.System)
                        showThemeDialog = false
                    }
                }
            }
        )*/

    ReminderDialog(
        showDialog = showReminderDialog,
        onDismiss = { showReminderDialog = false },
        onSetReminder = { reminderDate ->
            // Update reminder for all selected notes
            selectedNotes.forEach { note ->
                viewModel.updateNoteReminder(
                    noteId = note.id,
                    reminderDate = reminderDate,
                )
            }
            selectedNotes = emptySet() // Clear selection after updating reminder
            showReminderDialog = false
        },
        initialDate = initialDate,
        onDeleteReminder = {
            selectedNotes.forEach { note ->
                viewModel.updateNoteReminder(
                    noteId = note.id,
                    reminderDate = null,
                )
                selectedNotes = emptySet() // Clear selection after updating reminder
                showReminderDialog = false
            }
        }
    )
    LayoutSelectionDialog(
        enabled = showLayoutDialog,
        currentLayout = uiState.gridLayout,
        onLayoutSelected = { grid ->
            viewModel.updateGridLayout(grid)
            showLayoutDialog = false
        },
        onDismiss = { showLayoutDialog = false }
    )

}


@Composable
private fun NoteFAB(navigateToDetail: (Long) -> Unit) {
    FloatingActionButton(
        onClick = { navigateToDetail(-1L) },
        modifier = Modifier.padding(bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null)
            Text("Create", fontSize = 15.sp)
        }
    }
}

// HANDLE-ACTIONS
@Composable
fun HandleNoteActions(
    noteId: Long?,
    actionType: NoteActionType?, // "archive", "unarchive", "delete"
    viewModel: NoteListViewModel,
    clearAction: () -> Unit,
    isSearchExpanded: Boolean,
    setSearchExpanded: (Boolean) -> Unit,
    selectedNotes: Set<Note>,
    clearSelectedNotes: () -> Unit,
    clearSearch: () -> Unit = { viewModel.updateSearchQuery("") },
    isSearchMode: Boolean,
    setSearchMode: (Boolean) -> Unit
) {
    // Handle note actions from navigation
    LaunchedEffect(noteId, actionType) {
        when {
            noteId == null || actionType == null -> return@LaunchedEffect
            else -> {
                val action = when (actionType) {
                    NoteActionType.ARCHIVE -> NoteAction.Archive(noteId)
                    NoteActionType.UNARCHIVE -> NoteAction.Unarchive(noteId)
                    NoteActionType.DELETE -> NoteAction.Delete(noteId)
                }
                viewModel.handleNoteAction(action)
                clearAction()
            }
        }
    }

    // Clear snackBar when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSnackBarMessage()
        }
    }

    // Handle back button
    BackHandler(enabled = isSearchExpanded || selectedNotes.isNotEmpty() || isSearchMode) {
        when {
            selectedNotes.isNotEmpty() -> clearSelectedNotes()
            isSearchExpanded -> {
                setSearchExpanded(false)
                clearSearch()
                setSearchMode(false)
            }

            isSearchMode -> {
                clearSearch()
                setSearchMode(false)
            }
        }
    }
}

// SNACK-BAR
@Composable
fun NoteSnackBarHandler(
    snackBarMessage: SnackBarMessage?,
    snackBarHostState: SnackbarHostState
) {
    LaunchedEffect(snackBarMessage) {
        snackBarMessage?.let { message ->
            delay(600)
            val result = snackBarHostState.showSnackbar(
                message = message.message,
                actionLabel = message.actionLabel,
                duration = message.duration
            )
            when (result) {
                SnackbarResult.ActionPerformed -> message.onAction()
                SnackbarResult.Dismissed -> message.onDismiss()
            }
        }
    }
}

@Composable
fun SwipeableSnackBarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        SwipeToDismissContentSnack(
            onSwipeToDismiss = { hostState.currentSnackbarData?.dismiss() },
            content = { Snackbar(snackbarData = data) }
        )
    }
}






