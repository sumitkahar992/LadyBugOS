package com.despicable.feature.detail


import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.data.repository.ChecklistRepository
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.designsystem.colorPalette
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import com.despicable.feature.detail.navigation.DetailRoute
import com.despicable.widgets.data.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class NoteDetailViewModel(
    private val repo: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    savedStateHandle: SavedStateHandle,
    private val checklistRepo: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _noteUpdateTrigger = MutableSharedFlow<NoteUpdatePayload>()

    private var nextChecklistItemId = 0L


    init {
        val noteId = savedStateHandle.toRoute<DetailRoute>().id

        if (noteId > 0) {
            loadNoteById(noteId)   // Load existing note
        } else {
            updateUiState {       // Initialize new note
                it.initializeNewNote()
            }
        }

        fetchTags()
        setupNoteUpdateFlow()
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            repo.getNoteWithTagsById(id).catch { e -> Timber.e(e, "Error loading note") }
                .firstOrNull()?.let { noteWithTags ->
                    updateUiState { it.fromNoteWithTags(noteWithTags).copy(isLoading = false) }

                    // Start collecting checklist items immediately
                    checklistRepo.getChecklistItemsFlow(id)
                        .catch { e -> Timber.e(e, "Error loading checklist items") }
                        .collect { items ->
                            _uiState.update { it.copy(checklistItems = items) }
                        }

                }
        }
    }

    fun updateChecklistItem(position: Int, newContent: String) {
        viewModelScope.launch {
            try {
                val item = _uiState.value.checklistItems.getOrNull(position) ?: return@launch
                checklistRepo.updateChecklistItem(item, newContent)
            } catch (e: Exception) {
                Timber.e(e, "Error updating checklist item")
            }
        }
    }

    fun toggleChecklist() {
        viewModelScope.launch {
            try {
                val newChecklistState = !_uiState.value.isCheckList
                _uiState.update { it.copy(isCheckList = newChecklistState) }
                repo.updateNoteChecklist(_uiState.value.id, newChecklistState)
            } catch (e: Exception) {
                Timber.e(e, "Error toggling checklist status")
                _uiState.update { it.copy(isCheckList = !it.isCheckList) }
            }
        }
    }

    fun addChecklistItem(content: String = "") {
        viewModelScope.launch {
            try {
                checklistRepo.addChecklistItem(_uiState.value.id, content)
            } catch (e: Exception) {
                Timber.e(e, "Error adding checklist item")
            }
        }
    }

    fun toggleChecklistItem(position: Int) {
        viewModelScope.launch {
            val item = _uiState.value.checklistItems.getOrNull(position) ?: return@launch
            try {
                checklistRepo.toggleChecklistItem(item)
            } catch (e: Exception) {
                Timber.e(e, "Error toggling checklist item")
            }
        }
    }

    fun removeChecklistItem(position: Int) {
        viewModelScope.launch {
            val item = _uiState.value.checklistItems.getOrNull(position) ?: return@launch
            try {
                checklistRepo.deleteChecklistItem(item)
            } catch (e: Exception) {
                Timber.e(e, "Error removing checklist item")
            }
        }
    }

    fun reorderChecklistItems(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            try {
                checklistRepo.reorderChecklistItems(_uiState.value.id, fromPosition, toPosition)
            } catch (e: Exception) {
                Timber.e(e, "Error reordering checklist items")
            }
        }
    }


    /*        fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
                viewModelScope.launch {
                    try {
                        val noteWithTags = noteRepository.getNoteWithTagsById(noteId).firstOrNull()
                        updateUiState { it.fromNoteWithTags(noteWithTags) }

                    } catch (e: Exception) {
                        Timber.e(e, "Error preloading note data")
                        // Optionally, you could add error handling in the UI state if needed:
                        // updateUiState { it.copy(error = e.localizedMessage) }
                    }
                }
            }*/


    @OptIn(FlowPreview::class)
    private fun setupNoteUpdateFlow() {
        viewModelScope.launch {
            _noteUpdateTrigger.debounce(500L).distinctUntilChanged().flowOn(Dispatchers.Default)
                .catch { e ->
                    Timber.e(e, "Error in note update flow")
                }.collect { payload ->
                    try {
                        repo.updateNoteWithTags(payload.note, payload.tagIds)
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating note")
                    }
                }
        }
    }

    fun updateNoteTitle(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                title = newValue.text,
                titleSelection = newValue.selection,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
    }

    fun updateNoteContent(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                content = newValue.text,
                contentSelection = newValue.selection,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
    }

    private fun updateUiStateAndTriggerSave(update: (NoteUiState) -> NoteUiState) {
        updateUiState(update)
        viewModelScope.launch {
            val currentState = _uiState.value
            try {
                // If it's a new note (id = 0), insert it first
                if (currentState.id == 0L) {
                    val noteId = repo.insertNoteWithTags(
                        currentState.toNote(),
                        currentState.selectedTagIds.toList()
                    )
                    // Update the UI state with the new ID
                    updateUiState { it.copy(id = noteId) }
                } else {
                    // Existing note, update as normal
                    _noteUpdateTrigger.emit(
                        NoteUpdatePayload(
                            note = currentState.toNote(),
                            tagIds = currentState.selectedTagIds.toList()
                        )
                    )
                    widgetUpdater.updateSingleWidget(currentState.toNote())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving/updating note")
            }
        }
    }

    fun toggleTag(tagId: Long) {
        updateUiStateAndTriggerSave {
            it.copy(selectedTagIds = it.selectedTagIds.toggle(tagId))
        }
    }

    fun togglePinStatus() {
        updateUiStateAndTriggerSave { currentState ->
            val updatedNote = currentState.toNote().copy(isPinned = !currentState.isPinned)
            currentState.copy(
                isPinned = updatedNote.isPinned
            )
        }
    }

    fun updateColor(newColor: Int?) {
        updateUiStateAndTriggerSave { currentState ->
            currentState.copy(
                lightColor = newColor ?: currentState.lightColor,
            )
        }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            repo.updateNoteReminder(noteId, reminderDate)
            repo.getNoteById(noteId).firstOrNull()?.let { updatedNote ->
                updateUiStateAndTriggerSave {
                    it.copy(
                        reminderDate = updatedNote.reminderDate,
                        id = updatedNote.id,
                        isDone = updatedNote.isDone,
                    )
                }
            }
        }
    }

    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = _uiState.value.toNote()
            if (currentNote.title.isBlank() && currentNote.content.isBlank()) {
                onSkip()
            } else {
                saveOrUpdateNote(currentNote)
                onComplete()
            }
        }
    }

    private suspend fun saveOrUpdateNote(note: Note) {
        if (note.id == 0L) {
            repo.insertNoteWithTags(note, _uiState.value.selectedTagIds.toList())
        } else {
            _noteUpdateTrigger.emit(
                NoteUpdatePayload(
                    note = note, tagIds = _uiState.value.selectedTagIds.toList()
                )
            )
            widgetUpdater.updateSingleWidget(note)

        }
    }

    fun handleNoteAction(
        action: NoteAction,
        onComplete: (noteId: Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val noteId = _uiState.value.id

                // Update UI state based on action type
                updateUiState {
                    when (action) {
                        is NoteAction.Delete -> it.copy(
                            isTrashed = true,
                            isPinned = false
                        )

                        is NoteAction.Archive -> it.copy(
                            isArchived = true,
                            isPinned = false
                        )

                        is NoteAction.Unarchive -> it.copy(isArchived = false)
                    }
                }
                // Navigate with action
                onComplete(noteId)
            } catch (e: Exception) {
                Timber.e(e, "Error handling note action: ${action.message}")
                // Revert UI state on error
                updateUiState {
                    it.copy(
                        isArchived = _uiState.value.isArchived,
                        isTrashed = _uiState.value.isTrashed,
                    )
                }
            }
        }
    }

    fun deleteNoteIfEmpty() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isEmpty()) {
                repo.deleteNote(currentState.toNote())
            }
        }
    }


    private fun fetchTags() {
        viewModelScope.launch {
            repo.getAllTags().catch { e -> Timber.e(e, "Error fetching tags") }
                .collect { tags ->
                    updateUiState { it.copy(allTags = tags) }
                }
        }
    }

    private fun updateUiState(update: (NoteUiState) -> NoteUiState) {
        _uiState.update(update)

    }

    private fun Set<Long>.toggle(id: Long) = if (contains(id)) minus(id) else plus(id)

    private data class NoteUpdatePayload(
        val note: Note, val tagIds: List<Long>
    )

    fun deleteNoteForever(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                repo.deleteNote(currentNote)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting note permanently")
            }
        }
    }

    fun restoreFromTrash(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                val restoredNote = currentNote.copy(isTrashed = false)
                repo.updateNotes(listOf(restoredNote))
                updateUiState { it.copy(isTrashed = false) }
                widgetUpdater.updateSingleWidget(restoredNote)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Error restoring note from trash")
            }
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                val trashedNote = currentNote.copy(isTrashed = true)
                repo.updateNotes(listOf(trashedNote))
                updateUiState { it.copy(isTrashed = true) }
                widgetUpdater.updateSingleWidget(trashedNote)
            } catch (e: Exception) {
                Timber.e(e, "Error undoing restore")
            }
        }
    }
}


