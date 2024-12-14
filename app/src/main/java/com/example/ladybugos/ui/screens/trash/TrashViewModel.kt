package com.example.ladybugos.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.Note
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.theme.GridLayout
import com.example.ladybugos.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val noteRepository: NoteRepository,
    repo: SettingsRepo,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    val gridLayout = repo.get { gridLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GridLayout.TwoColumns)

    private val _lastRestoredNotes = MutableStateFlow<List<Note>?>(null)
    val lastRestoredNotes: StateFlow<List<Note>?> = _lastRestoredNotes.asStateFlow()

    val trashedNotes = noteRepository.getAllNotesWithTags()
        .map { notes -> notes.filter { it.note.isTrashed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun deleteNotesPermanently(notes: List<Note>) {
        viewModelScope.launch {
            notes.forEach { noteRepository.deleteNote(it) }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrashWithTags()
        }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notes.map { it.copy(isTrashed = false) }
            noteRepository.updateNotes(updatedNotes)
            widgetUpdater.undoDeleteWidgets(updatedNotes)


            _lastRestoredNotes.value = notes
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            lastRestoredNotes.value?.let { notes ->
                val updatedNotes = notes.map { it.copy(isTrashed = true) }
                noteRepository.updateNotes(updatedNotes)
                _lastRestoredNotes.value = null
            }
        }
    }

    fun clearLastRestoredNotes() {
        _lastRestoredNotes.value = null
    }
}