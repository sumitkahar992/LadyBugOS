package com.example.ladybugos.ui.drawer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.Tag
import com.example.ladybugos.ui.components.ExpandableSearchView
import com.example.ladybugos.ui.components.NoteGridTags
import com.example.ladybugos.ui.components.SwipeToDismissContentSnack
import com.example.ladybugos.ui.components.scrollConnectionToProvideVisibility
import com.example.ladybugos.ui.components.tagHeader
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber


@Composable
fun NoteListScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = koinViewModel(),
    navigateToNoteDetail: (Long) -> Unit,
    onMenuClick: () -> Unit,
) {
    val notes by viewModel.filteredNotes.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val isSearchBarVisible = remember { mutableStateOf(true) }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var showUpdateTagDialog by remember { mutableStateOf(false) }
    var tagToUpdate by remember { mutableStateOf<Tag?>(null) }
    var updatedTagName by remember { mutableStateOf("") }

    val originalNote by viewModel.originalNote.collectAsState()

    // grid layout
    val gridLayout by viewModel.gridLayout.collectAsState()
    var showLayoutDialog by remember { mutableStateOf(false) }


    fun handleAction(action: (Set<Note>) -> Unit, message: String, restoreAction: () -> Unit) {
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

    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) toggleSelection(note)
        else navigateToNoteDetail(note.id)
    }
    HandleOriginalNote(originalNote, snackBarHostState, viewModel)

    Timber.tag("DEBUG").d("[TAGS]=[$tags]")
    var showReminderDialog by remember { mutableStateOf(false) }


    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                SwipeToDismissContentSnack(
                    onSwipeToDismiss = { snackBarHostState.currentSnackbarData?.dismiss() },
                    content = { Snackbar(snackbarData = data) }
                )
            }
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigateToNoteDetail(-1L) },
                modifier = Modifier.padding(bottom = 22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("New Note", fontSize = 15.sp)
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .scrollConnectionToProvideVisibility(isSearchBarVisible)
        ) {
            NoteGridTags(
                notes = notes,
                selectedNotes = selectedNotes,
                onNoteClick = ::handleNoteClick,
                onNoteLongPress = ::toggleSelection,
                theme = theme,
                gridContent = {

                    tagHeader(
                        tags = tags,
                        selectedTagId = selectedTagId,
                        onTagClick = { tagId -> viewModel.toggleTag(tagId) },
                        onTagLongClick = { tag ->
                            tagToUpdate = tag
                            updatedTagName = tag.name
                            showUpdateTagDialog = true
                        },
                        onAddTagClick = { showAddTagDialog = true }
                    )
                },
                gridLayout = gridLayout
            )
            ExpandableSearchView(
                onMenuClick = onMenuClick,
                searchQuery = searchQuery,
                onSearchQueryChanged = viewModel::updateSearchQuery,
                onSearchClosed = {
                    isSearchBarVisible.value = false
                    viewModel.updateSearchQuery("")
                },
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
                isSearchBarVisible = isSearchBarVisible,
                onSetReminder = {
                    showReminderDialog = true
                },
                onGridLayoutClick = { showLayoutDialog = true }
            )
        }
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { tagName ->
                viewModel.addTag(tagName)
                showAddTagDialog = false
            }
        )
    }

    if (showUpdateTagDialog) {
        tagToUpdate?.let { tag ->
            UpdateTagDialog(
                tag = tag,
                onDismiss = { showUpdateTagDialog = false },
                onConfirm = { updatedTag ->
                    viewModel.updateTag(updatedTag)
                    showUpdateTagDialog = false
                },
                onDelete = {
                    viewModel.deleteTag(tag)
                    showUpdateTagDialog = false
                }
            )
        }
    }

    val initialDate = selectedNotes.firstOrNull()?.reminderDate

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
    )
    if (showLayoutDialog) {
        LayoutSelectionDialog(
            currentLayout = gridLayout,
            onLayoutSelected = { grid ->
                viewModel.updateGridLayout(grid)
                showLayoutDialog = false
            },
            onDismiss = { showLayoutDialog = false }
        )
    }
}


@Composable
fun HandleOriginalNote(
    originalNote: Note,
    snackBarHostState: SnackbarHostState,
    viewModel: NoteListViewModel
) {
    LaunchedEffect(originalNote) {
        val message = when {
            originalNote.isTrashed -> "Note moved to trash and unpinned"
            originalNote.isArchived -> "Note archived and unpinned"
            else -> return@LaunchedEffect
        }
        message.let {
            val result = snackBarHostState.showSnackbar(it, "UNDO")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreDeletedArchivedNote()
            }
        }
    }
}

