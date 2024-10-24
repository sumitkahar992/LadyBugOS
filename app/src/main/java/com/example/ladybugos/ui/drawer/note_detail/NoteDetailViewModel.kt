package com.example.ladybugos.ui.drawer.note_detail


import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.glance.GlanceId
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.theme.Theme
import com.example.ladybugos.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NoteDetailViewModel(
    settingsRepo: SettingsRepo,
    savedStateHandle: SavedStateHandle,
    private val widgetUpdater: WidgetUpdater,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    val theme: StateFlow<Theme> = settingsRepo.get { theme }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Theme.System
    )

    init {
        val noteId = savedStateHandle.toRoute<Screen.NoteDetail>().id
        loadNoteById(noteId)
        fetchTags()
    }


    /*    fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
            viewModelScope.launch {
                try {
                    val note = loadNoteData(noteId)
                    updateNoteState { note }
                    originalNote = note
                    _noteTitle.value = TextFieldValue(note.title)
                    _noteContent.value = TextFieldValue(note.content)
                    widgetUpdater.updateSingleWidget(note)
                } catch (e: Exception) {
                    Timber.e(e, "Error preloading note data")
                }
            }
        }*/

    fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
        viewModelScope.launch {
            try {
                val noteWithTags = noteRepository.getNoteWithTagsById(noteId).firstOrNull()
                updateUiState { it.fromNoteWithTags(noteWithTags) }

                noteWithTags?.note?.let { note ->
                    widgetUpdater.updateSingleWidget(note)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error preloading note data")
                // Optionally, you could add error handling in the UI state if needed:
                // updateUiState { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun updateNoteTitle(newTitle: TextFieldValue) {
        updateUiState {
            it.copy(
                title = newTitle,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
        updateNote() // Add this to update date on title change
    }

    fun updateNoteContent(newContent: TextFieldValue) {
        updateUiState {
            it.copy(
                content = newContent,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
        updateNote() // Add this to update date on content change
    }

    fun toggleTag(tagId: Long) =
        updateUiState { it.copy(selectedTagIds = it.selectedTagIds.toggle(tagId)) }

    fun togglePinStatus() = updateNoteStatus { it.copy(isPinned = !it.isPinned) }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)
            // Fetch the updated note to ensure we have the latest data
            noteRepository.getNoteById(noteId).first()?.let { updatedNote ->
                updateUiState {
                    it.copy(
                        reminderDate = updatedNote.reminderDate,
                        id = updatedNote.id,
                        isDone = updatedNote.isDone
                    )
                }
            }
        }
    }

    fun updateColor(newColor: Int?) =
        updateNoteStatus { it.copy(lightColor = newColor ?: it.lightColor) }

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

    fun deleteReminder() {
        viewModelScope.launch {
            val noteId = _uiState.value.id
            if (noteId != 0L) {
                noteRepository.deleteReminder(noteId)
                updateUiState { it.copy(reminderDate = null, isDone = false) }
            }
        }
    }

    fun deleteNoteAndUpdateLists(onDelete: (deletedNoteId: Long?) -> Unit) {
        viewModelScope.launch {
            moveNoteToTrash()

            noteRepository.getAllNotesWithTags().first { updatedList ->
                val noteId = _uiState.value.id
                val isDeleted = updatedList.any { it.note.id == noteId && it.note.isTrashed }
                if (isDeleted) {
                    onDelete(noteId)
                }
                isDeleted
            }
        }
    }

    fun archiveNoteAndUpdateLists(onArchive: (archivedNoteId: Long?) -> Unit) {
        viewModelScope.launch {
            moveNoteToArchive()

            noteRepository.getAllNotesWithTags().first { updatedList ->
                val noteId = _uiState.value.id
                val isArchived = updatedList.any { it.note.id == noteId && it.note.isArchived }
                if (isArchived) {
                    onArchive(noteId)
                }
                isArchived
            }
        }
    }

    private fun moveNoteToTrash() = updateNoteStatus { it.copy(isTrashed = true) }

    private fun moveNoteToArchive() = updateNoteStatus { it.copy(isArchived = true) }

    fun deleteNoteIfEmpty() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isEmpty()) {
                noteRepository.deleteNote(currentState.toNote())
            }
        }
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            val noteWithTags = noteRepository.getNoteWithTagsById(id).firstOrNull()
            updateUiState { it.fromNoteWithTags(noteWithTags) }
        }
    }

    private fun fetchTags() {
        viewModelScope.launch {
            noteRepository.getAllTags().collect { tags ->
                updateUiState { it.copy(allTags = tags) }
            }
        }
    }

    private fun updateUiState(update: (NoteUiState) -> NoteUiState) {
        _uiState.update(update)
    }

    private fun updateNoteStatus(update: (Note) -> Note) {
        updateUiState { currentState ->
            val updatedNote = update(currentState.toNote())
            currentState.copy(
                id = updatedNote.id,
                title = TextFieldValue(updatedNote.title),
                content = TextFieldValue(updatedNote.content),
                lightColor = updatedNote.lightColor,
                isPinned = updatedNote.isPinned,
                isArchived = updatedNote.isArchived,
                isTrashed = updatedNote.isTrashed,
                reminderDate = updatedNote.reminderDate,
                isDone = updatedNote.isDone,
                updateDate = updatedNote.updateDate
            )
        }
        updateNote()
    }


    private fun updateNote() {
        viewModelScope.launch {
            try {
                val note = _uiState.value.toNote()
                noteRepository.updateNote(note, _uiState.value.selectedTagIds.toList())

                widgetUpdater.updateSingleWidget(note)
            } catch (e: Exception) {
                Timber.e(e, "Error updating note")
            }
        }
    }

    private suspend fun saveOrUpdateNote(note: Note) {
        if (note.id == 0L) {
            noteRepository.insertNote(note, _uiState.value.selectedTagIds.toList())
        } else {
            noteRepository.updateNote(note, _uiState.value.selectedTagIds.toList())
        }
        widgetUpdater.updateSingleWidget(note)
    }

    private fun Set<Long>.toggle(id: Long) = if (contains(id)) minus(id) else plus(id)
}

data class NoteUiState(
    val id: Long = 0,
    val title: TextFieldValue = TextFieldValue(),
    val content: TextFieldValue = TextFieldValue(),
    val lightColor: Int = generateRandomColor(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Long? = null,
    val isDone: Boolean = false,
    val updateDate: String = getCurrentFormattedDate(),
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet()
) {
    fun toNote() = Note(
        id = id,
        title = title.text,
        content = content.text,
        lightColor = lightColor,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        updateDate = updateDate
    )

    fun isEmpty() = title.text.isBlank() && content.text.isBlank()

    fun fromNoteWithTags(noteWithTags: NoteWithTags?) = noteWithTags?.let {
        copy(
            id = it.note.id,
            title = TextFieldValue(it.note.title),
            content = TextFieldValue(it.note.content),
            lightColor = it.note.lightColor,
            isPinned = it.note.isPinned,
            isArchived = it.note.isArchived,
            isTrashed = it.note.isTrashed,
            reminderDate = it.note.reminderDate,
            isDone = it.note.isDone,
            updateDate = it.note.updateDate,
            selectedTagIds = it.tags.map { tag -> tag.id }.toSet()
        )
    } ?: this

    companion object {
        fun getCurrentFormattedDate(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        private fun generateRandomColor(): Int = colorPalette.random().toArgb()
    }
}







