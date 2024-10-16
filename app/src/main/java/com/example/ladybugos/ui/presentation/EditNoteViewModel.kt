package com.example.ladybugos.ui.presentation


import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.glance.GlanceId
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ReminderStatus {
    COMPLETED, RESCHEDULED
}

class EditNoteViewModel(
    settingsRepo: SettingsRepo,
    savedStateHandle: SavedStateHandle,
    private val widgetUpdater: WidgetUpdater,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _noteState = MutableStateFlow(Note())
    val noteState: StateFlow<Note> = _noteState.asStateFlow()

    private val _noteTitle = MutableStateFlow(TextFieldValue())
    val noteTitle: StateFlow<TextFieldValue> = _noteTitle.asStateFlow()

    private val _noteContent = MutableStateFlow(TextFieldValue())
    val noteContent: StateFlow<TextFieldValue> = _noteContent.asStateFlow()

    private var originalNote: Note? = null
    private var isUpdating = false

    val theme: StateFlow<Theme> = settingsRepo.get { theme }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Theme.System
    )

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    private val _reminderStatus = MutableStateFlow<ReminderStatus?>(null)
    val reminderStatus: StateFlow<ReminderStatus?> = _reminderStatus

    init {
        val noteId = savedStateHandle.toRoute<Screen.NoteDetail>().id
        loadNoteById(noteId)
        fetchTags()
    }

    fun updateNoteReminder(
        noteId: Long,
        reminderDate: Long?,
    ) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)

            _noteState.update { currentNote ->
                currentNote.copy(
                    reminderDate = reminderDate,
                )
            }

        }
    }


    fun dismissReminderStatus() {
        _reminderStatus.value = null
    }

    fun deleteReminder() {
        viewModelScope.launch {
            noteState.value.let { note ->
                updateNoteReminder(note.id, null)
                _reminderStatus.value = null
            }
        }
    }

    private fun fetchTags() {
        viewModelScope.launch {
            noteRepository.getAllTags().collect { tags ->
                _allTags.value = tags
            }
        }
    }

    fun toggleTag(tagId: Long) {
        _selectedTagIds.update { currentIds ->
            if (currentIds.contains(tagId)) currentIds - tagId else currentIds + tagId
        }
    }

    fun togglePinStatus() = toggleNoteStatus({ it.copy(isPinned = !it.isPinned) })

    private fun toggleNoteStatus(
        stateUpdater: (Note) -> Note,
        postAction: (() -> Unit)? = null
    ) {
        if (isUpdating) return
        updateNoteState(stateUpdater)
        updateNote()
        postAction?.invoke()
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            val noteWithTags = noteRepository.getNoteWithTagsById(id).firstOrNull()
            noteWithTags?.let {
                _noteState.value = it.note
                _noteTitle.value = TextFieldValue(it.note.title)
                _noteContent.value = TextFieldValue(it.note.content)
                _selectedTagIds.value = it.tags.map { tag -> tag.id }.toSet()
                originalNote = it.note
            } ?: run {
                // Handle case where note is new or not found
                _noteState.value = Note(lightColor = generateRandomColor())
            }
        }
    }

    // Add this new function to update the note state
    private fun updateNoteState(updatedNote: Note) {
        _noteState.value = updatedNote
        _noteTitle.value = TextFieldValue(updatedNote.title)
        _noteContent.value = TextFieldValue(updatedNote.content)
        // Update other relevant state if necessary
    }

    fun updateNoteTitle(newTitle: TextFieldValue) {
        _noteTitle.value = newTitle
        updateNoteState { it.copy(title = newTitle.text) }
    }

    fun updateNoteContent(newContent: TextFieldValue) {
        _noteContent.value = newContent
        updateNoteState { it.copy(content = newContent.text) }
    }

    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = _noteState.value
            if (currentNote.title.isBlank() && currentNote.content.isBlank()) {
                onSkip()
                return@launch
            }
            saveOrUpdateNote()
            onComplete()
        }
    }

    private suspend fun saveOrUpdateNote() {
        val note = _noteState.value
        if (note.id == 0L) {
            noteRepository.insertNote(note, _selectedTagIds.value.toList())
        } else {
            noteRepository.updateNote(note, _selectedTagIds.value.toList())
        }
        originalNote = note
        widgetUpdater.updateSingleWidget(note)
    }


    fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
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
    }

    private suspend fun loadNoteData(id: Long): Note {
        return try {
            noteRepository.getNoteWithTagsById(id).firstOrNull()?.note
                ?: Note(lightColor = generateRandomColor())
        } catch (e: Exception) {
            Timber.e(e, "Error loading note data")
            Note()
        }
    }

    fun updateColor(newColor: Int? = null) {
        updateNoteState { currentState ->
            currentState.copy(
                lightColor = newColor ?: currentState.lightColor,
                updateDate = getCurrentFormattedDate()
            )
        }
        updateNote()
    }

    private fun updateNote() {
        if (isUpdating) return
        isUpdating = true
        viewModelScope.launch {
            try {
                val note = _noteState.value.copy(
                    title = _noteTitle.value.text,
                    content = _noteContent.value.text
                )
                noteRepository.updateNote(note, _selectedTagIds.value.toList())
                widgetUpdater.updateSingleWidget(note)
            } catch (e: Exception) {
                Timber.e(e, "Error updating note")
            } finally {
                isUpdating = false
            }
        }
    }

    private fun updateNoteState(update: (Note) -> Note) {
        _noteState.update(update)
    }

    private fun getCurrentFormattedDate(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    private fun generateRandomColor(): Int = colorPalette.random().toArgb()


    // Method to move note to trash and wait for the updated note list
    fun deleteNoteAndUpdateLists(onDelete: (deletedNoteId: Long?) -> Unit) {
        viewModelScope.launch {
            // Move the note to trash
            moveNoteToTrash()

            // Collect the updated note list and check if the note has been deleted
            noteRepository.getAllNotesWithTags().collect { updatedList ->
                val noteId = _noteState.value.id
                if (updatedList.any { it.note.id == noteId && it.note.isTrashed }) {
                    // If the note is no longer in the list, trigger the onDelete callback
                    onDelete(noteId)
                }
            }
        }
    }

    // Method to move note to archive and wait for the updated note list
    fun archiveNoteAndUpdateLists(onDelete: (deletedNoteId: Long?) -> Unit) {
        viewModelScope.launch {
            // Move the note to archive
            moveNoteToArchive()

            // Collect the updated note list and check if the note has been deleted
            noteRepository.getAllNotesWithTags().collect { updatedList ->
                val noteId = _noteState.value.id
                if (updatedList.any { it.note.id == noteId && it.note.isArchived }) {
                    // If the note is no longer in the list, trigger the onDelete callback
                    onDelete(noteId)
                }
            }
        }
    }


    fun deleteNoteIfEmpty() {
        viewModelScope.launch {
            if (_noteTitle.value.text.isEmpty() && _noteContent.value.text.isEmpty()) {
                noteRepository.deleteNote(_noteState.value)
            }
        }
    }

    private fun moveNoteToTrash() = toggleNoteStatuss({ it.copy(isTrashed = true) })

    private fun moveNoteToArchive() = toggleNoteStatuss({ it.copy(isArchived = true) })

    private fun toggleNoteStatuss(
        stateUpdater: (Note) -> Note,
        postAction: (() -> Unit)? = null
    ) {
        if (isUpdating) return
        updateNoteState(stateUpdater)
        updateNote()
        postAction?.invoke()
    }


}


