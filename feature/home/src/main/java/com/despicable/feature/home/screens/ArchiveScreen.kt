package com.despicable.feature.home.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.despicable.core.common.navigation.NoteActionType
import com.despicable.core.designsystem.component.CollapsedSearchView
import com.despicable.core.designsystem.component.ExpandedSearchView
import com.despicable.feature.home.NoteScreenContent
import com.despicable.core.designsystem.component.ReminderDialog
import com.despicable.core.designsystem.component.ScreenType
import com.despicable.core.designsystem.component.SelectionTopBar
import com.despicable.feature.home.HandleNoteActions
import com.despicable.feature.home.NoteGridTags
import com.despicable.feature.home.NoteListViewModel
import com.despicable.feature.home.NoteSnackBarHandler
import com.despicable.feature.home.R
import com.despicable.feature.home.SwipeableSnackBarHost
import com.despicable.core.model.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    navigateToNoteDetail: (Long) -> Unit,
    noteId: Long?,
    actionType: NoteActionType?,
    clearNoteAction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val archivedNotes = uiState.notes.filter { it.note.isArchived && !it.note.isTrashed }
    val snackbarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

    val isSearchBarVisible = rememberSaveable { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()
    var showReminderDialog by remember { mutableStateOf(false) }

    var isSearchMode by rememberSaveable { mutableStateOf(false) }


    fun toggleSelection(note: Note) {
        selectedNotes = selectedNotes.toMutableSet().apply {
            if (contains(note)) remove(note) else add(note)
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current


    // Also clear snackBar when navigating to detail
    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) {
            toggleSelection(note)
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
            viewModel.clearSnackBarMessage()
            navigateToNoteDetail(note.id)
        }
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

    // Handle actions
    HandleNoteActions(
        noteId = noteId,
        actionType = actionType,
        viewModel = viewModel,
        clearAction = { clearNoteAction() },
        isSearchExpanded = isSearchBarVisible.value,
        setSearchExpanded = { isSearchBarVisible.value = it },
        selectedNotes = selectedNotes,
        clearSelectedNotes = { selectedNotes = emptySet() },
        isSearchMode = isSearchMode,
        setSearchMode = { isSearchMode = it }
    )

    // Handle snackBar
    NoteSnackBarHandler(snackbarMessage, snackBarHostState)

    Scaffold(
        snackbarHost = { SwipeableSnackBarHost(snackBarHostState) },
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedTopBar(
                title = "Archive",
                selectedNotes = selectedNotes,
                isSearchActive = isSearchBarVisible.value,
                onSearchActiveChange = { isSearchBarVisible.value = it },
                onClearSelection = { selectedNotes = emptySet() },
                onSetReminder = {
                    showReminderDialog = true
                },
                onPinNotes = {
                    handleAction(
                        viewModel::pinAndUnarchiveNotes,
                        "${selectedNotes.size} notes pinned and un-archived"
                    )
                },
                onUnarchiveNotes = {
                    handleAction(
                        viewModel::unarchiveNotes,
                        "${selectedNotes.size} notes un-archived"
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        NoteScreenContent(
            modifier = modifier,
            paddingValues = padding,
            notes = archivedNotes,
            isInitialized = uiState.isNotesInitialized,
            searchQuery = uiState.searchQuery,
            emptyIcon = R.drawable.archive,
            emptyTitle = "No archived notes available"
        ) {
            NoteGridTags(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                notes = archivedNotes,
                selectedNotes = selectedNotes,
                onNoteClick = ::handleNoteClick,
                onNoteLongPress = ::toggleSelection,
                gridLayout = uiState.gridLayout,
                searchHeightPadding = 0.dp,
                pinnedHeader = false
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedTopBar(
    modifier: Modifier = Modifier,
    title: String = "",
    onMenuClick: () -> Unit,
    selectedNotes: Set<Note>,
    onClearSelection: () -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSetReminder: () -> Unit,
    onPinNotes: () -> Unit,
    onDeleteNotes: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUnarchiveNotes: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val isFocused = remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var topBarSize by remember { mutableStateOf(IntSize.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    val isFocuseds = interactionSource.collectIsFocusedAsState().value
    val shouldClearFocus = !isSearchActive && isFocuseds
    LaunchedEffect(isFocused) {
        if (shouldClearFocus) {
            delay(100)
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = isSearchActive) {
        onSearchActiveChange(false)
    }

//    BackHandler(enabled = searchQuery.isNotBlank() && isFocused.value.not()) {
//        onSearchQueryChange("")
//    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { topBarSize = it }
    ) {

        val transitionState = remember { MutableTransitionState(false) }
        transitionState.targetState = selectedNotes.isNotEmpty() || isSearchActive

        val transition = rememberTransition(transitionState, label = "searchTransition")

        val expandProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 300, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                }
            },
            label = "expandProgress"
        ) { state -> if (state) 1f else 0f }

        CollapsedSearchView(
            modifier = Modifier,
            title = title,
            onMenuClick = onMenuClick,
            onSearchClick = { onSearchActiveChange(true) },
            screenType = ScreenType.Archive,
            scrollBehavior = scrollBehavior,
        )

        /*        SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onSearch = onSearchQueryChange,
                            expanded = false,
                            onExpandedChange = {},
                            enabled = true,
                            placeholder = { Text("Search") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            trailingIcon = {

                            },
                            interactionSource = null,
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 18.dp),
                    shape = SearchBarDefaults.inputFieldShape,
                    tonalElevation = SearchBarDefaults.TonalElevation,
                    shadowElevation = SearchBarDefaults.ShadowElevation,
                    windowInsets = SearchBarDefaults.windowInsets,
                    content =  {},
                )*/

        // Expanded search view and Selection top bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = expandProgress
                    scaleY = 0.8f + (0.2f * expandProgress)
                    translationY = (-20f + (20f * expandProgress)).dp.toPx()
                }
        ) {
            when {
                selectedNotes.isNotEmpty() -> {
                    SelectionTopBar(
                        onClearSelection = onClearSelection,
                        onPinNotes = onPinNotes,
                        onUnpinNotes = onPinNotes,
                        onSetReminder = onSetReminder,
                        onUnarchiveNotes = onUnarchiveNotes,
                        onDeleteNotes = onDeleteNotes,
                        selectedNotes = selectedNotes,
                        screenType = ScreenType.Archive,
                    )
                }

                isSearchActive -> {


                    ExpandedSearchView(
                        modifier = Modifier
                            .padding(horizontal = 12.dp),
                        searchQuery = searchQuery,
                        onSearchQueryChanged = onSearchQueryChange,
                        isFocused = isFocused,
                        focusRequester = focusRequester,
                        focusManager = focusManager,
                        onSearchActiveChange = onSearchActiveChange
                    )

                }
            }

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
        )


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
*/













































