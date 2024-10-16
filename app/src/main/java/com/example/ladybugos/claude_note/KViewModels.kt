package com.example.ladybugos.claude_note

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class KNoteListViewModel(
    private val repository: KNoteRepository
) : ViewModel() {

    private val notesWithTags = repository.getAllNotesWithTags().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val tags = repository.getAllTags().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    val filteredNotes = combine(notesWithTags, selectedTagId) { notes, tagId ->
        if (tagId == null) {
            notes
        } else {
            notes.filter { noteWithTags -> noteWithTags.tags.any { it.id == tagId } }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )


    fun toggleTag(tagId: Long) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            repository.insertTag(KTag(name = name))
        }
    }

    fun deleteNote(note: KNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun updateTag(tag: KTag) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun deleteTag(tag: KTag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }
}

class KEditNoteViewModel(
    private val repository: KNoteRepository,
    repo: SettingsRepo,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _noteState = MutableStateFlow(KNote())
    val noteState: StateFlow<KNote> = _noteState.asStateFlow()

    private val _noteTitle = MutableStateFlow(TextFieldValue())
    val noteTitle: StateFlow<TextFieldValue> = _noteTitle.asStateFlow()

    private val _noteContent = MutableStateFlow(TextFieldValue())
    val noteContent: StateFlow<TextFieldValue> = _noteContent.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    private val _allTags = MutableStateFlow<List<KTag>>(emptyList())
    val allTags: StateFlow<List<KTag>> = _allTags.asStateFlow()

    private var originalNote: KNote? = null

    val theme: StateFlow<Theme> = repo.get { theme }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Theme.System
    )


    init {
        viewModelScope.launch {
            repository.getAllTags().collect { tags ->
                _allTags.value = tags
            }
        }
    }

    fun loadNote(id: Long) {
        viewModelScope.launch {
            repository.getNoteWithTagsById(id).collect { noteWithTags ->
                noteWithTags?.let {
                    _noteState.value = it.note
                    _noteTitle.value = TextFieldValue(it.note.title)
                    _noteContent.value = TextFieldValue(it.note.content)
                    _selectedTagIds.value = it.tags.map { tag -> tag.id }.toSet()
                }
            }
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

    fun toggleTag(tagId: Long) {
        _selectedTagIds.update { currentIds ->
            if (currentIds.contains(tagId)) currentIds - tagId else currentIds + tagId
        }
    }

    fun updateColor(newColor: Int? = null) {
        updateNoteState {
            it.copy(
                lightColor = newColor ?: it.lightColor,
                updateDate = if (isNoteChanged())
                    getCurrentFormattedDate()
                else it.updateDate
            )
        }
    }

    private fun isNoteChanged(): Boolean {
        return originalNote?.let { original ->
            _noteTitle.value.text != original.title ||
                    _noteContent.value.text != original.content
        } ?: false
    }

    fun togglePinned() {
        updateNoteState { it.copy(isPinned = !it.isPinned) }
    }

    fun toggleArchived() {
        updateNoteState { it.copy(isArchived = !it.isArchived) }
    }

    fun toggleTrashed() {
        updateNoteState { it.copy(isTrashed = !it.isTrashed) }
    }

    private fun updateNoteState(update: (KNote) -> KNote) {
        _noteState.update(update)
    }

    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = _noteState.value
            if (currentNote.title.isBlank() && currentNote.content.isBlank()) {
                onSkip()
                return@launch
            }

            val updatedNote = currentNote.copy(
                updateDate = LocalDateTime.now()
                    .toString() // You might want to use a specific date format
            )

            if (currentNote.id == 0L) {
                repository.insertNote(updatedNote, _selectedTagIds.value.toList())
            } else {
                repository.updateNote(updatedNote, _selectedTagIds.value.toList())
            }
            onComplete()
        }
    }

    private fun getCurrentFormattedDate(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    private fun generateRandomColor(): Int = colorPalette.random().toArgb()
}

