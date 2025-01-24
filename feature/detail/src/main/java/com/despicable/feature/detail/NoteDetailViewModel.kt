package com.despicable.feature.detail


import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.designsystem.colorPalette
import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import com.despicable.feature.detail.navigation.DetailRoute
import com.despicable.widgets.data.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class NoteDetailViewModel(
    private val repo: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _noteUpdateTrigger = MutableSharedFlow<NoteUpdatePayload>()


    init {
        val noteId = savedStateHandle.toRoute<DetailRoute>().id

        if (noteId > 0) {
            loadNoteById(noteId)   // Load existing note
        } else {
            updateUiState {       // Initialize new note
                it.initializeNewNote()
            }
        }

        fetchTags()
        setupNoteUpdateFlow()
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            repo.getNoteWithTagsById(id).catch { e -> Timber.e(e, "Error loading note") }
                .firstOrNull()?.let { noteWithTags ->
                    updateUiState { it.fromNoteWithTags(noteWithTags).copy(isLoading = false) }

                    // Start collecting checklist items immediately
                    repo.getChecklistItemsByNoteId(id)
                        .catch { e -> Timber.e(e, "Error loading checklist items") }
                        .collect { items ->
                            _uiState.update { it.copy(checklistItems = items) }
                        }

                }
        }
    }


    fun onEvent(event: CheckListEvent) {
        when (event) {

            CheckListEvent.ToggleChecklist -> toggleChecklist()
            is CheckListEvent.RemoveChecklistItem -> removeChecklistItem(event.index)
            is CheckListEvent.ChecklistItemChecked -> toggleChecklistItem(event.item)
            is CheckListEvent.ReorderChecklistItems -> reorderChecklistItems(
                event.fromPosition,
                event.toPosition
            )

            is CheckListEvent.AddChecklistItemAt -> addChecklistItemAt(event.position)
            is CheckListEvent.UpdateChecklistItemContent -> updateChecklistItemContent(
                event.item,
                event.content
            )

            is CheckListEvent.UpdateFocusedPosition -> {
                _uiState.update { it.copy(focusedItemPosition = event.position) }
            }
        }
    }


    private fun reorderChecklistItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentItems = uiState.value.checklistItems.toMutableList()

            // Perform the move operation
            val item = currentItems.removeAt(fromIndex)
            currentItems.add(toIndex, item)

            // Calculate new focus position more efficiently
            val newFocusPosition = when (val currentFocus = uiState.value.focusedItemPosition) {
                fromIndex -> toIndex
                in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex) -> {
                    if (fromIndex < toIndex) currentFocus - 1 else currentFocus + 1
                }

                else -> currentFocus
            }

            // Update UI state
            updateUiStateAndTriggerSave { state ->
                state.copy(
                    checklistItems = currentItems,
                    focusedItemPosition = newFocusPosition
                )
            }


            // Update database in background without creating new coroutine
            withContext(Dispatchers.IO) {
                // Batch update items with new positions
                currentItems.mapIndexed { index, item ->
                    item.copy(position = index)
                }.let { updatedItems ->
                    repo.updateAllChecklistItems(updatedItems)
                }
            }
        }
    }

    private fun toggleChecklistItem(item: Checklist) {
        viewModelScope.launch {
            val updatedItem = item.copy(isChecked = !item.isChecked)
            repo.updateChecklistItem(updatedItem)


            updateUiStateAndTriggerSave { state ->
                val updatedItems = state.checklistItems.map {
                    if (it.id == item.id) updatedItem else it
                }
                state.copy(
                    checklistItems = updatedItems,
                )
            }
        }
    }

    private fun addChecklistItemAt(position: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value

//            val noteId = currentState.id

            // Ensure the note is saved first if it's a new note
            val noteId = if (currentState.id == 0L) {
                // Save the note first if it hasn't been saved
                val savedNoteId =
                    repo.insertNoteWithTagsChecklist(
                        currentState.toNote(),
                        emptyList(),
                        currentState.checklistItems
                    )
                savedNoteId
            } else {
                currentState.id
            }

            Timber.tag("DEBUG").d("=--noteId--==[$noteId]")
            // Create new item
            val newItem = Checklist(
                noteId = noteId,
                content = "",
                position = position
            )

            // Update positions of existing items
            val updatedItems = currentState.checklistItems.toMutableList()
            updatedItems.forEachIndexed { index, item ->
                if (index >= position) {
                    val updatedItem = item.copy(position = index + 1)
                    repo.updateChecklistItem(updatedItem)
                }
            }

            // Insert new item
            val insertedId = repo.insertChecklistItem(newItem)
            val insertedItem = newItem.copy(id = insertedId)

            updatedItems.add(position, insertedItem)

            updateUiStateAndTriggerSave { state ->
                state.copy(
                    id = noteId,
                    checklistItems = updatedItems,
                    focusedItemPosition = position
                )
            }
        }
    }

    private fun updateChecklistItemContent(item: Checklist, content: String) {
        viewModelScope.launch {
            val updatedItem = item.copy(content = content)
            repo.updateChecklistItem(updatedItem)


            updateUiStateAndTriggerSave { state ->
                val updatedItems = state.checklistItems.map {
                    if (it.id == item.id) updatedItem else it
                }
                state.copy(
                    checklistItems = updatedItems,
                )
            }
        }
    }


    /*
        private fun toggleChecklist() {
            viewModelScope.launch {
                val currentState = _uiState.value
                val isChecklist = !currentState.isCheckList

                if (isChecklist) {
                    // Save note first if new
                    val noteId = currentState.id

                    // Clear existing items first
                    repo.deleteChecklistItemsByNoteId(noteId)

                    // Convert non-empty content lines to checklist items
                    val checklistItems = currentState.content
                        .split("\n")
                        .filter { it.isNotBlank() }
                        .mapIndexed { index, line ->
                            Checklist(
                                noteId = noteId,
                                content = line.trim(),
                                position = index
                            )
                        }

                    // Insert new items
                    val insertedItems = checklistItems.map { item ->
                        val id = repo.insertChecklistItem(item)
                        item.copy(id = id)
                    }

                    _uiState.update {
                        it.copy(
                            id = noteId,
                            isCheckList = true,
                            checklistItems = insertedItems,
                            content = ""
                        )
                    }
                } else {
                    // Convert checklist items to content
                    val content = currentState.checklistItems
                        .sortedBy { it.position }
                        .joinToString("\n") { it.content }

                    // Clear checklist items
                    repo.deleteChecklistItemsByNoteId(currentState.id)

                    _uiState.update {
                        it.copy(
                            isCheckList = false,
                            checklistItems = emptyList(),
                            content = content
                        )
                    }
                }

                // Update note state
                repo.updateNoteWithTagsChecklist(_uiState.value.toNote(), emptyList(), _uiState.value.checklistItems)
            }
        }
    */


    private fun toggleChecklist() {
        viewModelScope.launch {
            val currentState = uiState.value
            val isChecklist = !currentState.isCheckList

            if (isChecklist) {
                // Save note first if new
                val noteId = currentState.id
                val existingItems = currentState.checklistItems

                // Use existing items if present, otherwise convert content
                val checklistItems = existingItems.ifEmpty {
                    currentState.content
                        .split("\n")
                        .filter { it.isNotBlank() }
                        .mapIndexed { index, line ->
                            Checklist(
                                noteId = noteId,
                                content = line.trim(),
                                position = index
                            )
                        }
                }

                // Insert items if needed
                val insertedItems = if (checklistItems.isEmpty()) {
                    listOf(Checklist(noteId = noteId, content = "", position = 0))
                } else {
                    checklistItems.map { item ->
                        val id = if (item.id == 0L) repo.insertChecklistItem(item) else item.id
                        item.copy(id = id)
                    }
                }

                updateUiStateAndTriggerSave { state ->
                    state.copy(
                        id = noteId,
                        isCheckList = true,
                        checklistItems = insertedItems,
                        content = if (insertedItems.isNotEmpty()) "" else currentState.content
                    )
                }
            } else {
                // Convert checklist items to content
                val content = currentState.checklistItems
                    .sortedBy { it.position }
                    .joinToString("\n") { it.content }

                updateUiStateAndTriggerSave { state ->
                    state.copy(
                        isCheckList = false,
                        checklistItems = emptyList(),
                        content = content
                    )
                }
            }
        }
    }


    private fun removeChecklistItem(index: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (index >= currentState.checklistItems.size) {
                return@launch
            }

            val itemToRemove = currentState.checklistItems[index]
            repo.deleteChecklistItem(itemToRemove.id)

            // Calculate next focus position
            val nextFocusPosition = when {
                index < currentState.checklistItems.size - 1 -> index // Focus next item
                currentState.checklistItems.size > 1 -> index - 1 // Focus previous item
                else -> -1 // No items left, clear focus
            }

            // Update UI immediately with new list and focus position
            updateUiStateAndTriggerSave { state ->
                state.copy(
                    checklistItems = currentState.checklistItems.filterIndexed { i, _ -> i != index },
                    focusedItemPosition = nextFocusPosition
                )
            }
        }
    }


    /*        fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
                viewModelScope.launch {
                    try {
                        val noteWithTags = noteRepository.getNoteWithTagsById(noteId).firstOrNull()
                        updateUiState { it.fromNoteWithTags(noteWithTags) }

                    } catch (e: Exception) {
                        Timber.e(e, "Error preloading note data")
                        // Optionally, you could add error handling in the UI state if needed:
                        // updateUiState { it.copy(error = e.localizedMessage) }
                    }
                }
            }*/


    @OptIn(FlowPreview::class)
    private fun setupNoteUpdateFlow() {
        viewModelScope.launch {
            _noteUpdateTrigger.debounce(500L).distinctUntilChanged().flowOn(Dispatchers.Default)
                .catch { e ->
                    Timber.e(e, "Error in note update flow")
                }.collect { payload ->
                    try {
                        repo.updateNoteWithTagsChecklist(
                            payload.note,
                            payload.tagIds,
                            payload.checklistItems
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating note")
                    }
                }
        }
    }

    fun updateNoteTitle(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                title = newValue.text,
                titleSelection = newValue.selection,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
    }

    fun updateNoteContent(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                content = newValue.text,
                contentSelection = newValue.selection,
                updateDate = NoteUiState.getCurrentFormattedDate()
            )
        }
    }

    private fun updateUiStateAndTriggerSave(update: (NoteUiState) -> NoteUiState) {
        updateUiState(update)
        viewModelScope.launch {
            val currentState = _uiState.value
            try {
                // If it's a new note (id = 0), insert it first
                if (currentState.id == 0L) {
                    val noteId = repo.insertNoteWithTagsChecklist(
                        currentState.toNote(),
                        currentState.selectedTagIds.toList(),
                        currentState.checklistItems
                    )
                    // Update the UI state with the new ID
                    updateUiState { it.copy(id = noteId) }
                } else {
                    // Existing note, update as normal
                    _noteUpdateTrigger.emit(
                        NoteUpdatePayload(
                            note = currentState.toNote(),
                            tagIds = currentState.selectedTagIds.toList(),
                            checklistItems = currentState.checklistItems
                        )
                    )
                    widgetUpdater.updateSingleWidget(currentState.toNote())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving/updating note")
            }
        }
    }

    fun toggleTag(tagId: Long) {
        updateUiStateAndTriggerSave {
            it.copy(selectedTagIds = it.selectedTagIds.toggle(tagId))
        }
    }

    fun togglePinStatus() {
        updateUiStateAndTriggerSave { currentState ->
            val updatedNote = currentState.toNote().copy(isPinned = !currentState.isPinned)
            currentState.copy(
                isPinned = updatedNote.isPinned
            )
        }
    }

    fun updateColor(newColor: Int?) {
        updateUiStateAndTriggerSave { currentState ->
            currentState.copy(
                lightColor = newColor ?: currentState.lightColor,
            )
        }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            repo.updateNoteReminder(noteId, reminderDate)
            repo.getNoteById(noteId).firstOrNull()?.let { updatedNote ->
                updateUiStateAndTriggerSave {
                    it.copy(
                        reminderDate = updatedNote.reminderDate,
                        id = updatedNote.id,
                        isDone = updatedNote.isDone,
                    )
                }
            }
        }
    }

    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = _uiState.value.toNote()
            Timber.tag("DEBUG").d("currentNote.content- [${currentNote.content}]")
            Timber.tag("DEBUG").d("currentNote.title- [${currentNote.title}]")
            if (currentNote.title.isBlank() && currentNote.content.isBlank()) {
                onSkip()
                Timber.tag("DEBUG").d("[ onSkip() ]")
            } else {
                saveOrUpdateNote(currentNote)
                onComplete()
                Timber.tag("DEBUG").d("[ onComplete()() ]")
            }
        }
    }

    private suspend fun saveOrUpdateNote(note: Note) {
        if (note.id == 0L) {
            repo.insertNoteWithTagsChecklist(
                note,
                _uiState.value.selectedTagIds.toList(),
                _uiState.value.checklistItems
            )
        } else {
            _noteUpdateTrigger.emit(
                NoteUpdatePayload(
                    note = note,
                    tagIds = _uiState.value.selectedTagIds.toList(),
                    checklistItems = _uiState.value.checklistItems
                )
            )
            widgetUpdater.updateSingleWidget(note)

        }
    }

    fun handleNoteAction(
        action: NoteAction,
        onComplete: (noteId: Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val noteId = _uiState.value.id

                // Update UI state based on action type
                updateUiState {
                    when (action) {
                        is NoteAction.Delete -> it.copy(
                            isTrashed = true,
                            isPinned = false
                        )

                        is NoteAction.Archive -> it.copy(
                            isArchived = true,
                            isPinned = false
                        )

                        is NoteAction.Unarchive -> it.copy(isArchived = false)
                    }
                }
                // Navigate with action
                onComplete(noteId)
            } catch (e: Exception) {
                Timber.e(e, "Error handling note action: ${action.message}")
                // Revert UI state on error
                updateUiState {
                    it.copy(
                        isArchived = _uiState.value.isArchived,
                        isTrashed = _uiState.value.isTrashed,
                    )
                }
            }
        }
    }

    fun deleteNoteIfEmpty() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isEmpty()) {
                repo.deleteNote(currentState.toNote())
            }
        }
    }


    private fun fetchTags() {
        viewModelScope.launch {
            repo.getAllTags().catch { e -> Timber.e(e, "Error fetching tags") }
                .collect { tags ->
                    updateUiState { it.copy(allTags = tags) }
                }
        }
    }

    private fun updateUiState(update: (NoteUiState) -> NoteUiState) {
        _uiState.update(update)

    }

    private fun Set<Long>.toggle(id: Long) = if (contains(id)) minus(id) else plus(id)

    private data class NoteUpdatePayload(
        val note: Note, val tagIds: List<Long>, val checklistItems: List<Checklist> = emptyList()
    )

    fun deleteNoteForever(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                repo.deleteNote(currentNote)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting note permanently")
            }
        }
    }

    fun restoreFromTrash(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                val restoredNote = currentNote.copy(isTrashed = false)
                repo.updateNotes(listOf(restoredNote))
                updateUiState { it.copy(isTrashed = false) }
                widgetUpdater.updateSingleWidget(restoredNote)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Error restoring note from trash")
            }
        }
    }

    fun undoRestore() {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                val trashedNote = currentNote.copy(isTrashed = true)
                repo.updateNotes(listOf(trashedNote))
                updateUiState { it.copy(isTrashed = true) }
                widgetUpdater.updateSingleWidget(trashedNote)
            } catch (e: Exception) {
                Timber.e(e, "Error undoing restore")
            }
        }
    }
}


