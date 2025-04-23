package com.despicable.widgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.Note
import com.despicable.core.model.NoteType
import com.despicable.core.model.Tag
import com.despicable.widgets.data.CoroutineDispatchers
import com.despicable.widgets.data.NoteWidgetRepository
import com.despicable.widgets.mapper.toWidgetChecklistItem
import com.despicable.widgets.model.WidgetChecklistItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import timber.log.Timber

// 1. UI State and Events using sealed interfaces
sealed interface NoteSelectionEvent {
    data class SelectNote(val widgetId: Int, val noteId: Long) : NoteSelectionEvent
    data object RefreshNotes : NoteSelectionEvent
}

sealed class NoteSelectionUiState {
    data object Loading : NoteSelectionUiState()
    data class Success(val notes: List<NotesTagsChecklist>) : NoteSelectionUiState()
    data class Error(val message: String) : NoteSelectionUiState()
}

// New data class to combine note, tags, and checklist items
data class NotesTagsChecklist(
    val note: Note,
    val tags: List<Tag>,
    val checklistItems: List<WidgetChecklistItem> = emptyList()
)


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
            combine(
                repo.getAllTags(),
                repo.getAllNotesWithTags(),
            ) { tags, allNotes ->
                // Calculate active tag IDs (tags with non-trashed notes)
                val activeTagIds = allNotes
                    .filter { !it.note.isTrashed }
                    .flatMap { it.tags }
                    .map { it.id }
                    .toSet()


                // Transform to NoteWithTagsAndChecklist
                coroutineScope {
                    val notesWithChecklist = allNotes.map { noteWithTags ->
                        async {
                            val checklistItems =
                                if (noteWithTags.note.noteType == NoteType.CHECKLIST) {
                                    try {
                                        repo.getChecklistItemsByNoteId(noteWithTags.note.id).first()
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error loading checklist items")
                                        emptyList()
                                    }
                                } else {
                                    emptyList()
                                }
                            NotesTagsChecklist(
                                note = noteWithTags.note,
                                tags = noteWithTags.tags,
                                checklistItems = checklistItems.map { it.toWidgetChecklistItem() }
                            )
                        }
                    }.awaitAll()


                    _uiState.value = NoteSelectionUiState.Success(notesWithChecklist)
                }
            }.collect()
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


}





