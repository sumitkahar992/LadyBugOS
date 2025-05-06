package com.despicable.feature.detail


import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.Checklist
import com.despicable.core.model.HabitItem
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.NoteContent
import com.despicable.core.model.NoteType
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


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

        Log.e("DETAIL", "NoteDetailViewModel: NoteId=[$noteId]")


        fetchTags()


        if (noteId > 0) {
            loadNoteById(noteId)   // Load existing note
        } else {
            updateUiState {       // Initialize new note
                it.initializeNewNote()
            }
        }

        setupNoteUpdateFlow()
    }


    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            repo.getNoteCompleteById(id)
                .catch { e ->
                    Log.e("DETAIL", "Error loading note #$id")
                    updateUiState { it.copy(isLoading = false) }

                }
                .firstOrNull()?.let { noteComplete ->
                    updateUiState {
                        it.fromNoteComplete(noteComplete)
                    }

                    Log.e("DETAIL", "Title = [${noteComplete.note.title}]")
                    Log.e("DETAIL", "Content = [${noteComplete.note.content}]")


                }
        }
    }

    fun makeNoteCopy(note: Note, onCopySuccess: () -> Unit) {

        val copiedNote = note.copy(
            id = 0,
            title = "${note.title} (Copy)",  // Add "(Copy)" to title for clarity
            updateDate = Clock.System.now()
        )

        // Create copies of the checklist items with reset IDs
        val copiedChecklistItems = uiState.value.checklistItems.map { it.copy(id = 0, noteId = 0) }

        // Create copies of habit items with reset IDs
        val copiedHabitItems = uiState.value.habitItems.map { item ->
            item.copy(
                id = 0,  // Reset ID so Room will generate a new one
                noteId = 0  // This will be updated after note insertion
            )
        }

        viewModelScope.launch {
            try {
                val newNoteId = repo.insertNoteWithTagsChecklist(
                    copiedNote,
                    uiState.value.selectedTagIds.toList(),
                    copiedChecklistItems,
                    copiedHabitItems
                )

                // Log success
                Log.e("DETAIL", "Note copied successfully with new ID: $newNoteId")
                onCopySuccess()
            } catch (e: Exception) {
                Log.e("DETAIL", "Error copying note: ${e.message}")
            }

        }
    }

    /*    private fun loadNoteById(id: Long) {
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
        }*/


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

            // Update positions
            val updatedItems = currentItems.mapIndexed { index, checklistItem ->
                checklistItem.copy(position = index)
            }

            // Update UI state
            updateUiStateAndTriggerSave { state ->
                state.copy(
                    checklistItems = updatedItems,
                    noteContent = NoteContent.ChecklistItems(updatedItems), // Keep in sync
                    focusedItemPosition = newFocusPosition
                )
            }

            // Update database in background without creating new coroutine
            withContext(Dispatchers.IO) {
                repo.updateAllChecklistItems(updatedItems)
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
                    noteContent = NoteContent.ChecklistItems(updatedItems) // Keep in sync
                )
            }
        }
    }

    private fun addChecklistItemAt(position: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Ensure the note is saved first if it's a new note
            val noteId = if (currentState.id == 0L) {
                // Save the note first if it hasn't been saved
                repo.insertNoteWithTagsChecklist(
                    currentState.toNote(),
                    emptyList(),
                    currentState.checklistItems,
                    currentState.habitItems
                )
            } else {
                currentState.id
            }

            Log.e("DETAIL", "=--noteId--==[$noteId]")

            // Create new item
            val newItem = Checklist(
                noteId = noteId,
                content = "",
                position = position
            )

            // Update positions of existing items in the database
            val existingItems = currentState.checklistItems
            existingItems.forEachIndexed { index, item ->
                if (index >= position) {
                    val updatedItem = item.copy(position = index + 1)
                    repo.updateChecklistItem(updatedItem)
                }
            }

            // Insert new item
            val insertedId = repo.insertChecklistItem(newItem)
            val insertedItem = newItem.copy(id = insertedId)

            // Update UI with new list
            val updatedItems = currentState.checklistItems.toMutableList()
            updatedItems.add(position, insertedItem)

            updateUiStateAndTriggerSave { state ->
                state.copy(
                    id = noteId,
                    checklistItems = updatedItems,
                    noteContent = NoteContent.ChecklistItems(updatedItems), // Keep in sync
                    focusedItemPosition = position
                )
            }
        }
    }

    private fun updateChecklistItemContent(item: Checklist, content: String) {
        viewModelScope.launch {
            val updatedItem = item.copy(content = content)
            repo.updateChecklistItem(updatedItem)

            updateUiStateAndTriggerSave(updateTimestamp = true) { state ->
                val updatedItems = state.checklistItems.map {
                    if (it.id == item.id) updatedItem else it
                }
                state.copy(
                    checklistItems = updatedItems,
                    noteContent = NoteContent.ChecklistItems(updatedItems) // Keep in sync
                )
            }
        }
    }


    private fun toggleChecklist() {
        viewModelScope.launch {
            val currentState = uiState.value
            val isConvertingToChecklist = currentState.noteType != NoteType.CHECKLIST

            if (isConvertingToChecklist) {
                // Converting from text to checklist
                val noteId = currentState.id

                // Extract text content
                val textContent = when (currentState.noteContent) {
                    is NoteContent.Text -> currentState.noteContent.text
                    is NoteContent.ChecklistItems -> currentState.noteContent.items.joinToString("\n") { it.content }
                }

                // Create checklist items from text content
                val checklistItems = currentState.checklistItems.ifEmpty {
                    textContent
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

                updateUiStateAndTriggerSave(
                    updateTimestamp = true
                ) { state ->
                    state.copy(
                        id = noteId,
                        checklistItems = insertedItems,
                        noteContent = NoteContent.ChecklistItems(insertedItems),
                        noteType = NoteType.CHECKLIST
                    )
                }
            } else {
                // Converting from checklist to text
                // Extract text from checklist items
                val content = currentState.checklistItems
                    .sortedBy { it.position }
                    .joinToString("\n") { it.content }

                updateUiStateAndTriggerSave(
                    updateTimestamp = true
                ) { state ->
                    state.copy(
                        checklistItems = emptyList(),
                        noteContent = NoteContent.Text(content),
                        noteType = NoteType.TEXT
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

            // Update checklist items
            val updatedItems = currentState.checklistItems.filterIndexed { i, _ -> i != index }

            // Fix positions after removal
            val reindexedItems = updatedItems.mapIndexed { i, item ->
                item.copy(position = i)
            }

            // Update UI immediately with new list and focus position
            updateUiStateAndTriggerSave { state ->
                state.copy(
                    checklistItems = reindexedItems,
                    noteContent = if (reindexedItems.isEmpty()) {
                        NoteContent.Text("") // Switch to empty text if all items removed
                    } else {
                        NoteContent.ChecklistItems(reindexedItems)
                    },
                    noteType = if (reindexedItems.isEmpty()) NoteType.TEXT else NoteType.CHECKLIST,
                    focusedItemPosition = nextFocusPosition
                )
            }

            // Update positions in database
            if (updatedItems.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    repo.updateAllChecklistItems(reindexedItems)
                }
            }
        }
    }


/*    fun preloadNoteData(noteId: Long, glanceId: GlanceId) {
        viewModelScope.launch {
            try {
                val noteWithTags = noteRepository.getNoteWithTagsById(noteId).firstOrNull()
                updateUiState { it.fromNoteWithTags(noteWithTags) }

            } catch (e: Exception) {
                Log.e("DETAIL", "$e : Error preloading note data")
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
                    Log.e("DETAIL", "Error in note update flow")
                }
                .collect { payload ->
                    try {
                        repo.updateNoteWithTagsChecklist(
                            payload.note,
                            payload.tagIds,
                            payload.checklistItems,
                            payload.updateTimestamp
                        )
                    } catch (e: Exception) {
                        Log.e("DETAIL", "Error updating note")
                    }
                }
        }
    }

    fun updateNoteTitle(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                title = newValue.text,
                titleSelection = newValue.selection,
            )
        }
    }

    fun updateNoteContent(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            when (it.noteType) {
                NoteType.TEXT -> {
                    it.copy(
                        noteContent = NoteContent.Text(newValue.text),
                        contentSelection = newValue.selection
                    )
                }

                NoteType.CHECKLIST -> {
                    if (it.focusedItemPosition >= 0) {
                        // Update specific checklist item
                        val updatedItems = it.checklistItems.toMutableList()
                        if (it.focusedItemPosition < updatedItems.size) {
                            val currentItem = updatedItems[it.focusedItemPosition]
                            updatedItems[it.focusedItemPosition] =
                                currentItem.copy(content = newValue.text)

                            // Update the checklist content
                            it.copy(
                                checklistItems = updatedItems,
                                noteContent = NoteContent.ChecklistItems(updatedItems),
                                contentSelection = newValue.selection
                            )
                        } else {
                            it // No change if position is invalid
                        }
                    } else {
                        it // No change if no item is focused
                    }
                }
            }
        }
    }

    /*    fun updateNoteContent(newValue: TextFieldValue) {
            updateUiStateAndTriggerSave { currentState ->
                when (currentState.noteType) {
                    NoteType.TEXT -> {
                        // For text notes, update the text content
                        val newNoteContent = (currentState.noteContent as NoteContent.Text).copy(text = newValue.text)
                        currentState.copy(
                            noteContent = newNoteContent,
                            contentSelection = newValue.selection
                        )
                    }
                    NoteType.CHECKLIST -> {
                        // For checklist notes, update the focused item's text if valid
                        if (currentState.focusedItemPosition >= 0 && currentState.noteContent is NoteContent.Checklist) {
                            val checklist = currentState.noteContent
                            if (currentState.focusedItemPosition < checklist.items.size) {
                                val newItems = checklist.items.mapIndexed { index, item ->
                                    if (index == currentState.focusedItemPosition) {
                                        item.copy(content = newValue.text)
                                    } else {
                                        item
                                    }
                                }
                                val newNoteContent = checklist.copy(items = newItems)
                                currentState.copy(
                                    noteContent = newNoteContent,
                                    contentSelection = newValue.selection
                                )
                            } else {
                                currentState // Invalid focusedItemPosition, no change
                            }
                        } else {
                            currentState // No focused item or wrong type, no change
                        }
                    }
                }
            }
        }*/

    private fun updateUiStateAndTriggerSave(
        updateTimestamp: Boolean = true,
        update: (NoteUiState) -> NoteUiState
    ) {
        updateUiState(update)
        viewModelScope.launch {
            val currentState = _uiState.value
            try {
                // If it's a new note (id = 0), insert it first
                if (currentState.id == 0L) {
                    val noteId = repo.insertNoteWithTagsChecklist(
                        currentState.toNote(),
                        currentState.selectedTagIds.toList(),
                        currentState.checklistItems,
                        currentState.habitItems
                    )
                    // Update the UI state with the new ID
                    updateUiState { it.copy(id = noteId) }
                } else {
                    // Existing note, update as normal
                    _noteUpdateTrigger.emit(
                        NoteUpdatePayload(
                            note = currentState.toNote(),
                            tagIds = currentState.selectedTagIds.toList(),
                            checklistItems = currentState.checklistItems,
                            updateTimestamp = updateTimestamp
                        )
                    )
//                    widgetUpdater.updateSingleWidget(
//                        NoteComplete(
//                            note = currentState.toNote(),
//                            checklistItems = currentState.checklistItems,
//                            habitItems = currentState.habitItems
//                        )
//                    )
                    widgetUpdater.updateSingleWidget(currentState.toNote())
                }
            } catch (e: Exception) {
                Log.e("DETAIL", "Error saving/updating note")
            }
        }
    }

    fun toggleTag(tagId: Long) {
        updateUiStateAndTriggerSave(
            updateTimestamp = false // Don't update timestamp for UI toggle
        ) {
            it.copy(selectedTagIds = it.selectedTagIds.toggle(tagId))
        }
    }

    fun togglePinStatus() {
        updateUiStateAndTriggerSave(
            updateTimestamp = false
        ) { currentState ->
            val updatedNote = currentState.toNote().copy(isPinned = !currentState.isPinned)
            currentState.copy(
                isPinned = updatedNote.isPinned,
                pinnedDate = Clock.System.now()
            )
        }
    }

    /*    fun updateColor(newColor: Int?) {
            updateUiStateAndTriggerSave(
                updateTimestamp = false
            ) { currentState ->
                currentState.copy(
                    lightColor = newColor ?: currentState.lightColor,
                )
            }
        }*/

    /**
     * Updates the note's color.
     *
     * @param color The new color to apply, or null to use the default theme color
     */
    fun updateColor(color: Int?) {
        updateUiStateAndTriggerSave(
            updateTimestamp = false
        ) { currentState ->
            currentState.copy(
                lightColor = when (color) {
                    null -> 0 // Default theme color (0 is the special code for default)
                    else -> color
                }
            )
        }
    }

    /**
     * Updates a note's reminder date and refreshes the UI state with the updated values.
     * Does not modify update timestamp as this is a UI-related change.
     *
     * @param noteId The ID of the note to update
     * @param reminderDate The new reminder date, or null to remove reminder
     */
    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            try {
                // Update the reminder in a single database call
                repo.updateNoteReminder(noteId, reminderDate)

                // Get the updated note to refresh UI state
                repo.getNoteById(noteId).firstOrNull()?.let { updatedNote ->
                    // Only update the relevant fields, not the entire state
                    updateUiStateAndTriggerSave(updateTimestamp = false) { currentState ->
                        currentState.copy(
                            reminderDate = updatedNote.reminderDate,
                            isDone = updatedNote.isDone
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DETAIL", "Failed to update reminder for note #$noteId")
            }
        }
    }


    /*    private suspend fun saveOrUpdateNote(note: Note) {
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
                        checklistItems = _uiState.value.checklistItems,
                        updateTimestamp = true
                    )
                )
                widgetUpdater.updateSingleWidget(note)

            }
        }*/
    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = uiState.value.toNote()

            // Check if note is empty before saving
            if (uiState.value.isEmpty()) {
                Log.e("DETAIL", "[ onSkip() ]")
                onSkip()
                return@launch
            }

            saveOrUpdateNote(currentNote)
            Log.e("DETAIL", "[ onComplete() ]")
            onComplete()
        }
    }

    /**
     * Saves or updates a note with optimized database access.
     * For new notes, performs an insert.
     * For existing notes, only updates if there are meaningful changes.
     *
     * @param note The note to save or update
     */
    private suspend fun saveOrUpdateNote(note: Note) {
        try {
            // Handle new notes
            if (note.id == 0L) {
                val newNoteId = repo.insertNoteWithTagsChecklist(
                    note,
                    uiState.value.selectedTagIds.toList(),
                    uiState.value.checklistItems,
                    uiState.value.habitItems
                )

                // Update UI state with the new ID
                _uiState.update { it.copy(id = newNoteId) }
                return
            }

            // For existing notes, check if there are meaningful changes
            val currentState = uiState.value
            val originalNote = repo.getNoteById(note.id).firstOrNull()

            // Determine if there are content changes that should update the timestamp
            val hasContentChanges = originalNote?.let { original ->
                note.title != original.title ||
                        note.content != original.content ||
                        note.noteType != original.noteType ||
                        hasTypeSpecificChanges(note.noteType, currentState)
            } == true

            // Emit update through debounced flow
            _noteUpdateTrigger.emit(
                NoteUpdatePayload(
                    note = note,
                    tagIds = currentState.selectedTagIds.toList(),
                    checklistItems = currentState.checklistItems,
                    updateTimestamp = hasContentChanges
                )
            )

            // Update any widgets associated with this note
//            widgetUpdater.updateSingleWidget(
//                NoteComplete(
//                    note = note,
//                    checklistItems = currentState.checklistItems,
//                    habitItems = currentState.habitItems
//                )
//            )
            widgetUpdater.updateSingleWidget(note)


        } catch (e: Exception) {
            Log.e("DETAIL", "Error saving/updating note #${note.id}")
        }
    }

    /**
     * Helper function to check for type-specific changes
     */
    private fun hasTypeSpecificChanges(
        noteType: NoteType,
        currentState: NoteUiState
    ): Boolean {
        return when (noteType) {
            NoteType.CHECKLIST -> checklistItemsHaveChanged(currentState.checklistItems)
            NoteType.TEXT -> false
        }
    }


    private fun checklistItemsHaveChanged(currentItems: List<Checklist>): Boolean {
        return currentItems.any { it.content.isNotBlank() || it.isChecked }
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
                Log.e("DETAIL", "Error handling note action: ${action.message}")
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
            repo.getAllTags()
                .catch { e -> Log.e("DETAIL", "Error fetching tags") }
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
        val note: Note,
        val tagIds: List<Long>,
        val checklistItems: List<Checklist> = emptyList(),
        val updateTimestamp: Boolean = true
    )

    fun deleteNoteForever(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentNote = _uiState.value.toNote()
                repo.deleteNote(currentNote)
                onComplete()
            } catch (e: Exception) {
                Log.e("DETAIL", "Error deleting note permanently")
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
//                widgetUpdater.updateSingleWidget(
//                    NoteComplete(
//                        note = restoredNote,
//                        checklistItems = _uiState.value.checklistItems,
//                        habitItems = _uiState.value.habitItems
//                    )
//                )
                widgetUpdater.updateSingleWidget(restoredNote)

                onComplete()
            } catch (e: Exception) {
                Log.e("DETAIL", "Error restoring note from trash")
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
//                widgetUpdater.updateSingleWidget(
//                    NoteComplete(
//                        note = trashedNote,
//                        checklistItems = _uiState.value.checklistItems,
//                        habitItems = _uiState.value.habitItems
//                    )
//                )
                widgetUpdater.updateSingleWidget(trashedNote)

            } catch (e: Exception) {
                Log.e("DETAIL", "Error undoing restore")
            }
        }
    }
}


