package com.example.ladybugos.ui.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.model.Note
import com.example.ladybugos.ui.presentation.ExpandedSearchView
import com.example.ladybugos.ui.presentation.NoteGrid
import com.example.ladybugos.ui.presentation.NotesViewModel
import com.example.ladybugos.ui.presentation.ScreenType
import com.example.ladybugos.ui.presentation.SelectionTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    viewModel: NotesViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
) {
    val notes by viewModel.archivedNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    fun toggleSelection(note: Note) {
        selectedNotes = selectedNotes.toMutableSet().apply {
            if (contains(note)) remove(note) else add(note)
        }
    }

    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) toggleSelection(note)
        else navigateToNoteDetail(note.id)
    }

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
                onPinNotes = {
                    handleAction(
                        { viewModel.pinAndUnarchiveNotes(it.toList()) },
                        "${selectedNotes.size} notes pinned and un-archived",
                        viewModel::restoreLastPinnedUnArchivedNotes
                    )
                },
                onUnarchiveNotes = {
                    handleAction(
                        { viewModel.unarchiveNotes(it.toList()) },
                        "${selectedNotes.size} notes un-archived",
                        viewModel::restoreLastUnArchivedNotes
                    )
                },
                onDeleteNotes = {
                    handleAction(
                        { viewModel.trashNotes(it.toList()) },
                        "${selectedNotes.size} notes moved to trash",
                        viewModel::restoreLastDeletedNotes
                    )
                },
                title = "Archive",
                onMenuClick = onMenuClick,
                onSearchActiveChange = { isSearchActive = it },
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                scrollBehavior = scrollBehavior,
                suggestions = emptyList(),
                onSuggestionSelected = viewModel::updateSearchQuery,
                onSearchClosed = {
                    isSearchActive = false
                    viewModel.updateSearchQuery("")
                }
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (notes.isEmpty()) {
                    Text(
                        text = if (searchQuery.isBlank()) "No archived notes available"
                        else "No notes found",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize()
                    )
                } else {
                    NoteGrid(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        notes = notes,
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

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    viewModel: NotesViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
) {
    val notes by viewModel.archivedNotes.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val searchQuery by viewModel.searchQuery.collectAsState()


    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) {
            selectedNotes =
                if (note in selectedNotes) selectedNotes - note else selectedNotes + note
        } else {
            navigateToNoteDetail(note.id)
        }
    }

    fun toggleSelection(note: Note) {
        selectedNotes =
            if (note in selectedNotes) selectedNotes - note else selectedNotes + note
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
            AnimatedTopBar(
                selectedNotes = selectedNotes,
                isSearchActive = isSearchActive,
                onClearSelection = { selectedNotes = emptySet() },
                onPinNotes = {
                    viewModel.pinAndUnarchiveNotes(selectedNotes.toList())
                    // Handle multiple pinned and un-archived notes
                    viewModel.lastPinnedUnArchivedNotes?.takeIf { it.isNotEmpty() }?.let {
                        scope.launch {
                            val result = snackBarHostState.showSnackbar(
                                message = "${it.size} notes pinned and un-archived",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Long

                            )
                            if (result == SnackbarResult.ActionPerformed)
                                viewModel.restoreLastPinnedUnArchivedNotes()
                        }
                    }
                    selectedNotes = emptySet()
                },
                onUnarchiveNotes = {
                    viewModel.unarchiveNotes(selectedNotes.toList())
                    // Handle multiple un-archived notes
                    viewModel.lastUnArchivedNotes?.takeIf { it.isNotEmpty() }?.let {
                        scope.launch {
                            val result = snackBarHostState.showSnackbar(
                                message = "${it.size} notes un-archived",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Long

                            )
                            if (result == SnackbarResult.ActionPerformed)
                                viewModel.restoreLastUnArchivedNotes()
                        }
                    }

                    selectedNotes = emptySet()
                },
                onDeleteNotes = {
                    viewModel.trashNotes(selectedNotes.toList())
                    // Handle multiple deleted notes
                    viewModel.lastDeletedNotes?.takeIf { it.isNotEmpty() }?.let {
                        scope.launch {
                            val result = snackBarHostState.showSnackbar(
                                message = "${it.size} notes moved to trash",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Long

                            )
                            if (result == SnackbarResult.ActionPerformed)
                                viewModel.restoreLastDeletedNotes()

                        }
                    }
                    selectedNotes = emptySet()
                },
                title = "Archive",
                onMenuClick = onMenuClick,
                onSearchActiveChange = { isSearchActive = it },
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    viewModel.updateSearchQuery(it)

                },
                scrollBehavior = scrollBehavior,
                suggestions = emptyList(),
                onSuggestionSelected = { suggestion ->
                    viewModel.updateSearchQuery(suggestion)
                },
                onSearchClosed = {
                    isSearchActive = false
                    viewModel.updateSearchQuery("")
                }
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (notes.isEmpty()) {
                    Text(
                        text = if (searchQuery.isBlank()) "No archived notes available" else "No notes found",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize()
                    )
                } else {
                    NoteGrid(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        notes = notes,
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
}*/
/*

// Display the snackbar for restoring single and multiple notes
RestoreArchiveSnackBar(
scope = scope,
snackBarHostState = snackBarHostState,
lastDeletedNotes = viewModel.lastDeletedNotes,
lastUnArchivedNotes = viewModel.lastUnArchivedNotes,
lastPinnedUnArchivedNotes = viewModel.lastPinnedUnArchivedNotes,
onUndoDelete = { viewModel.restoreLastDeletedNotes() },
onUndoPinnedUnArchive = { viewModel.restoreLastPinnedUnArchivedNotes() },
onUndoUnArchive = { viewModel.restoreLastUnArchivedNotes() }
)


@Composable
private fun RestoreArchiveSnackBar(
    scope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    lastDeletedNotes: List<Note>?,
    lastUnArchivedNotes: List<Note>?,
    lastPinnedUnArchivedNotes: List<Note>?,
    onUndoDelete: () -> Unit,
    onUndoPinnedUnArchive: () -> Unit,
    onUndoUnArchive: () -> Unit,
    ) {
    LaunchedEffect(lastDeletedNotes, lastUnArchivedNotes, lastPinnedUnArchivedNotes) {
        // Handle multiple deleted notes
        lastDeletedNotes?.takeIf { it.isNotEmpty() }?.let {
            scope.launch {
                val result = snackBarHostState.showSnackbar(
                    message = "${it.size} note(s) moved to trash",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Long

                )
                if (result == SnackbarResult.ActionPerformed) onUndoDelete()
            }
        }

        // Handle multiple un-archived notes
        lastUnArchivedNotes?.takeIf { it.isNotEmpty() }?.let {
            scope.launch {
                val result = snackBarHostState.showSnackbar(
                    message = "${it.size} note(s) un-archived",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Long

                )
                if (result == SnackbarResult.ActionPerformed) onUndoUnArchive()
            }
        }

        // Handle multiple pinned and un-archived notes
        lastPinnedUnArchivedNotes?.takeIf { it.isNotEmpty() }?.let {
            scope.launch {
                val result = snackBarHostState.showSnackbar(
                    message = "${it.size} note(s) archived and unpinned",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Long

                )
                if (result == SnackbarResult.ActionPerformed) onUndoPinnedUnArchive()
            }
        }
    }
}
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedTopBar(
    selectedNotes: Set<Note>,
    isSearchActive: Boolean,
    onClearSelection: () -> Unit,
    onPinNotes: () -> Unit,
    onUnarchiveNotes: () -> Unit,
    onDeleteNotes: () -> Unit,
    title: String,
    onMenuClick: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String) -> Unit = {},
    onSearchClosed: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100) // Add a small delay before focusing
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    val transition = updateTransition(
        targetState = when {
            selectedNotes.isNotEmpty() -> TopBarState.SELECTION
            isSearchActive -> TopBarState.SEARCH
            else -> TopBarState.NORMAL
        },
        label = "TopBar Transition"
    )


    var topBarSize by remember { mutableStateOf(IntSize.Zero) }
//    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { topBarSize = it }
    ) {

        transition.AnimatedContent(
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            }
        ) { targetState ->
            when (targetState) {
                TopBarState.SELECTION -> SelectionTopBar(
                    onClearSelection = onClearSelection,
                    selectedNotes = selectedNotes,
                    scrollBehavior = scrollBehavior,
                    onPinNotes = onPinNotes,
                    onUnarchiveNotes = onUnarchiveNotes,
                    onDeleteNotes = onDeleteNotes,
                    screenType = ScreenType.Archive,
                    onSetReminder = {},
                )

                TopBarState.NORMAL -> NormalTopBar(
                    title = title,
                    onMenuClick = onMenuClick,
                    onSearchClick = { onSearchActiveChange(true) },
                    scrollBehavior = scrollBehavior
                )

                TopBarState.SEARCH -> {
                    // Empty composable for SEARCH state
//                    Box(modifier = Modifier.height(with(density) { topBarSize.height.toDp() }))
                    ExpandedSearchView(
                        searchQuery = searchQuery,
                        onSearchQueryChanged = onSearchQueryChange,
                        onBackClick = {
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                        },
                        onSearchClosed = onSearchClosed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .focusRequester(focusRequester)
                    )
                }
            }
        }

        /*
                // SearchBar Overlay
                AnimatedSearchBarOverlay(
                    isVisible = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchActiveChange = onSearchActiveChange,
                    suggestions = suggestions,
                    onSuggestionSelected = onSuggestionSelected
                )*/
    }
}