/*

*/
/*data class EditNoteState(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val lightColor: Int = 0,
    val updateDate: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false
)*//*


class EditNoteViewModel(
    private val useCases: NotesUseCasesInterface,
    repo: SettingsRepo,
    savedStateHandle: SavedStateHandle,
    private val widgetUpdater: WidgetUpdater,
    private val repository: NoteRepository
) : ViewModel() {

    private val _noteState = MutableStateFlow(Note())
    val noteState: StateFlow<Note> = _noteState.asStateFlow()

    private val _noteTitle = MutableStateFlow(TextFieldValue())
    val noteTitle: StateFlow<TextFieldValue> = _noteTitle.asStateFlow()

    private val _noteContent = MutableStateFlow(TextFieldValue())
    val noteContent: StateFlow<TextFieldValue> = _noteContent.asStateFlow()

    private var originalNote: Note? = null
    private var isUpdating = false

    val theme: StateFlow<Theme> = repo.get { theme }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Theme.System
    )

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()


    init {
        val noteId = savedStateHandle.toRoute<Screen.NoteDetail>().id
        loadNoteById(noteId)

        viewModelScope.launch {
            repository.getAllTags().collect { _allTags.value = it }
        }

    }

    fun toggleTag(tagId: Long) {
        _selectedTagIds.update { currentIds ->
            if (currentIds.contains(tagId)) currentIds - tagId else currentIds + tagId
        }
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            repository.getNoteWithTagsById(id).collect { noteWithTags ->
                noteWithTags?.let {

                    val note = loadNoteData(id)
                    updateNoteState { note }
                    originalNote = note

                    _noteState.value = it.note
                    _noteTitle.value = TextFieldValue(it.note.title)
                    _noteContent.value = TextFieldValue(it.note.content)
                    _selectedTagIds.value = it.tags.map { tag -> tag.id }.toSet()
                }
            }
        }
    }

    private suspend fun loadNoteData(id: Long): Note {
        return if (id != -1L) {
            repository.getNoteWithTagsById(id).firstOrNull()?.note ?: Note()
        } else {
            Note(lightColor = generateRandomColor())
        }
    }

    fun updateNoteTitle(newTitle: TextFieldValue) {
        _noteTitle.value = newTitle
        updateNoteState { it.copy(title = newTitle.text) }
    }

    fun updateNoteContent(newContent: TextFieldValue) {
        _noteContent.value = newContent
        updateNoteState { it.copy(content = newContent.text) }
    }

    fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
        */
