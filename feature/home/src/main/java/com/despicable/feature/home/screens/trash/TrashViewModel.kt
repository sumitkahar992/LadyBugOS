package com.despicable.feature.home.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.datastore.SettingsRepo
import com.despicable.core.designsystem.theme.GridLayout
import com.despicable.core.model.Note
import com.despicable.widgets.data.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repo: NoteRepository,
    settingsRepo: SettingsRepo,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    val gridLayout = settingsRepo.get { gridLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GridLayout.TwoColumns)

    private val _lastRestoredNotes =
        MutableStateFlow<List<Note>?>(null)
    val lastRestoredNotes: StateFlow<List<Note>?> =
        _lastRestoredNotes.asStateFlow()

    val trashedNotes = repo.getAllNotesWithTags()
        .map { notes -> notes.filter { it.note.isTrashed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun deleteNotesPermanently(notes: List<Note>) {
        viewModelScope.launch {
            notes.forEach { repo.deleteNote(it) }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repo.emptyTrashWithTags()
        }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notes.map { it.copy(isTrashed = false) }
            repo.updateNotes(updatedNotes)
            widgetUpdater.undoDeleteWidgets(updatedNotes)


            _lastRestoredNotes.value = notes
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            lastRestoredNotes.value?.let { notes ->
                val updatedNotes = notes.map { it.copy(isTrashed = true) }
                repo.updateNotes(updatedNotes)
                _lastRestoredNotes.value = null
            }
        }
    }

    fun clearLastRestoredNotes() {
        _lastRestoredNotes.value = null
    }
}