/*
@Composable
fun AnimatedSearchBarOverlay(
    isVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = ""
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isVisible) 1f else 0f)
    ) {
        if (isVisible || alpha > 0f) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                CollapsibleSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchActiveChange = onSearchActiveChange,
                    suggestions = suggestions,
                    onSuggestionSelected = onSuggestionSelected
                )
            }
        }
    }
}

*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestionSelected: (String) -> Unit = {}
) {
    // Ensure the search bar overlays smoothly without pushing content
    Surface(
        modifier = Modifier
            .fillMaxWidth(), tonalElevation = 4.dp
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { newQuery -> onSearchQueryChange(newQuery) },
                    onSearch = { onSearchActiveChange(false) },
                    expanded = true,
                    onExpandedChange = { if (!it) onSearchActiveChange(false) },
                    placeholder = { Text("Search notes") },
                    leadingIcon = {
                        IconButton(onClick = { onSearchActiveChange(false) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            expanded = true,
            onExpandedChange = { active -> if (!active) onSearchActiveChange(false) },
            modifier = Modifier.fillMaxWidth(),
            colors = SearchBarDefaults.colors(),
            content = {
                LazyColumn {
                    items(suggestions) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onSuggestionSelected = {
                                onSuggestionSelected(it)
                                onSearchQueryChange(it)
                            }
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun SuggestionItem(
    suggestion: String,
    onSuggestionSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onSuggestionSelected(suggestion) }
    ) {
        Text(
            text = suggestion,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        title = { Text(title, Modifier.padding(start = 16.dp)) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
        },
        scrollBehavior = scrollBehavior
    )
}

enum class TopBarState {
    NORMAL, SELECTION, SEARCH
}















