/*  viewModelScope.launch {
              val note = loadNoteData(noteId)
              updateNoteState { note }
              originalNote = note
              _noteTitle.value = TextFieldValue(note.title)
              _noteContent.value = TextFieldValue(note.content)
              widgetUpdater.updateSingleWidget(note)
          }*//*

    }


    fun saveNoteIfChanged(onComplete: () -> Unit, onSkip: () -> Unit) {
//        if (isNoteChanged()) {
        viewModelScope.launch {
            val currentNote = _noteState.value
            if (currentNote.title.isBlank() && currentNote.content.isBlank()) {
                onSkip()
                return@launch
            }

            if (currentNote.id == 0L) {
                repository.insertNote(currentNote, _selectedTagIds.value.toList())
            } else {
                repository.updateNote(currentNote, _selectedTagIds.value.toList())
            }
            onComplete()
            widgetUpdater.updateSingleWidget(currentNote)
            originalNote = currentNote
        }
//        }
    }

    private fun isNoteChanged(): Boolean {
        return originalNote?.let { original ->
            _noteTitle.value.text != original.title ||
                    _noteContent.value.text != original.content
        } ?: false
    }


    */
/*    fun deleteNoteAndUpdateList(onDelete: (deletedNoteId: Long?) -> Unit) {
            viewModelScope.launch {
                moveNoteToTrash()
                useCases.getAllNotes()
                    .map { notes -> notes.any { it.id == _noteState.value.id && it.isTrashed } }
                    .filter { it }
                    .collect { onDelete(_noteState.value.id) }
            }
        }*//*


    // Method to move note to trash and wait for the updated note list
    fun deleteNoteAndUpdateList(onDelete: (deletedNoteId: Long?) -> Unit) {
        viewModelScope.launch {
            // Move the note to trash
            moveNoteToTrash()

            // Collect the updated note list and check if the note has been deleted
            useCases.getAllNotes().collect { updatedList ->
                val noteId = _noteState.value.id
                if (updatedList.any { it.id == noteId && it.isTrashed }) {
                    // If the note is no longer in the list, trigger the onDelete callback
                    onDelete(noteId)
                }
            }
        }
    }

    // Method to move note to archive and wait for the updated note list
    fun archiveNoteAndUpdateList(onDelete: (deletedNoteId: Long?) -> Unit) {
        viewModelScope.launch {
            // Move the note to archive
            moveNoteToArchive()

            // Collect the updated note list and check if the note has been deleted
            useCases.getAllNotes().collect { updatedList ->
                val noteId = _noteState.value.id
                if (updatedList.any { it.id == noteId && it.isArchived }) {
                    // If the note is no longer in the list, trigger the onDelete callback
                    onDelete(noteId)
                }
            }
        }
    }


    */
