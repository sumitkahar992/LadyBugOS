package com.despicable.feature.home


import androidx.annotation.Keep
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.data.sample.LoadSampleDataUseCase
import com.despicable.core.datastore.SettingsRepo
import com.despicable.core.designsystem.theme.GridLayout
import com.despicable.core.designsystem.theme.Theme
import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import com.despicable.widgets.data.WidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber


@Keep
data class NoteListUiState(
    val isNotesInitialized: Boolean = false,
    val notes: List<NoteWithTagsAndChecklist> = emptyList(),
    val tagState: TagState = TagState(),
    val searchQuery: String = "",
    val gridLayout: GridLayout = GridLayout.TwoColumns,
    val lastModifiedNotes: List<Note>? = null
)

// New data class to combine note, tags, and checklist items
data class NoteWithTagsAndChecklist(
    val note: Note,
    val tags: List<Tag>,
    val checklistItems: List<Checklist> = emptyList()
)

@Keep
data class TagState(
    val availableTags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val activeTagIds: Set<Long> = emptySet() // Tags that have non-trashed notes
)

data class SnackBarMessage(
    val message: String,
    val actionLabel: String = "UNDO",
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onDismiss: () -> Unit = {},
    val onAction: () -> Unit = {}
)

class NoteListViewModel(
    private val settingsRepo: SettingsRepo,
    private val repo: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val loadSampleDataUseCase: LoadSampleDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState = _uiState.asStateFlow()

    private var noteFlowJob: Job? = null

    private val _snackBarMessage = MutableStateFlow<SnackBarMessage?>(null)
    val snackBarMessage = _snackBarMessage.asStateFlow()

    // Add these properties for reminder screens
    val upcomingReminders = repo.getUpcomingRemindersWithTagsAndChecklist()
        .map { noteCompleteList ->
            noteCompleteList.map { noteComplete ->
                NoteWithTagsAndChecklist(
                    note = noteComplete.note,
                    tags = noteComplete.tags,
                    checklistItems = noteComplete.checklistItems
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val completedReminders = repo.getCompletedRemindersWithTagsAndChecklist()
        .map { noteCompleteList ->
            noteCompleteList.map { noteComplete ->
                NoteWithTagsAndChecklist(
                    note = noteComplete.note,
                    tags = noteComplete.tags,
                    checklistItems = noteComplete.checklistItems
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val archivedNotes = repo.getArchivedNotesWithTagsAndChecklist()
        .map { noteCompleteList ->
            noteCompleteList.map { noteComplete ->
                NoteWithTagsAndChecklist(
                    note = noteComplete.note,
                    tags = noteComplete.tags,
                    checklistItems = noteComplete.checklistItems
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())


    init {
        initializeNotes()
        viewModelScope.launch {
            loadSampleDataUseCase()
        }
    }

    private fun initializeNotes() {
        noteFlowJob = viewModelScope.launch {
            combine(
                settingsRepo.get { gridLayout },
                repo.getAllTags(),
                repo.getAllNotesWithTags(),
                _uiState.map { it.searchQuery }
            ) { gridLayout, tags, allNotes, query ->
                // Calculate active tag IDs (tags with non-trashed notes)
                val activeTagIds = allNotes
                    .filter { !it.note.isTrashed }
                    .flatMap { it.tags }
                    .map { it.id }
                    .toSet()

                // Filter and sort notes based on current state
                val filteredNotes = filterNotes(
                    notes = allNotes,
                    query = query,
                    selectedTagId = _uiState.value.tagState.selectedTagId
                )

                // Inside initializeNotes() method, replace the current coroutineScope block with this:

// Transform to NoteWithTagsAndChecklist
                coroutineScope {
                    // Process notes in batches of 20 for better performance
                    val notesWithChecklist = filteredNotes
                        .chunked(20) // Split the list into chunks of 20 notes
                        .flatMap { chunk ->
                            // Process each chunk in parallel
                            chunk.map { noteWithTags ->
                                async {
                                    val checklistItems = if (noteWithTags.note.isChecklist) {
                                        try {
                                            repo.getChecklistItemsByNoteId(noteWithTags.note.id)
                                                .first()
                                        } catch (e: Exception) {
                                            Timber.e(e, "Error loading checklist items")
                                            emptyList()
                                        }
                                    } else {
                                        emptyList()
                                    }
                                    NoteWithTagsAndChecklist(
                                        note = noteWithTags.note,
                                        tags = noteWithTags.tags,
                                        checklistItems = checklistItems
                                    )
                                }
                            }.awaitAll() // Wait for all notes in this chunk to complete
                        }

                    _uiState.update { current ->
                        current.copy(
                            isNotesInitialized = true,
                            notes = notesWithChecklist,
                            gridLayout = gridLayout,
                            tagState = current.tagState.copy(
                                availableTags = tags,
                                activeTagIds = activeTagIds
                            )
                        )
                    }
                }
                /*
                                // Transform to NoteWithTagsAndChecklist
                                coroutineScope {
                                    val notesWithChecklist = filteredNotes.map { noteWithTags ->
                                        async {
                                            val checklistItems = if (noteWithTags.note.isChecklist) {
                                                try {
                                                    repo.getChecklistItemsByNoteId(noteWithTags.note.id).first()
                                                } catch (e: Exception) {
                                                    Timber.e(e, "Error loading checklist items")
                                                    emptyList()
                                                }
                                            } else {
                                                emptyList()
                                            }
                                            NoteWithTagsAndChecklist(
                                                note = noteWithTags.note,
                                                tags = noteWithTags.tags,
                                                checklistItems = checklistItems
                                            )
                                        }
                                    }.awaitAll()


                                    _uiState.update { current ->
                                        current.copy(
                                            isNotesInitialized = true,
                                            notes = notesWithChecklist,
                                            gridLayout = gridLayout,
                                            tagState = current.tagState.copy(
                                                availableTags = tags,
                                                activeTagIds = activeTagIds
                                            )
                                        )
                                    }
                                }
                                */

            }.collect()
        }
    }

    private fun filterNotes(
        notes: List<NoteWithTags>,
        query: String,
        selectedTagId: Long?
    ): List<NoteWithTags> {
        return notes
            .asSequence()
            .filter { noteWithTags ->
                val note = noteWithTags.note
                val matchesQuery = query.isEmpty() || note.matchesSearch(query)

                val matchesTag = selectedTagId == null ||
                        noteWithTags.tags.any { it.id == selectedTagId }


                matchesQuery && matchesTag
            }
            .sortedWith(
                compareByDescending<NoteWithTags> { it.note.isPinned }
                    .thenByDescending { it.note.pinnedDate }
            )
            .toList()
    }

    fun clearSnackBarMessage() {
        _snackBarMessage.value = null
    }


    fun handleNoteAction(action: NoteAction) {
        viewModelScope.launch {
            try {
                // Store original state for undo
                val originalNote =
                    repo.getNoteById(action.noteId).firstOrNull() ?: return@launch

                // Update database based on action
                when (action) {
                    is NoteAction.Archive -> archiveNotes(
                        listOf(
                            originalNote
                        )
                    )

                    is NoteAction.Delete -> trashNotes(
                        listOf(
                            originalNote
                        )
                    )

                    is NoteAction.Unarchive -> unarchiveNotes(
                        listOf(originalNote)
                    )
                }

                // Show snackBar with undo option
                _snackBarMessage.value = SnackBarMessage(
                    message = when (action) {
                        is NoteAction.Archive -> "Note archived"
                        is NoteAction.Delete -> "Note moved to trash"
                        is NoteAction.Unarchive -> "Note unarchived"
                    },
                    onDismiss = {
                        _snackBarMessage.value = null
                        _uiState.update { it.copy(lastModifiedNotes = null) }
                    },
                    onAction = {
                        viewModelScope.launch {
                            when (action) {
                                is NoteAction.Archive -> unarchiveNotes(
                                    listOf(originalNote)
                                )

                                is NoteAction.Delete -> {
                                    updateNotes(
                                        notes = listOf(originalNote),
                                        updates = { it.copy(isTrashed = false) }
                                    )
                                }

                                is NoteAction.Unarchive -> archiveNotes(
                                    listOf(originalNote)
                                )
                            }
                            _snackBarMessage.value = null
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error handling note action")
            }
        }
    }


    private fun updateNotes(
        notes: List<Note>,
        updates: (Note) -> Note,
        delayTime: Long = 400
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(lastModifiedNotes = notes) }
                val updatedNotes = notes.map(updates)
                delay(delayTime)

                // Update database
                repo.updateNotes(updatedNotes)

                // Update widgets
                widgetUpdater.updateWidgetsForNotes(updatedNotes)

                // Refresh notes list to ensure UI is in sync
                initializeNotes()

            } catch (e: Exception) {
                Timber.e(e, "Error updating notes")
            }
        }
    }


    fun toggleTag(tagId: Long) {
        viewModelScope.launch {
            _uiState.update { current ->
                val newSelectedId = if (current.tagState.selectedTagId == tagId) null else tagId
                current.copy(
                    tagState = current.tagState.copy(selectedTagId = newSelectedId)
                )
            }
            // Re-filter notes when tag is toggled
            initializeNotes()
        }
    }


    fun updateSearchQuery(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query) }
        }
    }

    fun updateTheme(theme: Theme) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
    }


    fun updateGridLayout(gridLayout: GridLayout) {
        viewModelScope.launch { settingsRepo.setGridLayout(gridLayout) }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            repo.updateNoteReminder(noteId, reminderDate)
        }
    }

    fun pinNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = {
            it.copy(
                isPinned = true,
                pinnedDate = System.currentTimeMillis()
            )
        }
    )

    fun unpinNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = {
            it.copy(
                isPinned = false,
                pinnedDate = System.currentTimeMillis()
            )
        }
    )

    fun archiveNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = {
            it.copy(
                isArchived = true,
                isPinned = false,
                pinnedDate = null,
            )
        },
        delayTime = 500
    )

    fun unarchiveNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = { it.copy(isArchived = false) },
        delayTime = 500
    )

    fun trashNotes(notes: List<Note>) =
        updateNotes(
            notes = notes,
            updates = {
                it.copy(
                    isTrashed = true,
                    isPinned = false,
                    pinnedDate = null
                )
            },
            delayTime = 500
        )


    fun pinAndUnarchiveNotes(notes: List<Note>) =
        updateNotes(
            notes = notes,
            updates = {
                it.copy(
                    isPinned = true,
                    pinnedDate = System.currentTimeMillis(),
                    isArchived = false
                )
            }
        )

    fun undoLastOperation() {
        viewModelScope.launch {
            _uiState.value.lastModifiedNotes?.let { notes ->
                updateNotes(
                    notes,
                    updates = { it.copy() }
                )
                widgetUpdater.updateWidgetsForNotes(notes)
                _uiState.update { it.copy(lastModifiedNotes = null) }
            }
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            repo.insertTag(Tag(name = name))
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            repo.updateTag(tag)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repo.deleteTag(tag)
        }
    }

    override fun onCleared() {
        super.onCleared()
        noteFlowJob?.cancel() // Clean up when ViewModel is destroyed
    }
}