// Main UI state
data class NoteUiState(
    val isLoading: Boolean = true,
    val id: Long = 0,
    val title: String = "",
    val titleSelection: TextRange = TextRange(0),
    val content: String = "",
    val contentSelection: TextRange = TextRange(0),
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Long? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Long? = null,
    val isDone: Boolean = false,
    val updateDate: String = "",
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val isCheckList: Boolean = false,
    val checklistItems: List<Checklist> = emptyList(),
    val focusedItemPosition: Int = -1,
) {
    fun toNote() = Note(
        id = id,
        title = title,
        content = content,
        lightColor = lightColor,
        isPinned = isPinned,
        pinnedDate = pinnedDate,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        updateDate = updateDate,
        isChecklist = isCheckList,
    )

    fun isEmpty() = title.isBlank() && content.isBlank() && checklistItems.isEmpty()

    fun fromNoteWithTags(noteWithTags: NoteWithTags?) =
        noteWithTags?.let {
            copy(
                id = it.note.id,
                title = it.note.title,
                content = it.note.content,
                lightColor = it.note.lightColor,
                isPinned = it.note.isPinned,
                pinnedDate = it.note.pinnedDate,
                isArchived = it.note.isArchived,
                isTrashed = it.note.isTrashed,
                reminderDate = it.note.reminderDate,
                isDone = it.note.isDone,
                updateDate = it.note.updateDate,
                selectedTagIds = it.tags.map { tag -> tag.id }.toSet(),
                isCheckList = it.note.isChecklist,
                checklistItems = checklistItems,
                focusedItemPosition = focusedItemPosition
            )
        } ?: this

    private fun toTextFieldValue(text: String, selection: TextRange) = TextFieldValue(
        text = text, selection = selection
    )

    val titleFieldValue: TextFieldValue
        get() = toTextFieldValue(title, titleSelection)

    val contentFieldValue: TextFieldValue
        get() = toTextFieldValue(content, contentSelection)

    companion object {
        fun getCurrentFormattedDate(): String =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        private fun generateRandomColor(): Int = colorPalette.random().toArgb()
    }

    // Add initialization logic for new notes
    fun initializeNewNote() = copy(
        // lightColor = generateRandomColor(),
        lightColor = 0, // Default color (0 means use theme color)
        updateDate = getCurrentFormattedDate(),
        pinnedDate = System.currentTimeMillis(),
        isLoading = false
    )
}


sealed interface CheckListEvent {
    data object ToggleChecklist : CheckListEvent
    data class ReorderChecklistItems(val fromPosition: Int, val toPosition: Int) : CheckListEvent
    data class AddChecklistItemAt(val position: Int) : CheckListEvent
    data class RemoveChecklistItem(val index: Int) : CheckListEvent
    data class UpdateChecklistItemContent(val item: Checklist, val content: String) :
        CheckListEvent

    data class ChecklistItemChecked(val item: Checklist) : CheckListEvent
    data class UpdateFocusedPosition(val position: Int) : CheckListEvent
}


