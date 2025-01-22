package com.despicable.feature.home.screens.trash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.despicable.core.designsystem.component.CollapsedSearchView
import com.despicable.core.designsystem.component.EmptyStateContent
import com.despicable.core.designsystem.component.NoteeDialog
import com.despicable.core.designsystem.component.ScreenType
import com.despicable.core.designsystem.component.SelectionTopBar
import com.despicable.feature.home.NoteGridTags
import com.despicable.feature.home.R
import com.despicable.feature.home.SwipeableSnackBarHost
import com.despicable.core.model.Note
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToDetail: (Long) -> Unit,
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
                    duration = SnackbarDuration.Short
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> viewModel.undoRestore()
                    SnackbarResult.Dismissed -> viewModel.clearLastRestoredNotes()
                }
            }
        }
    }
    // Clear snackBar when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearLastRestoredNotes()
        }
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
            viewModel.clearLastRestoredNotes()
            navigateToDetail(note.id)
        }
    }

    Scaffold(
        snackbarHost = { SwipeableSnackBarHost(snackBarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBarTrash(
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
                        notes = emptyList(),
                        selectedNotes = selectedNotes,
                        onNoteClick = ::handleNoteClick,
                        onNoteLongPress = ::toggleSelection,
                        searchHeightPadding = 0.dp,
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
fun TopBarTrash(
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

    if (selectedNotes.isEmpty()) {
        CollapsedSearchView(
            title = "Trash",
            onMenuClick = onMenuClick,
            screenType = ScreenType.Trash,
            onEmptyTrash = {
                showEmptyTrashDialog = true
            },
            scrollBehavior = scrollBehavior
        )
    } else {
        SelectionTopBar(
            onClearSelection = onClearSelection,
            onRestoreNotes = onRestoreNotes,
            onDeleteNotes = { showDeleteDialog = true },
            selectedNotes = selectedNotes,
            screenType = ScreenType.Trash,
        )
    }


    NoteeDialog(
        enabled = showDeleteDialog,
        title = "Delete note forever",
        description = "Are you sure you want to permanently delete these notes?",
        icon = Icons.Default.DeleteForever,
        confirmText = "Delete",
        dismissText = "Cancel",
        onConfirm = {
            onDeleteNotes()
            showDeleteDialog = false
        },
        onDismiss = {
            showDeleteDialog = false
        }
    )

    NoteeDialog(
        enabled = showEmptyTrashDialog,
        title = "Empty Trash",
        description = "All notes in Trash will be permanently deleted. This action cannot be undone.",
        confirmText = "Empty Trash",
        dismissText = "Cancel",
        onConfirm = {
            onEmptyTrash()
            showEmptyTrashDialog = false
        },
        onDismiss = {
            showEmptyTrashDialog = false
        }
    )

}

