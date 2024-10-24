package com.example.ladybugos.ui.drawer.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.Note
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.drawer.home.GridLayout
import com.example.ladybugos.ui.theme.Theme
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
    private val widgetUpdater: WidgetUpdater,
    repo: SettingsRepo,
) : ViewModel() {

    val theme: StateFlow<Theme> = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

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
            updateWidgets()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrashWithTags()
            updateWidgets()
        }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notes.map { it.copy(isTrashed = false) }
            noteRepository.updateNotes(updatedNotes)
            _lastRestoredNotes.value = notes
            updateWidgets()
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            lastRestoredNotes.value?.let { notes ->
                val updatedNotes = notes.map { it.copy(isTrashed = true) }
                noteRepository.updateNotes(updatedNotes)
                _lastRestoredNotes.value = null
                updateWidgets()
            }
        }
    }

    fun clearLastRestoredNotes() {
        _lastRestoredNotes.value = null
    }

    private suspend fun updateWidgets() {
        widgetUpdater.updateAllWidgets(trashedNotes.value.map { it.note })
    }
}