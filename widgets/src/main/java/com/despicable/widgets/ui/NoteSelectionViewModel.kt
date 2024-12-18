package com.despicable.widgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.domain.usecase.GetAllNotesUseCase
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.widgets.data.CoroutineDispatchers
import com.despicable.widgets.data.NoteWidgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import timber.log.Timber

// 1. UI State and Events using sealed interfaces
sealed interface NoteSelectionEvent {
    data class SelectNote(val widgetId: Int, val noteId: Long) : NoteSelectionEvent
    data object RefreshNotes : NoteSelectionEvent
}

sealed interface NoteSelectionUiState {
    data object Loading : NoteSelectionUiState
    data class Success(val notes: List<Note>) : NoteSelectionUiState
    data class Error(val message: String) : NoteSelectionUiState
}

class NoteSelectionViewModel(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val widgetRepository: NoteWidgetRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow<NoteSelectionUiState>(NoteSelectionUiState.Loading)
    val uiState = _uiState.asStateFlow()

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
        viewModelScope.launch(dispatchers.io) {
            getAllNotesUseCase()
                .map { notes ->
                    notes.filter {
                        !it.isTrashed && !it.isArchived
                    }
                }
                .catch { e ->
                    Timber.e(e, "Failed to load notes")
                    _uiState.value = NoteSelectionUiState.Error("Failed to load notes")
                }
                .collect { notes ->
                    _uiState.value = NoteSelectionUiState.Success(notes)
                }
        }
    }

    private fun selectNoteForWidget(widgetId: Int, noteId: Long) {
        viewModelScope.launch(dispatchers.io) {
            try {
                widgetRepository.saveWidgetNoteId(widgetId, noteId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to select note for widget")
                _uiState.value = NoteSelectionUiState.Error("Failed to update widget")
            }
        }
    }

    private fun List<NoteWithTags>.filterActive() =
        filter { !it.note.isTrashed && !it.note.isArchived }
}




