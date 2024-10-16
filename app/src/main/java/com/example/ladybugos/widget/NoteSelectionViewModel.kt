package com.example.ladybugos.widget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.model.Note
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.presentation.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteSelectionViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    context: Context
) : ViewModel() {


    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val widgetUpdater = WidgetUpdater(context)

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            try {
                noteRepository.getAllNotes.collectLatest { notesList ->
                    _uiState.value = NotesUiState.Success(notesList)
                }
            } catch (e: Exception) {
                _uiState.value = NotesUiState.Error("Failed to load notes: ${e.localizedMessage}")
            }
        }
    }
}

sealed class NotesUiState {
    data object Loading : NotesUiState()
    data class Success(val notes: List<Note>) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}