// Main UI state
data class NoteUiState(
    // Status flags
    val isLoading: Boolean = true,

    // UI state for text editing
    val titleSelection: TextRange = TextRange(0),
    val contentSelection: TextRange = TextRange(0),

    // Core note properties
    val id: Long = 0,
    val title: String = "",
    val noteContent: NoteContent = NoteContent.Text(""),
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Instant? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val updateDate: Instant = Clock.System.now(),
    val creationDate: Instant = Clock.System.now(),

    // Reminder state
    val reminderDate: Instant? = null,
    val isDone: Boolean = false,

    // Tag handling
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),

    // Checklist state
    val checklistItems: List<Checklist> = emptyList(),
    val habitItems: List<HabitItem> = emptyList(),
    val focusedItemPosition: Int = -1,
    // Note type
    val noteType: NoteType = NoteType.TEXT
) {
    fun toNote() = Note(
        id = id,
        title = title,
        content = noteContent,
        lightColor = lightColor,
        isPinned = isPinned,
        pinnedDate = pinnedDate,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        creationDate = creationDate,
        updateDate = updateDate,
        noteType = noteType,
    )

    fun isEmpty(): Boolean {
        return title.isBlank() && when (noteContent) {
            is NoteContent.Text -> noteContent.text.isBlank()
            is NoteContent.ChecklistItems -> noteContent.items.isEmpty() || noteContent.items.all { it.content.isBlank() }
        }
    }


    /*    fun fromNoteWithTags(noteWithTags: NoteWithTags?): NoteUiState {

            if (noteWithTags == null) return this

            Timber.tag("DEBUG").d("TAGS[NoteComplete]=[$${noteWithTags.tags.size}]")


            return copy(
                    id = noteWithTags.note.id,
                    title = noteWithTags.note.title,
                    content = noteWithTags.note.content,
                    lightColor = noteWithTags.note.lightColor,
                    isPinned = noteWithTags.note.isPinned,
                    pinnedDate = noteWithTags.note.pinnedDate,
                    isArchived = noteWithTags.note.isArchived,
                    isTrashed = noteWithTags.note.isTrashed,
                    reminderDate = noteWithTags.note.reminderDate,
                    isDone = noteWithTags.note.isDone,
                    updateDate = noteWithTags.note.updateDate,
                    selectedTagIds = noteWithTags.tags.map { tag -> tag.id }.toSet(),
                    isCheckList = noteWithTags.note.isChecklist,
                    checklistItems = checklistItems,
                    focusedItemPosition = focusedItemPosition
                )

        }*/

    fun fromNoteComplete(noteComplete: NoteComplete?): NoteUiState {
        if (noteComplete == null) return this

        Log.e("DETAIL", "TAGS[NoteComplete]=[$${noteComplete.tags.size}]")

        return copy(
            id = noteComplete.note.id,
            title = noteComplete.note.title,
            noteContent = noteComplete.note.content,
            lightColor = noteComplete.note.lightColor,
            isPinned = noteComplete.note.isPinned,
            pinnedDate = noteComplete.note.pinnedDate,
            isArchived = noteComplete.note.isArchived,
            isTrashed = noteComplete.note.isTrashed,
            reminderDate = noteComplete.note.reminderDate,
            isDone = noteComplete.note.isDone,
            updateDate = noteComplete.note.updateDate,
            selectedTagIds = noteComplete.tags.map { tag -> tag.id }.toSet(),
            checklistItems = noteComplete.checklistItems,
            habitItems = noteComplete.habitItems,
            focusedItemPosition = -1, // Reset or adjust as needed
            isLoading = false,
            noteType = noteComplete.note.noteType
        )
    }

    // Computed properties for TextFieldValue (calculated only when accessed)
    val titleFieldValue: TextFieldValue
        get() = TextFieldValue(text = title, selection = titleSelection)

//    val contentFieldValue: TextFieldValue
//        get() = TextFieldValue(text = noteContent, selection = contentSelection)

    fun getContentFieldValue(): TextFieldValue? {
        return when (noteType) {
            NoteType.TEXT -> {
                val textContent = (noteContent as NoteContent.Text).text
                TextFieldValue(text = textContent, selection = contentSelection)
            }

            NoteType.CHECKLIST -> {
                if (focusedItemPosition >= 0 && noteContent is NoteContent.ChecklistItems) {
                    val items = noteContent.items
                    val item = items.getOrNull(focusedItemPosition)
                    item?.let { TextFieldValue(text = it.content, selection = contentSelection) }
                } else null
            }
        }
    }


    // Add initialization logic for new notes
    fun initializeNewNote() = copy(
        lightColor = 0, // Default color (0 means use theme color)
        pinnedDate = Clock.System.now(),
        isLoading = false
    )

}


