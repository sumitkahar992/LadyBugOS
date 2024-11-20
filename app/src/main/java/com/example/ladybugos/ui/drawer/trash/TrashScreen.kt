package com.example.ladybugos.ui.drawer.trash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.R
import com.example.ladybugos.model.Note
import com.example.ladybugos.ui.components.EmptyStateContent
import com.example.ladybugos.ui.components.NoteGridTags
import com.example.ladybugos.ui.components.SwipeToDismissContentSnack
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
) {
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackBarHostState = remember { SnackbarHostState() }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val lastRestoredNotes by viewModel.lastRestoredNotes.collectAsStateWithLifecycle()

    // grid layout
    val gridLayout by viewModel.gridLayout.collectAsState()

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
                    EmptyStateContent(
                        icon = R.drawable.trash,
                        title = "No trashed notes available"
                    )
                } else {
                    NoteGridTags(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        notes = trashedNotes,
                        selectedNotes = selectedNotes,
                        onNoteClick = ::handleNoteClick,
                        onNoteLongPress = ::toggleSelection,
                        pinnedHeader = false,
                        gridLayout = gridLayout
                    )
                }
            }
        }
    )
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
            title = { Text("Trash", Modifier.padding(start = 16.dp)) },
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

