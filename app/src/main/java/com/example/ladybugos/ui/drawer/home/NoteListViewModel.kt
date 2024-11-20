package com.example.ladybugos.ui.drawer.home


import androidx.annotation.Keep
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.LoadSampleDataUseCase
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.navigation.NoteAction
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.theme.GridLayout
import com.example.ladybugos.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Keep
data class NoteListUiState(
    val isNotesInitialized: Boolean = false,
    val notes: List<NoteWithTags> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val selectedTagId: Long? = null,
    val gridLayout: GridLayout = GridLayout.TwoColumns,
    val lastModifiedNotes: List<Note>? = null,
    val originalNote: Note? = null,
    val originalNoteTags: List<Tag> = emptyList(),
    val tagIds: List<Long> = emptyList()
)

data class SnackBarMessage(
    val message: String,
    val actionLabel: String = "UNDO",
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onDismiss: () -> Unit = {},
    val onAction: () -> Unit = {}
)

class NoteListViewModel(
    private val repo: SettingsRepo,
    private val noteRepository: NoteRepository,
    private val loadSampleDataUseCase: LoadSampleDataUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState = _uiState.asStateFlow()

    private var noteFlowJob: Job? = null

    private val _snackBarMessage = MutableStateFlow<SnackBarMessage?>(null)
    val snackBarMessage = _snackBarMessage.asStateFlow()

    init {
//        initializeViewModel()
        initializeNotes()

        viewModelScope.launch {
//            loadSampleDataUseCase()
        }
    }

    /*  private fun initializeViewModel() {
          val deletedId = savedStateHandle.toRoute<Screen.NoteList>().deletedId
          Timber.tag("DEBUG").d("LIST[originalNote] - [$deletedId]")
          deletedId?.let { loadNoteById(it) }
      }

      private fun loadNoteById(id: Long) {
          viewModelScope.launch(Dispatchers.Main) {
              try {
                  noteRepository.getNoteWithTagsById(id).firstOrNull()?.let { noteWithTags ->

                      // Store original note state before any modification
                      _uiState.update {
                          it.copy(
                              originalNote = noteWithTags.note,
                              tagIds = noteWithTags.tags.map { tag -> tag.id }
                          )
                      }
                      delay(600)
                      withContext(Dispatchers.IO) {
                          noteRepository.updateNoteWithTags(
                              note = noteWithTags.note.copy(
                                  isTrashed = true,
                              ),
                              tagIds = noteWithTags.tags.map { it.id }
                          )
                      }
                  }
              } catch (e: Exception) {
                  Timber.e(e, "Error deleting note")
              }
          }
      }*/


    fun clearSnackbarMessage() {
        _snackBarMessage.value = null
    }


    fun handleNoteAction(action: NoteAction) {
        viewModelScope.launch {
            try {
                // Store original state for undo
                val originalNote =
                    noteRepository.getNoteById(action.noteId).firstOrNull() ?: return@launch

                // Update database based on action
                when (action) {
                    is NoteAction.Archive -> archiveNotes(listOf(originalNote))
                    is NoteAction.Delete -> trashNotes(listOf(originalNote))
                    is NoteAction.Unarchive -> unarchiveNotes(listOf(originalNote))
                }

                // Show snackbar with undo option
                _snackBarMessage.value = SnackBarMessage(
                    message = when (action) {
                        is NoteAction.Archive -> "Note archived"
                        is NoteAction.Delete -> "Note moved to trash"
                        is NoteAction.Unarchive -> "Note unarchived"
                    },
                    onDismiss = {
                        _snackBarMessage.value = null
                        _uiState.update { it.copy(lastModifiedNotes = null) }
                    },
                    onAction = {
                        viewModelScope.launch {
                            when (action) {
                                is NoteAction.Archive -> unarchiveNotes(listOf(originalNote))
                                is NoteAction.Delete -> {
                                    updateNotes(
                                        notes = listOf(originalNote),
                                        updates = { it.copy(isTrashed = false) }
                                    )
                                }

                                is NoteAction.Unarchive -> archiveNotes(listOf(originalNote))
                            }
                            _snackBarMessage.value = null
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error handling note action")
            }
        }
    }


    private fun initializeNotes() {
        noteFlowJob?.cancel()
        noteFlowJob = viewModelScope.launch {
            combine(
                repo.get { gridLayout },
                noteRepository.getAllTags(),
                noteRepository.getAllNotesWithTags(),
                _uiState.map { it.searchQuery }
            ) { gridLayout, tags, allNotes, query ->
                _uiState
                    .update { current ->
                        current.copy(
                            isNotesInitialized = true,
                            gridLayout = gridLayout,
                            tags = tags,
                            notes = filterAndSortNotes(
                                notes = allNotes,
                                tagId = current.selectedTagId,
                                query = query,
                            )
                        )
                    }
            }.collect()
        }
    }

    private fun filterAndSortNotes(
        notes: List<NoteWithTags>,
        tagId: Long?,
        query: String,
    ): List<NoteWithTags> {
        return notes.asSequence()
            .filter { noteWithTags ->
                val note = noteWithTags.note
                !note.isTrashed &&
                        (tagId == null || noteWithTags.tags.any { it.id == tagId }) &&
                        note.matchesSearch(query)
            }
            .toList()
    }


    // In your ViewModel
    fun updateTags(
        addedTags: List<Tag>,
        updatedTags: List<Tag>,
        deletedTagIds: List<Tag>
    ) {
        viewModelScope.launch {
            // Process all changes in a single transaction
            withContext(Dispatchers.IO) {
                // Delete tags
                deletedTagIds.forEach { noteRepository.deleteTag(it) }

                // Update existing tags
                updatedTags.forEach { noteRepository.updateTag(it) }

                // Add new tags
                addedTags.forEach { noteRepository.insertTag(it) }

                // Refresh tags list
//                refreshTags()
            }
        }
    }


    fun updateSearchQuery(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query) }
        }
    }


    private fun updateNotes(notes: List<Note>, updates: (Note) -> Note, delayTime: Long = 400) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(lastModifiedNotes = notes) }
                val updatedNotes = notes.map(updates)
                delay(delayTime)

                // Update database
                noteRepository.updateNotes(updatedNotes)

                // Update widgets
                widgetUpdater.updateWidgetsForNotes(updatedNotes)

                // Refresh notes list to ensure UI is in sync
                initializeNotes()

            } catch (e: Exception) {
                Timber.e(e, "Error updating notes")
            }
        }
    }


    fun toggleTag(tagId: Long) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    selectedTagId = if (currentState.selectedTagId == tagId) null else tagId
                )
            }
            // Re-filter notes when tag is toggled
            initializeNotes()
        }
    }


    fun updateGridLayout(gridLayout: GridLayout) {
        viewModelScope.launch { repo.setGridLayout(gridLayout) }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)
        }
    }

    fun pinNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = {
            it.copy(
                isPinned = true,
                pinnedDate = System.currentTimeMillis()
            )
        }
    )

    fun unpinNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = {
            it.copy(
                isPinned = false,
                pinnedDate = null
            )
        }
    )

    fun archiveNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = {
            it.copy(
                isArchived = true,
                isPinned = false,
                pinnedDate = null,
            )
        },
        delayTime = 500
    )

    fun unarchiveNotes(notes: List<Note>) = updateNotes(
        notes = notes,
        updates = { it.copy(isArchived = false) },
        delayTime = 500
    )

    fun trashNotes(notes: List<Note>) =
        updateNotes(
            notes = notes,
            updates = {
                it.copy(
                    isTrashed = true,
                    isPinned = false,
                    pinnedDate = null
                )
            },
            delayTime = 500
        )


    fun pinAndUnarchiveNotes(notes: List<Note>) =
        updateNotes(
            notes = notes,
            updates = {
                it.copy(
                    isPinned = true,
                    pinnedDate = System.currentTimeMillis(),
                    isArchived = false
                )
            }
        )

    fun undoLastOperation() {
        viewModelScope.launch {
            _uiState.value.lastModifiedNotes?.let { notes ->
                updateNotes(
                    notes,
                    updates = { it.copy() }
                )
                widgetUpdater.updateWidgetsForNotes(notes)
                _uiState.update { it.copy(lastModifiedNotes = null) }
            }
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            noteRepository.insertTag(Tag(name = name))
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            noteRepository.updateTag(tag)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            noteRepository.deleteTag(tag)
        }
    }

    override fun onCleared() {
        super.onCleared()
        noteFlowJob?.cancel()
    }
}