// Main UI state
data class NoteUiState(
    val isLoading: Boolean = true,
    val id: Long = 0,
    val title: String = "",
    val titleSelection: TextRange = TextRange(0),
    val content: String = "",
    val contentSelection: TextRange = TextRange(0),
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Long? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Long? = null,
    val isDone: Boolean = false,
    val updateDate: String = "",
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val isCheckList: Boolean = false,
    val checklistItems: List<ChecklistEntity> = emptyList()
) {
    fun toNote() = Note(
        id = id,
        title = title,
        content = content,
        lightColor = lightColor,
        isPinned = isPinned,
        pinnedDate = pinnedDate,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        updateDate = updateDate,
        isChecklist = isCheckList
    )

    fun isEmpty() = title.isBlank() && content.isBlank()

    fun fromNoteWithTags(noteWithTags: NoteWithTags?) =
        noteWithTags?.let {
            copy(
                id = it.note.id,
                title = it.note.title,
                content = it.note.content,
                lightColor = it.note.lightColor,
                isPinned = it.note.isPinned,
                pinnedDate = it.note.pinnedDate,
                isArchived = it.note.isArchived,
                isTrashed = it.note.isTrashed,
                reminderDate = it.note.reminderDate,
                isDone = it.note.isDone,
                updateDate = it.note.updateDate,
                selectedTagIds = it.tags.map { tag -> tag.id }.toSet(),
                isCheckList = it.note.isChecklist,
            )
        } ?: this

    private fun toTextFieldValue(text: String, selection: TextRange) = TextFieldValue(
        text = text, selection = selection
    )

    val titleFieldValue: TextFieldValue
        get() = toTextFieldValue(title, titleSelection)

    val contentFieldValue: TextFieldValue
        get() = toTextFieldValue(content, contentSelection)

    companion object {
        fun getCurrentFormattedDate(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        private fun generateRandomColor(): Int = colorPalette.random().toArgb()
    }

    // Add initialization logic for new notes
    fun initializeNewNote() = copy(
        // lightColor = generateRandomColor(),
        lightColor = 0, // Default color (0 means use theme color)
        updateDate = getCurrentFormattedDate(),
        pinnedDate = System.currentTimeMillis(),
        isLoading = false
    )
}


data class ChecklistItemUiState(
    val id: Long = 0L,
    val noteId: Long = 0L,  // Add noteId field
    val content: TextFieldValue = TextFieldValue(""),
    val isChecked: Boolean = false,
    val position: Int = 0
) {
    fun toEntity() = ChecklistEntity(
        id = id,  // Map id to id
        noteId = noteId,  // Map noteId to noteId
        content = content.text,
        isChecked = isChecked,
        position = position
    )
}

fun ChecklistEntity.toUiState() = ChecklistItemUiState(
    id = id,  // Map id to id
    noteId = noteId,  // Map noteId to noteId
    content = TextFieldValue(content),
    isChecked = isChecked,
    position = position
)