/*

@OptIn(ExperimentalMaterial3Api::class)
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


    val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()


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

    // Also clear snackBar when navigating to detail
    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) {
            toggleSelection(note)
        } else {
            viewModel.clearSnackBarMessage()
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
        clearSelectedNotes = { selectedNotes = emptySet() }
    )

    // Handle snackBar
    NoteSnackBarHandler(snackBarMessage, snackBarHostState)

    Scaffold(
        snackbarHost = { SwipeableSnackBarHost(snackBarHostState) },
        modifier = modifier.scrollConnectionToProvideVisibility(isSearchBarVisible),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        floatingActionButton = { NoteFAB(navigateToDetail) },
    ) { padding ->
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

        ExpandableSearchView(
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
            isSearchBarVisible = isSearchBarVisible,
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
            scrollBehavior = null
        )
    }

    val initialDate = selectedNotes.firstOrNull()?.reminderDate

    NoteeDialog(
        enabled = showThemeDialog,
        title = "Choose Theme",
        onDismiss = { showThemeDialog = false },
        description = {
            Column(Modifier.selectableGroup()) {
                ThemeOption(Theme.Light, theme) {
                    viewModel.updateTheme(Theme.Light)
                    showThemeDialog = false
                }
                ThemeOption(Theme.Dark, theme) {
                    viewModel.updateTheme(Theme.Dark)
                    showThemeDialog = false
                }
                ThemeOption(Theme.System, theme) {
                    viewModel.updateTheme(Theme.System)
                    showThemeDialog = false
                }
            }
        }
    )

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

*/