sealed interface CheckListEvent {
    data object ToggleChecklist : CheckListEvent // error time not updating

    data class ReorderChecklistItems(val fromPosition: Int, val toPosition: Int) : CheckListEvent
    data class ChecklistItemChecked(val item: Checklist) : CheckListEvent
    data class AddChecklistItemAt(val position: Int) : CheckListEvent
    data class RemoveChecklistItem(val index: Int) : CheckListEvent
    data class UpdateFocusedPosition(val position: Int) : CheckListEvent

    data class UpdateChecklistItemContent(val item: Checklist, val content: String) :
        CheckListEvent  // error goes to top

}


/*



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

        fetchTags()


        if (noteId > 0) {
            loadNoteById(noteId)   // Load existing note
        } else {
            updateUiState {       // Initialize new note
                it.initializeNewNote()
            }
        }

        setupNoteUpdateFlow()
    }


    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            repo.getNoteCompleteById(id)
                .catch { e ->
                    Timber.e(e, "Error loading note #$id")
                    updateUiState { it.copy(isLoading = false) }

                }
                .firstOrNull()?.let { noteComplete ->
                    updateUiState {
                        it.fromNoteComplete(noteComplete)
                    }

                }
        }
    }


    /*    private fun loadNoteById(id: Long) {
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
        }*/


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


            updateUiStateAndTriggerSave(updateTimestamp = true) { state ->
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

                updateUiStateAndTriggerSave(
                    updateTimestamp = true
                ) { state ->
                    state.copy(
                        id = noteId,
                        isCheckList = true,
                        checklistItems = insertedItems,
                        content = if (insertedItems.isNotEmpty()) "" else currentState.content,
                    )
                }
            } else {
                // Convert checklist items to content
                val content = currentState.checklistItems
                    .sortedBy { it.position }
                    .joinToString("\n") { it.content }

                updateUiStateAndTriggerSave(
                    updateTimestamp = true  // Don't update timestamp for UI toggle
                ) { state ->
                    state.copy(
                        isCheckList = false,
                        checklistItems = emptyList(),
                        content = content,
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
                }
                .collect { payload ->
                    try {
                        repo.updateNoteWithTagsChecklist(
                            payload.note,
                            payload.tagIds,
                            payload.checklistItems,
                            payload.updateTimestamp
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
            )
        }
    }

    fun updateNoteContent(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                content = newValue.text,
                contentSelection = newValue.selection,
            )
        }
    }

    private fun updateUiStateAndTriggerSave(
        updateTimestamp: Boolean = true,
        update: (NoteUiState) -> NoteUiState
    ) {
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
                            checklistItems = currentState.checklistItems,
                            updateTimestamp = updateTimestamp
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
        updateUiStateAndTriggerSave(
            updateTimestamp = false // Don't update timestamp for UI toggle
        ) {
            it.copy(selectedTagIds = it.selectedTagIds.toggle(tagId))
        }
    }

    fun togglePinStatus() {
        updateUiStateAndTriggerSave(
            updateTimestamp = false
        ) { currentState ->
            val updatedNote = currentState.toNote().copy(isPinned = !currentState.isPinned)
            currentState.copy(
                isPinned = updatedNote.isPinned,
                pinnedDate = System.currentTimeMillis()
            )
        }
    }

    /*    fun updateColor(newColor: Int?) {
            updateUiStateAndTriggerSave(
                updateTimestamp = false
            ) { currentState ->
                currentState.copy(
                    lightColor = newColor ?: currentState.lightColor,
                )
            }
        }*/

    /**
     * Updates the note's color.
     *
     * @param color The new color to apply, or null to use the default theme color
     */
    fun updateColor(color: Int?) {
        updateUiStateAndTriggerSave(
            updateTimestamp = false
        ) { currentState ->
            currentState.copy(
                lightColor = when (color) {
                    null -> 0 // Default theme color (0 is the special code for default)
                    else -> color
                }
            )
        }
    }

    /**
     * Updates a note's reminder date and refreshes the UI state with the updated values.
     * Does not modify update timestamp as this is a UI-related change.
     *
     * @param noteId The ID of the note to update
     * @param reminderDate The new reminder date, or null to remove reminder
     */
    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            try {
                // Update the reminder in a single database call
                repo.updateNoteReminder(noteId, reminderDate)

                // Get the updated note to refresh UI state
                repo.getNoteById(noteId).firstOrNull()?.let { updatedNote ->
                    // Only update the relevant fields, not the entire state
                    updateUiStateAndTriggerSave(updateTimestamp = false) { currentState ->
                        currentState.copy(
                            reminderDate = updatedNote.reminderDate,
                            isDone = updatedNote.isDone
                        )
                    }

                    // Update any associated widgets
                    widgetUpdater.updateSingleWidget(updatedNote)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update reminder for note #$noteId")
            }
        }
    }


    /*    private suspend fun saveOrUpdateNote(note: Note) {
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
                        checklistItems = _uiState.value.checklistItems,
                        updateTimestamp = true
                    )
                )
                widgetUpdater.updateSingleWidget(note)

            }
        }*/
    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = uiState.value.toNote()

            if (currentNote.title.isBlank() && currentNote.content.isBlank()) {
                Timber.tag("DEBUG").d("[ onSkip() ]")
                onSkip()
                return@launch
            }

            saveOrUpdateNote(currentNote)
            Timber.tag("DEBUG").d("[ onComplete() ]")
            onComplete()
        }
    }

    /**
     * Saves or updates a note with optimized database access.
     * For new notes, performs an insert.
     * For existing notes, only updates if there are meaningful changes.
     *
     * @param note The note to save or update
     */
    private suspend fun saveOrUpdateNote(note: Note) {
        try {
            // Handle new notes
            if (note.id == 0L) {
                val newNoteId = repo.insertNoteWithTagsChecklist(
                    note,
                    uiState.value.selectedTagIds.toList(),
                    uiState.value.checklistItems
                )

                // Update UI state with the new ID
                _uiState.update { it.copy(id = newNoteId) }
                return
            }

            // For existing notes, check if there are meaningful changes
            val currentState = uiState.value
            val originalNote = repo.getNoteById(note.id).firstOrNull()

            // Determine if there are content changes that should update the timestamp
            val hasContentChanges = originalNote?.let { original ->
                note.title != original.title ||
                        note.content != original.content ||
                        note.isChecklist != original.isChecklist ||
                        // Consider checklist changes too
                        (note.isChecklist && checklistItemsHaveChanged(currentState.checklistItems))
            } == true

            // Emit update through debounced flow
            _noteUpdateTrigger.emit(
                NoteUpdatePayload(
                    note = note,
                    tagIds = currentState.selectedTagIds.toList(),
                    checklistItems = currentState.checklistItems,
                    updateTimestamp = hasContentChanges
                )
            )

            // Update any widgets associated with this note
            widgetUpdater.updateSingleWidget(note)

        } catch (e: Exception) {
            Timber.e(e, "Error saving/updating note #${note.id}")
        }
    }


    /**
     * Determines if checklist items have had meaningful content changes.
     * This helps decide whether to update the note's timestamp.
     *
     * @param currentItems The current checklist items
     * @return True if items have changed, false otherwise
     */
    private fun checklistItemsHaveChanged(currentItems: List<Checklist>): Boolean {
        // Implementation depends on how you track original checklist state
        // One approach is to store the original items when loading the note
        // and compare against that, or track changes directly

        // Simple implementation - assumes any non-empty items means changes
        return currentItems.any { !it.content.isBlank() || it.isChecked }
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
            repo.getAllTags()
                .catch { e -> Timber.e(e, "Error fetching tags") }
                .collect { tags ->
                    Timber.tag("DEBUG").d("TAGS[fetchTags]=[$${tags.size}]")
                    updateUiState { it.copy(allTags = tags) }
                }
        }
    }

    private fun updateUiState(update: (NoteUiState) -> NoteUiState) {
        val currentState = _uiState.value
        val updatedState = update(currentState)
        Timber.tag("DEBUG")
            .d("State update: allTags before=[${currentState.allTags.size}], after=[${updatedState.allTags.size}]")
        _uiState.value = updatedState
    }
