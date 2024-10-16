package com.example.ladybugos.ui.drawer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.Note
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.presentation.NoteGrid
import com.example.ladybugos.ui.presentation.WidgetUpdater
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
) {
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackBarHostState = remember { SnackbarHostState() }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val lastRestoredNotes by viewModel.lastRestoredNotes.collectAsStateWithLifecycle()


    LaunchedEffect(lastRestoredNotes) {
        lastRestoredNotes?.let { restoredNotes ->
            if (restoredNotes.isNotEmpty()) {
                val result = snackBarHostState.showSnackbar(
                    message = "${restoredNotes.size} notes restored",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Long
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> viewModel.undoRestore()
                    SnackbarResult.Dismissed -> viewModel.clearLastRestoredNotes()
                }
            }
        }
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

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                snackbar = { data ->
                    SwipeToDismissContentSnack(
                        onSwipeToDismiss = {
                            snackBarHostState.currentSnackbarData?.dismiss()
                        },
                        content = {
                            Snackbar(
                                snackbarData = data
                            )
                        }
                    )
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SelectionTopBarTrash(
                onClearSelection = { selectedNotes = emptySet() },
                onDeleteNotes = {
                    viewModel.deleteNotesPermanently(selectedNotes.toList())
                    selectedNotes = emptySet()
                },
                onRestoreNotes = {
                    viewModel.restoreNotes(selectedNotes.toList())
                    selectedNotes = emptySet()
                },
                onEmptyTrash = viewModel::emptyTrash,
                selectedNotes = selectedNotes,
                scrollBehavior = scrollBehavior,
                onMenuClick = onMenuClick
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (trashedNotes.isEmpty()) {
                    Text(
                        text = "No trashed notes available",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize()
                    )
                } else {
                    NoteGrid(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        notes = trashedNotes,
                        selectedNotes = selectedNotes,
                        onNoteClick = ::handleNoteClick,
                        onNoteLongPress = ::toggleSelection,
                        theme = theme,
                        searchHeightPadding = 0.dp,
                        pinnedHeader = false
                    )
                }
            }
        }
    )
}

class TrashViewModel(
    private val noteRepository: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    repo: SettingsRepo,
) : ViewModel() {

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())


    val trashedNotes: StateFlow<List<Note>> = _allNotes.map { notes ->
        notes.filter { it.isTrashed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val theme: StateFlow<Theme> = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

    private val _lastRestoredNotes = MutableStateFlow<List<Note>?>(null)
    val lastRestoredNotes: StateFlow<List<Note>?> = _lastRestoredNotes.asStateFlow()

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes.collectLatest { notesList ->
                _allNotes.value = notesList
                updateWidgets()
            }
        }
    }

    fun deleteNotesPermanently(notes: List<Note>) {
        viewModelScope.launch {
            notes.forEach {
                noteRepository.deleteNote(it)
            }
            updateWidgets()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
            updateWidgets()
        }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notes.map { it.copy(isTrashed = false) }
            noteRepository.updateNotes(updatedNotes)
            _lastRestoredNotes.value = notes
            updateWidgets()
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            _lastRestoredNotes.value?.let { notes ->
                val updatedNotes = notes.map { it.copy(isTrashed = true) }
                noteRepository.updateNotes(updatedNotes)
                _lastRestoredNotes.value = null
                updateWidgets()
            }
        }
    }

    fun clearLastRestoredNotes() {
        _lastRestoredNotes.value = null
    }

    private suspend fun updateWidgets() {
        widgetUpdater.updateAllWidgets(_allNotes.value)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBarTrash(
    onClearSelection: () -> Unit,
    onMenuClick: () -> Unit,
    onRestoreNotes: () -> Unit,
    onDeleteNotes: () -> Unit,
    onEmptyTrash: () -> Unit,
    selectedNotes: Set<Note>,
    scrollBehavior: TopAppBarScrollBehavior
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    if (selectedNotes.isEmpty()) {
        TopAppBar(
            title = { Text("Trash") },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Empty Trash") },
                        onClick = {
                            showMoreMenu = false
                            showEmptyTrashDialog = true
                        }
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            title = { Text("${selectedNotes.size} selected") },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }
            },
            actions = {
                IconButton(onClick = onRestoreNotes) {
                    Icon(Icons.Filled.Restore, contentDescription = "Restore notes")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete notes")
                }
            },
            scrollBehavior = scrollBehavior
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note forever") },
            text = { Text("Are you sure you want to permanently delete ${if (selectedNotes.size == 1) "this note" else "these notes"}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNotes()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("All notes in Trash will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyTrash()
                        showEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Empty Trash", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEmptyTrashDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SwipeToDismissContentSnack(
    onSwipeToDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                    coroutineScope.launch {
                        onSwipeToDismiss()
                    }
                }

                SwipeToDismissBoxValue.Settled -> {}
            }
            false
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {},
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        content = { content() },
    )
}