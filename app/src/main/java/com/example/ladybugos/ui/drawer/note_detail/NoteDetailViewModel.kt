package com.example.ladybugos.ui.drawer.note_detail


import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.glance.GlanceId
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.navigation.NoteAction
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.repository.NoteRepository
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
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _noteUpdateTrigger = MutableSharedFlow<NoteUpdatePayload>()


    init {
        val noteId = savedStateHandle.toRoute<Screen.NoteDetail>().id

        if (noteId > 0) {
            loadNoteById(noteId)   // Load existing note
        } else {
            updateUiState {       // Initialize new note
                it.initializeNewNote().copy(isLoading = false)
            }
        }

        fetchTags()
        setupNoteUpdateFlow()
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            noteRepository.getNoteWithTagsById(id).catch { e -> Timber.e(e, "Error loading note") }
                .firstOrNull()?.let { noteWithTags ->
                    updateUiState { it.fromNoteWithTags(noteWithTags).copy(isLoading = false) }
                }
        }
    }


    fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
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
    }


    @OptIn(FlowPreview::class)
    private fun setupNoteUpdateFlow() {
        viewModelScope.launch {
            _noteUpdateTrigger.debounce(500L).distinctUntilChanged().flowOn(Dispatchers.Default)
                .catch { e ->
                    Timber.e(e, "Error in note update flow")
                }.collect { payload ->
                    try {
                        noteRepository.updateNoteWithTags(payload.note, payload.tagIds)
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
                    val noteId = noteRepository.insertNoteWithTags(
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
                isPinned = updatedNote.isPinned, updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
    }

    fun updateColor(newColor: Int?) {
        updateUiStateAndTriggerSave { currentState ->
            currentState.copy(
                lightColor = newColor ?: currentState.lightColor,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)
            noteRepository.getNoteById(noteId).firstOrNull()?.let { updatedNote ->
                updateUiStateAndTriggerSave {
                    it.copy(
                        reminderDate = updatedNote.reminderDate,
                        id = updatedNote.id,
                        isDone = updatedNote.isDone,
                        updateDate = NoteUiState.getCurrentFormattedDate()
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
            noteRepository.insertNoteWithTags(note, _uiState.value.selectedTagIds.toList())
        } else {
            _noteUpdateTrigger.emit(
                NoteUpdatePayload(
                    note = note, tagIds = _uiState.value.selectedTagIds.toList()
                )
            )
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
                noteRepository.deleteNote(currentState.toNote())
            }
        }
    }


    private fun fetchTags() {
        viewModelScope.launch {
            noteRepository.getAllTags().catch { e -> Timber.e(e, "Error fetching tags") }
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
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Long? = null,
    val isDone: Boolean = false,
    val updateDate: String = "",
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
) {
    fun toNote() = Note(
        id = id,
        title = title,
        content = content,
        lightColor = lightColor,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        updateDate = updateDate,
    )

    fun isEmpty() = title.isBlank() && content.isBlank()

    fun fromNoteWithTags(noteWithTags: NoteWithTags?) = noteWithTags?.let {
        copy(
            id = it.note.id,
            title = it.note.title,
            content = it.note.content,
            lightColor = it.note.lightColor,
            isPinned = it.note.isPinned,
            isArchived = it.note.isArchived,
            isTrashed = it.note.isTrashed,
            reminderDate = it.note.reminderDate,
            isDone = it.note.isDone,
            updateDate = it.note.updateDate,
            selectedTagIds = it.tags.map { tag -> tag.id }.toSet(),
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
        lightColor = generateRandomColor(), updateDate = getCurrentFormattedDate()
    )
}



