package com.despicable.ladybugos.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.datastore.SettingsRepo
import com.despicable.core.domain.usecase.DeleteNoteUseCase
import com.despicable.core.domain.usecase.EmptyTrashWithTagsUseCase
import com.despicable.core.domain.usecase.GetAllNotesTagsUseCase
import com.despicable.core.domain.usecase.UpdateNotesUseCase
import com.despicable.model.GridLayout
import com.despicable.model.Note
import com.despicable.widgets.data.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val getAllNotesTagsUseCase: GetAllNotesTagsUseCase,
    private val updateNotesUseCase: UpdateNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val emptyTrashWithTagsUseCase: EmptyTrashWithTagsUseCase,
    repo: SettingsRepo,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    val gridLayout = repo.get { gridLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GridLayout.TwoColumns)

    private val _lastRestoredNotes =
        MutableStateFlow<List<Note>?>(null)
    val lastRestoredNotes: StateFlow<List<Note>?> =
        _lastRestoredNotes.asStateFlow()

    val trashedNotes = getAllNotesTagsUseCase()
        .map { notes -> notes.filter { it.note.isTrashed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun deleteNotesPermanently(notes: List<Note>) {
        viewModelScope.launch {
            notes.forEach { deleteNoteUseCase(it) }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            emptyTrashWithTagsUseCase()
        }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notes.map { it.copy(isTrashed = false) }
            updateNotesUseCase(updatedNotes)
            widgetUpdater.undoDeleteWidgets(updatedNotes)


            _lastRestoredNotes.value = notes
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            lastRestoredNotes.value?.let { notes ->
                val updatedNotes = notes.map { it.copy(isTrashed = true) }
                updateNotesUseCase(updatedNotes)
                _lastRestoredNotes.value = null
            }
        }
    }

    fun clearLastRestoredNotes() {
        _lastRestoredNotes.value = null
    }
}