/*  fun archiveNoteAndUpdateList(onArchive: (archivedNoteId: Long?) -> Unit) {
          viewModelScope.launch {
              moveNoteToArchive()
              useCases.getAllNotes()
                  .map { notes -> notes.any { it.id == _noteState.value.id && it.isArchived } }
                  .filter { it }
                  .collect { onArchive(_noteState.value.id) }
          }
      }*//*


    fun updateNote(newColor: Int? = null) {
        val shouldUpdateDate = isNoteChanged()
        Timber.tag("DEBUG").d("[isNoteChanged]=[$shouldUpdateDate]")
        updateNoteState { currentState ->
            currentState.copy(
                lightColor = newColor ?: currentState.lightColor,
                updateDate = if (shouldUpdateDate)
                    getCurrentFormattedDate() else
                    currentState.updateDate
            )
        }
        updateAndLaunch()
    }

    fun togglePinStatus() = toggleNoteStatus({ it.copy(isPinned = !it.isPinned) })

    fun toggleArchiveStatus(onArchiveToggled: (Boolean) -> Unit) {
        toggleNoteStatus({ it.copy(isArchived = !it.isArchived) }) {
            onArchiveToggled(_noteState.value.isArchived)
        }
    }

    fun deleteNoteIfEmpty() {
        viewModelScope.launch {
            if (_noteTitle.value.text.isEmpty() && _noteContent.value.text.isEmpty()) {
                useCases.deleteNote(_noteState.value)
            }
        }
    }

    private fun moveNoteToTrash() = toggleNoteStatus({ it.copy(isTrashed = true) })

    private fun moveNoteToArchive() = toggleNoteStatus({ it.copy(isArchived = true) })

    private fun toggleNoteStatus(
        stateUpdater: (Note) -> Note,
        postAction: (() -> Unit)? = null
    ) {
        if (isUpdating) return
        updateNoteState(stateUpdater)
        updateAndLaunch()
        postAction?.invoke()
    }

    private fun updateAndLaunch() {
        if (isUpdating) return
        isUpdating = true
        viewModelScope.launch {
            try {
                val updatedNote = _noteState.value.copy(
                    title = _noteTitle.value.text,
                    content = _noteContent.value.text
                )
                repository.updateNote(updatedNote, _selectedTagIds.value.toList())
                widgetUpdater.updateSingleWidget(updatedNote)
            } catch (e: Exception) {
                Timber.e(e, "Error updating note")
            } finally {
                isUpdating = false
            }
        }
    }


    private fun updateNoteState(update: (Note) -> Note) {
        _noteState.update(update)
    }


    private fun getCurrentFormattedDate(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    private fun generateRandomColor(): Int = colorPalette.random().toArgb()

    */
/*   private fun Note.toNote(): Note =
           Note(id, title, content, updateDate, lightColor, isPinned, isArchived, isTrashed)

       private fun Note.toEditNoteState(): Note =
           Note(id, title, content, lightColor, updateDate, isPinned, isArchived, isTrashed)*//*

}

*/