//    private fun updateUiState(update: (NoteUiState) -> NoteUiState) {
//        _uiState.update(update)
//
//    }

    private fun Set<Long>.toggle(id: Long) = if (contains(id)) minus(id) else plus(id)

    private data class NoteUpdatePayload(
        val note: Note,
        val tagIds: List<Long>,
        val checklistItems: List<Checklist> = emptyList(),
        val updateTimestamp: Boolean = true
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
    // Status flags
    val isLoading: Boolean = true,

    // UI state for text editing
    val titleSelection: TextRange = TextRange(0),
    val contentSelection: TextRange = TextRange(0),

    // Core note properties
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Long? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val updateDate: Long = System.currentTimeMillis(),

    // Reminder state
    val reminderDate: Long? = null,
    val isDone: Boolean = false,

    // Tag handling
    val allTags: List<Tag> = listOf(Tag(1, "www"), Tag(2, "XXX")),
    val selectedTagIds: Set<Long> = emptySet(),

    // Checklist state
    val isCheckList: Boolean = false,
    val checklistItems: List<Checklist> = emptyList(),
    val focusedItemPosition: Int = -1,
    val noteType: NoteType = NoteType.TEXT_NOTE
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
        noteType = noteType,
    )

    fun isEmpty(): Boolean {
        return title.isBlank() && content.isBlank() &&
                (checklistItems.isEmpty() || checklistItems.all { it.content.isBlank() })
    }


    /*    fun fromNoteWithTags(noteWithTags: NoteWithTags?): NoteUiState {

            if (noteWithTags == null) return this

            Timber.tag("DEBUG").d("TAGS[NoteComplete]=[$${noteWithTags.tags.size}]")


            return copy(
                    id = noteWithTags.note.id,
                    title = noteWithTags.note.title,
                    content = noteWithTags.note.content,
                    lightColor = noteWithTags.note.lightColor,
                    isPinned = noteWithTags.note.isPinned,
                    pinnedDate = noteWithTags.note.pinnedDate,
                    isArchived = noteWithTags.note.isArchived,
                    isTrashed = noteWithTags.note.isTrashed,
                    reminderDate = noteWithTags.note.reminderDate,
                    isDone = noteWithTags.note.isDone,
                    updateDate = noteWithTags.note.updateDate,
                    selectedTagIds = noteWithTags.tags.map { tag -> tag.id }.toSet(),
                    isCheckList = noteWithTags.note.isChecklist,
                    checklistItems = checklistItems,
                    focusedItemPosition = focusedItemPosition
                )

        }*/

    fun fromNoteComplete(noteComplete: NoteComplete?): NoteUiState {
        if (noteComplete == null) return this

        Timber.tag("DEBUG").d("TAGS[NoteComplete]=[$${noteComplete.tags.size}]")


        return copy(
            id = noteComplete.note.id,
            title = noteComplete.note.title,
            content = noteComplete.note.content,
            lightColor = noteComplete.note.lightColor,
            isPinned = noteComplete.note.isPinned,
            pinnedDate = noteComplete.note.pinnedDate,
            isArchived = noteComplete.note.isArchived,
            isTrashed = noteComplete.note.isTrashed,
            reminderDate = noteComplete.note.reminderDate,
            isDone = noteComplete.note.isDone,
            updateDate = noteComplete.note.updateDate,
            selectedTagIds = noteComplete.tags.map { tag -> tag.id }.toSet(),
            isCheckList = noteComplete.note.noteType == NoteType.CHECKLIST,
            checklistItems = noteComplete.checklistItems,
            focusedItemPosition = focusedItemPosition,
            isLoading = false
        )
    }

    // Computed properties for TextFieldValue (calculated only when accessed)
    val titleFieldValue: TextFieldValue
        get() = TextFieldValue(text = title, selection = titleSelection)

    val contentFieldValue: TextFieldValue
        get() = TextFieldValue(text = content, selection = contentSelection)


    // Add initialization logic for new notes
    fun initializeNewNote() = copy(
        lightColor = 0, // Default color (0 means use theme color)
        pinnedDate = System.currentTimeMillis(),
        isLoading = false
    )

}


