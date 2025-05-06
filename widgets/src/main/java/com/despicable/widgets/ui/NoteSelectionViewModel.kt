package com.despicable.widgets.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.NoteComplete
import com.despicable.widgets.data.CoroutineDispatchers
import com.despicable.widgets.data.NoteWidgetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

// 1. UI State and Events using sealed interfaces
sealed interface NoteSelectionEvent {
    data class SelectNote(val widgetId: Int, val noteId: Long) : NoteSelectionEvent
    data object RefreshNotes : NoteSelectionEvent
}

sealed class NoteSelectionUiState {
    data object Loading : NoteSelectionUiState()
    data class Success(val notes: List<NoteComplete>) : NoteSelectionUiState()
    data class Error(val message: String) : NoteSelectionUiState()
}

class NoteSelectionViewModel(
    private val repo: NoteRepository,
    private val widgetRepository: NoteWidgetRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow<NoteSelectionUiState>(NoteSelectionUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var noteFlowJob: Job? = null


    init {
        loadNotes()
    }

    fun handleEvent(event: NoteSelectionEvent) {
        viewModelScope.launch {
            when (event) {
                is NoteSelectionEvent.SelectNote -> selectNoteForWidget(
                    event.widgetId,
                    event.noteId
                )

                NoteSelectionEvent.RefreshNotes -> loadNotes()
            }
        }
    }

    private fun loadNotes() {
        noteFlowJob = viewModelScope.launch {
            repo.getAllNotesWithTags().collect { allNotes ->

                val notesWithChecklist = allNotes.map { noteWithTags ->
                    NoteComplete(
                        note = noteWithTags.note,
                        checklistItems = noteWithTags.checklistItems
                    )
                }

                _uiState.value = NoteSelectionUiState.Success(notesWithChecklist)
            }
        }
    }

    private fun selectNoteForWidget(widgetId: Int, noteId: Long) {
        viewModelScope.launch(dispatchers.io) {
            try {
                widgetRepository.saveWidgetNoteId(widgetId, noteId)
            } catch (e: Exception) {
                Log.e("WIDGET", "Failed to select note for widget")
                _uiState.value = NoteSelectionUiState.Error("Failed to update widget")
            }
        }
    }


}