sealed interface CheckListEvent {
    data object ToggleChecklist : CheckListEvent // error time not updating

    data class ReorderChecklistItems(val fromPosition: Int, val toPosition: Int) : CheckListEvent
    data class ChecklistItemChecked(val item: Checklist) : CheckListEvent
    data class AddChecklistItemAt(val position: Int) : CheckListEvent
    data class RemoveChecklistItem(val index: Int) : CheckListEvent
    data class UpdateFocusedPosition(val position: Int) : CheckListEvent

    data class UpdateChecklistItemContent(val item: Checklist, val content: String) :
        CheckListEvent  // error goes to top

}




 */


/*





class NoteDetailViewModel(
    private val repo: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _noteUpdateTrigger = MutableSharedFlow<NoteUpdatePayload>()

    private val transactionMutex = Mutex()


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

    private fun <T> executeTransaction(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(saveStatus = SaveStatus.SAVING) }

                val result = withContext(Dispatchers.IO) {
                    transactionMutex.withLock { operation() }
                }

                updateUiState { it.copy(saveStatus = SaveStatus.SAVED) }
                onSuccess(result)

                delay(1000)
                updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
            } catch (e: Exception) {
                Timber.e(e, "Transaction failed: ${e.message}")
                updateUiState { it.copy(saveStatus = SaveStatus.ERROR) }

                delay(2000)
                updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
            }
        }
    }

    // First, create a unified handler for checklist operations
// Updated executeChecklistOperation to support suspend functions in onSuccess callback
    private fun <T> executeChecklistOperation(
        operation: suspend () -> T,
        onSuccess: suspend (T) -> Unit = {}, // Changed to suspend lambda
        withStatus: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (withStatus) updateUiState { it.copy(saveStatus = SaveStatus.SAVING) }

                val result = operation()
                onSuccess(result) // Now can safely call suspend functions

                if (withStatus) {
                    updateUiState { it.copy(saveStatus = SaveStatus.SAVED) }
                    delay(1000)
                    updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in checklist operation: ${e.message}")
                if (withStatus) {
                    updateUiState { it.copy(saveStatus = SaveStatus.ERROR) }
                    delay(2000)
                    updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
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

    // working
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
            updateUiStateAndTriggerSave(updateTimestamp = true) { state ->
                state.copy(
                    checklistItems = currentItems,
                    focusedItemPosition = newFocusPosition,
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


    // No changes needed to toggleChecklistItem itself
    private fun toggleChecklistItem(item: Checklist) {
        // Update UI immediately for responsiveness
        val updatedItems = uiState.value.checklistItems.map {
            if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
        }

        updateUiState { it.copy(checklistItems = updatedItems) }

        // Then perform the database update
        val noteId = uiState.value.id
        if (item.id > 0 && noteId > 0) {
            executeChecklistOperation(
                operation = { repo.toggleChecklistItem(noteId, item.id) },
                onSuccess = {
                    // Now this can call suspend functions
                    widgetUpdater.updateSingleWidget(uiState.value.toNote())
                },
                withStatus = false // Skip status updates for toggle operations
            )
        }
    }

    // Simplify addChecklistItemAt
    private fun addChecklistItemAt(position: Int) {
        executeChecklistOperation(
            operation = {
                val currentState = _uiState.value

                // Ensure note exists
                val noteId = if (currentState.id == 0L) {
                    repo.insertNoteWithTagsChecklist(
                        currentState.toNote(),
                        currentState.selectedTagIds.toList(),
                        currentState.checklistItems
                    )
                } else {
                    currentState.id
                }

                // Return both the note ID and the new item
                Pair(noteId, repo.addChecklistItem(noteId, "", position))
            },
            onSuccess = { (noteId, newItem) ->
                // Update UI with new item
                val updatedItems = _uiState.value.checklistItems.toMutableList()
                updatedItems.add(position, newItem)

                updateUiState {
                    it.copy(
                        id = noteId,
                        checklistItems = updatedItems,
                        focusedItemPosition = position
                    )
                }
            }
        )
    }

    // Simplify updateChecklistItemContent to remove redundancy
    private fun updateChecklistItemContent(item: Checklist, content: String) {
        // Update UI immediately for responsiveness
        val updatedItem = item.copy(content = content)
        val updatedItems = uiState.value.checklistItems.map {
            if (it.id == item.id) updatedItem else it
        }

        updateUiState { state -> state.copy(checklistItems = updatedItems) }

        // Debounce database updates with simpler implementation
        val noteId = uiState.value.id
        if (item.id > 0 && noteId > 0) {
            viewModelScope.launch {
                try {
                    delay(300) // Only save after typing pause
                    repo.updateChecklistItemContent(noteId, item.id, content)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating checklist content: ${e.message}")
                }
            }
        }
    }


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

                updateUiStateAndTriggerSave(
                    updateTimestamp = false
                ) { state ->
                    state.copy(
                        id = noteId,
                        isCheckList = true,
                        checklistItems = insertedItems,
                        content = if (insertedItems.isNotEmpty()) "" else currentState.content,
                    )
                }
            } else {
                // Convert checklist items to content
                val content = currentState.checklistItems
                    .sortedBy { it.position }
                    .joinToString("\n") { it.content }

                updateUiStateAndTriggerSave(
                    updateTimestamp = false  // Don't update timestamp for UI toggle
                ) { state ->
                    state.copy(
                        isCheckList = false,
                        checklistItems = emptyList(),
                        content = content,
                    )
                }
            }
        }
    }


    // Simplify removeChecklistItem
    private fun removeChecklistItem(index: Int) {
        val currentState = _uiState.value
        val noteId = currentState.id

        if (noteId == 0L || index >= currentState.checklistItems.size) {
            return
        }

        // Calculate next focus position before the item is removed
        val nextFocusPosition = when {
            index < currentState.checklistItems.size - 1 -> index // Focus next item
            currentState.checklistItems.size > 1 -> index - 1 // Focus previous item
            else -> -1 // No items left, clear focus
        }

        executeChecklistOperation(
            operation = { repo.removeChecklistItem(noteId, index) },
            onSuccess = { updatedItems ->
                updateUiState {
                    it.copy(
                        checklistItems = updatedItems,
                        focusedItemPosition = nextFocusPosition
                    )
                }
            }
        )
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
                    updateUiState { it.copy(saveStatus = SaveStatus.ERROR) }

                    // Reset error status after delay
                    delay(2000)
                    updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
                }
                .collect { payload ->
                    try {
                        // Update status to saving if not already
                        if (_uiState.value.saveStatus != SaveStatus.SAVING) {
                            updateUiState { it.copy(saveStatus = SaveStatus.SAVING) }
                        }

                        withContext(Dispatchers.IO) {
                            repo.updateNoteWithTagsChecklist(
                                payload.note,
                                payload.tagIds,
                                payload.checklistItems,
                                payload.updateTimestamp
                            )
                        }

                        // Update status to saved
                        updateUiState { it.copy(saveStatus = SaveStatus.SAVED) }

                        // Reset status after delay
                        delay(1000)
                        updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating note")
                        updateUiState { it.copy(saveStatus = SaveStatus.ERROR) }

                        // Reset error status after delay
                        delay(2000)
                        updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
                    }
                }

        }
    }

    fun updateNoteTitle(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                title = newValue.text,
                titleSelection = newValue.selection,
            )
        }
    }

    fun updateNoteContent(newValue: TextFieldValue) {
        updateUiStateAndTriggerSave {
            it.copy(
                content = newValue.text,
                contentSelection = newValue.selection,
            )
        }
    }

/*    private fun updateUiStateAndTriggerSave(
        updateTimestamp: Boolean = true,
        update: (NoteUiState) -> NoteUiState
    ) {
        // Update UI state first with saving indicator
        updateUiState {
            update(it).copy(saveStatus = SaveStatus.SAVING)
        }

        // Only trigger save for non-empty notes
        val currentState = _uiState.value
        if (currentState.isEmpty()) {
            // Reset status for empty notes
            updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
            return
        }

        viewModelScope.launch {
            try {
                // If it's a new note (id = 0), insert it first
                if (currentState.id == 0L) {
                    val noteId = withContext(Dispatchers.IO) {
                        repo.insertNoteWithTagsChecklist(
                            currentState.toNote(),
                            currentState.selectedTagIds.toList(),
                            currentState.checklistItems
                        )
                    }

                    // Update the UI state with the new ID and saved status
                    updateUiState { it.copy(id = noteId, saveStatus = SaveStatus.SAVED) }

                    // Reset status after delay
                    delay(1000)
                    updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
                } else {
                    // Existing note, update via debounced flow
                    _noteUpdateTrigger.emit(
                        NoteUpdatePayload(
                            note = currentState.toNote(),
                            tagIds = currentState.selectedTagIds.toList(),
                            checklistItems = currentState.checklistItems,
                            updateTimestamp = updateTimestamp
                        )
                    )

                    // For existing notes, status is managed by the note update flow
                    // that collects from _noteUpdateTrigger

                    // Consider moving widget update to after db confirmation
                    widgetUpdater.updateSingleWidget(currentState.toNote())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving/updating note")
                updateUiState { it.copy(saveStatus = SaveStatus.ERROR) }

                // Reset error status after delay
                delay(2000)
                updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
            }
        }
    }*/

    private fun updateUiStateAndTriggerSave(
        updateTimestamp: Boolean = true,
        update: (NoteUiState) -> NoteUiState
    ) {
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
                            checklistItems = currentState.checklistItems,
                            updateTimestamp = updateTimestamp
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
        updateUiStateAndTriggerSave(
            updateTimestamp = false // Don't update timestamp for UI toggle
        ) {
            it.copy(selectedTagIds = it.selectedTagIds.toggle(tagId))
        }
    }

    fun togglePinStatus() {
        // Store original state for rollback
        val originalState = _uiState.value

        // Update UI immediately
        updateUiState { currentState ->
            val updatedNote = currentState.toNote().copy(isPinned = !currentState.isPinned)
            currentState.copy(
                isPinned = updatedNote.isPinned,
                pinnedDate = updatedNote.pinnedDate,
                saveStatus = SaveStatus.SAVING  // Use SaveStatus instead of isUpdating
            )
        }

        // Perform database update
        viewModelScope.launch {
            try {
                // Update the database
                withContext(Dispatchers.IO) {
                    repo.updateNotes(listOf(_uiState.value.toNote()))
                }

                // Update widgets
                widgetUpdater.updateSingleWidget(_uiState.value.toNote())

                // Update to saved status
                updateUiState { it.copy(saveStatus = SaveStatus.SAVED) }

                // Reset status after delay
                delay(1000)
                updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling pin status")

                // Rollback to original state with error
                updateUiState {
                    originalState.copy(saveStatus = SaveStatus.ERROR)
                }

                // Clear error after delay
                delay(2000)
                updateUiState { it.copy(saveStatus = SaveStatus.IDLE) }
            }
        }
    }


    fun updateColor(color: Int?) {
        updateUiStateAndTriggerSave(
            updateTimestamp = false
        ) { currentState ->
            currentState.copy(
                lightColor = when (color) {
                    null -> 0 // Default theme color (0 is the special code for default)
                    else -> color
                }
            )
        }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            repo.updateNoteReminder(noteId, reminderDate)
            repo.getNoteById(noteId).firstOrNull()?.let { updatedNote ->
                updateUiStateAndTriggerSave(
                    updateTimestamp = false
                ) {
                    it.copy(
                        reminderDate = updatedNote.reminderDate,
                        id = updatedNote.id,
                        isDone = updatedNote.isDone,
                    )
                }
            }
        }
    }

    // Update saveNote to use executeTransaction
    fun saveNote(onComplete: () -> Unit, onSkip: () -> Unit) {
        viewModelScope.launch {
            val currentNote = _uiState.value.toNote()
            val checklistItems = _uiState.value.checklistItems

            if (currentNote.title.isBlank() && currentNote.content.isBlank() && checklistItems.isEmpty()) {
                Timber.tag("DEBUG").d("[ onSkip() ]")
                onSkip()
                return@launch
            }

            executeTransaction(
                operation = { saveOrUpdateNote(currentNote) },
                onSuccess = { onComplete() }
            )
        }
    }

    // Simplify saveOrUpdateNote to fully utilize smartUpdateNoteWithTagsChecklist
    private suspend fun saveOrUpdateNote(note: Note): Boolean {
        return if (note.id == 0L) {
            // Insert new note
            val newId = repo.insertNoteWithTagsChecklist(
                note,
                uiState.value.selectedTagIds.toList(),
                uiState.value.checklistItems
            )

            // Update UI with new ID
            updateUiState { it.copy(id = newId) }
            true
        } else {
            // Smart update for existing notes
            val wasChanged = repo.smartUpdateNoteWithTagsChecklist(
                note,
                uiState.value.selectedTagIds.toList(),
                uiState.value.checklistItems
            )

            // Only update widget if note was changed
            if (wasChanged) {
                widgetUpdater.updateSingleWidget(note)
            }

            wasChanged
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
        val note: Note,
        val tagIds: List<Long>,
        val checklistItems: List<Checklist> = emptyList(),
        val updateTimestamp: Boolean = true
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

enum class SaveStatus {
    IDLE, SAVING, SAVED, ERROR
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
    val updateDate: Long = System.currentTimeMillis(),
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val isCheckList: Boolean = false,
    val checklistItems: List<Checklist> = emptyList(),
    val focusedItemPosition: Int = -1,
    val saveStatus: SaveStatus = SaveStatus.IDLE
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


    // Add initialization logic for new notes
    fun initializeNewNote() = copy(
        lightColor = 0, // Default color (0 means use theme color)
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




 */
