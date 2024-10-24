package com.example.ladybugos.ui.drawer.home


import androidx.annotation.Keep
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.LoadSampleDataUseCase
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.theme.Theme
import com.example.ladybugos.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber


@Keep
class NoteListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repo: SettingsRepo,
    private val widgetUpdater: WidgetUpdater,
    private val noteRepository: NoteRepository,
    private val loadSampleDataUseCase: LoadSampleDataUseCase
) : ViewModel() {

    val theme = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

    val gridLayout = repo.get { gridLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GridLayout.TwoColumns)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    val tags = noteRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val notesWithTags = noteRepository.getAllNotesWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    val filteredNotes = createFilteredNotesFlow(isArchived = false, isReminder = false)
    val archivedNotes = createFilteredNotesFlow(isArchived = true, isReminder = false)
    val upcomingReminders =
        createFilteredNotesFlow(isArchived = false, isReminder = true, isDone = false)
    val completedReminders =
        createFilteredNotesFlow(isArchived = false, isReminder = true, isDone = true)

    private var lastModifiedNotes: List<Note>? = null
    var originalNote = MutableStateFlow(Note())
    private var tagIds = emptyList<Long>()


    init {


        initializeViewModel(savedStateHandle)

        viewModelScope.launch {
            loadSampleDataUseCase()
        }
    }


    private fun initializeViewModel(savedStateHandle: SavedStateHandle) {
        viewModelScope.launch {
            handleDeletedId(savedStateHandle)
        }
    }

    private fun handleDeletedId(savedStateHandle: SavedStateHandle) {
        val deletedId = savedStateHandle.toRoute<Screen.NoteList>().deletedId
        if (deletedId != null) loadNoteById(deletedId)
        Timber.tag("DEBUG").d("NotesViewModel_[deletedId]=[$deletedId]")
    }


    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            noteRepository.getNoteWithTagsById(id).firstOrNull()?.let { loadedNote ->
                originalNote.value = loadedNote.note
                tagIds = loadedNote.tags.map { it.id }
            }
        }
    }


    private fun createFilteredNotesFlow(
        isArchived: Boolean,
        isReminder: Boolean,
        isDone: Boolean = false
    ): StateFlow<List<NoteWithTags>> = combine(
        notesWithTags,
        selectedTagId,
        searchQuery
    ) { notes, tagId, query ->
        notes.filter { noteWithTags ->
            val note = noteWithTags.note
            val matchesTag = tagId == null || noteWithTags.tags.any { it.id == tagId }
            val matchesSearch = note.matchesSearch(query)
            val matchesArchiveStatus = note.isArchived == isArchived
            val matchesReminderStatus = if (isReminder) {
                note.reminderDate != null && (note.isDone == isDone)
            } else true
            !note.isTrashed && matchesArchiveStatus && matchesTag && matchesSearch && matchesReminderStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTag(tagId: Long) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }

    fun updateGridLayout(gridLayout: GridLayout) {
        viewModelScope.launch { repo.setGridLayout(gridLayout) }
    }

    private fun updateNotes(notes: List<Note>, updates: (Note) -> Note) {
        viewModelScope.launch {
            lastModifiedNotes = notes
            val updatedNotes = notes.map(updates)
            noteRepository.updateNotes(updatedNotes)
        }
    }


    fun pinNotes(notes: List<Note>) = updateNotes(notes) { it.copy(isPinned = true) }
    fun unpinNotes(notes: List<Note>) = updateNotes(notes) { it.copy(isPinned = false) }
    fun archiveNotes(notes: List<Note>) =
        updateNotes(notes) { it.copy(isArchived = true, isPinned = false) }

    fun unarchiveNotes(notes: List<Note>) = updateNotes(notes) { it.copy(isArchived = false) }

    fun pinAndUnarchiveNotes(notes: List<Note>) =
        updateNotes(notes) { it.copy(isPinned = true, isArchived = false) }

    fun trashNotes(notes: List<Note>) =
        updateNotes(notes) { it.copy(isTrashed = true, isPinned = false) }


    fun restoreDeletedArchivedNote() {
        viewModelScope.launch {
            originalNote.update { it.copy(isTrashed = false, isArchived = false) }
            noteRepository.updateNote(originalNote.value, tagIds)
            widgetUpdater.updateSingleWidget(originalNote.value)
        }
    }

    fun undoLastOperation() {
        lastModifiedNotes?.let { notes ->
            updateNotes(notes) { it.copy() }
            lastModifiedNotes = null
        }
    }

    fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            noteRepository.insertTag(Tag(name = name, color = colorPalette.random().toArgb()))
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

}

/*class NotesViewModel(
    private val repo: SettingsRepo,
    private val widgetUpdater: WidgetUpdater,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val loadSampleDataUseCase: LoadSampleDataUseCase
) : ViewModel() {

    val theme: StateFlow<Theme> = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

    val gridLayout: StateFlow<GridLayout> = repo.get { gridLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), GridLayout.TwoColumns)


    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    private var lastDeletedNotes: List<Note>? = null
    private var lastArchivedNotes: List<Note>? = null
    private var lastUnArchivedNotes: List<Note>? = null
    private var lastPinnedUnArchivedNotes: List<Note>? = null

    // Original note state for editing
    var originalNote = MutableStateFlow(Note())
    private var tagIds = emptyList<Long>()


    // Use a single state flow for note data, avoiding recomputation
    private val notesWithTags: StateFlow<List<NoteWithTags>> = noteRepository.getAllNotesWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    val tags: StateFlow<List<Tag>> = noteRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())



    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    val filteredNotes = combine(
        notesWithTags,
        selectedTagId,
        searchQuery
    ) { notes, tagId, query ->
        notes.filter { noteWithTags ->
            val note = noteWithTags.note
            val matchesTag = tagId == null || noteWithTags.tags.any { it.id == tagId }
            val matchesSearch = note.matchesSearch(query)
            !note.isTrashed && !note.isArchived && matchesTag && matchesSearch
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val archivedNotes = combine(
        notesWithTags,
        selectedTagId,
        searchQuery
    ) { notes, tagId, query ->
        notes.filter { noteWithTags ->
            val note = noteWithTags.note
            val matchesTag = tagId == null || noteWithTags.tags.any { it.id == tagId }
            val matchesSearch = note.matchesSearch(query)
            note.isArchived && !note.isTrashed && matchesTag && matchesSearch
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )


    // Reminder
    val upcomingReminders = noteRepository.getUpcomingRemindersWithTags().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val completedReminders = noteRepository.getCompletedRemindersWithTags().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )


    init {
        Timber.tag("DEBUG").d("[TAGS]=[${tags.value.size}]")
        initializeViewModel(savedStateHandle)

        viewModelScope.launch(Dispatchers.IO) {
            loadSampleDataUseCase()
        }
    }

    // NotesViewModel
    fun updateNoteReminder(
        noteId: Long,
        reminderDate: Long?
    ) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)

        }
    }


    fun toggleTag(tagId: Long) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            noteRepository.insertTag(
                Tag(
                    name = name,
                    color = colorPalette.random().toArgb()
                )
            )
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

    // TAG
    fun updateGridLayout(gridLayout: GridLayout) {
        viewModelScope.launch { repo.setGridLayout(gridLayout) }
    }

    private fun initializeViewModel(savedStateHandle: SavedStateHandle) {
        viewModelScope.launch {
            handleDeletedId(savedStateHandle)
            observeNotes()
        }
    }

    private fun handleDeletedId(savedStateHandle: SavedStateHandle) {
        val deletedId = savedStateHandle.toRoute<Screen.NoteList>().deletedId
        if (deletedId != null) loadNoteById(deletedId)
        Timber.tag("DEBUG").d("NotesViewModel_[deletedId]=[$deletedId]")
    }


    private fun observeNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes.collectLatest { notesList ->
                _allNotes.value = notesList
                updateWidgets()
                Timber.tag("DEBUG").d("[ NOTES ]=[${notesList.size}]")
            }
        }
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            noteRepository.getNoteWithTagsById(id).firstOrNull()?.let { loadedNote ->
                originalNote.value = loadedNote.note
                tagIds = loadedNote.tags.map { it.id }
            }
        }
    }


    fun restoreDeletedNote() {
        viewModelScope.launch {
            originalNote.value = originalNote.value.copy(isTrashed = false)
            noteRepository.updateNote(originalNote.value, tagIds)
            widgetUpdater.updateAllWidgets(_allNotes.value)
        }
    }

    fun restoreArchivedNotes() {
        viewModelScope.launch {
            originalNote.value = originalNote.value.copy(isArchived = false)
            noteRepository.updateNote(originalNote.value, tagIds)
            widgetUpdater.updateAllWidgets(_allNotes.value)
        }
    }


    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun updateNotes(notesToUpdate: List<Note>, updates: (Note) -> Note) {
        viewModelScope.launch {
            val updatedNotes = notesToUpdate.map(updates)
            noteRepository.updateNotes(updatedNotes)
            updateWidgets()
        }
    }

    private suspend fun updateWidgets() {
        widgetUpdater.updateAllWidgets(_allNotes.value)
    }


    fun pinNotes(notes: List<Note>) = updateNotes(notes) { it.copy(isPinned = true) }
    fun unpinNotes(notes: List<Note>) = updateNotes(notes) { it.copy(isPinned = false) }

    fun archiveNotes(notes: List<Note>) {
        lastArchivedNotes = notes
        updateNotes(notes) { it.copy(isArchived = true, isPinned = false) }
    }

    fun unarchiveNotes(notes: List<Note>) {
        lastUnArchivedNotes = notes
        updateNotes(notes) { it.copy(isArchived = false) }
    }

    fun pinAndUnarchiveNotes(notes: List<Note>) {
        lastPinnedUnArchivedNotes = notes
        updateNotes(notes) { it.copy(isPinned = true, isArchived = false) }
    }


    fun trashNotes(notes: List<Note>) {
        lastDeletedNotes = notes
        updateNotes(notes) { it.copy(isTrashed = true, isPinned = false) }
    }

    fun restoreLastDeletedNotes() {
        lastDeletedNotes?.let { notes ->
            updateNotes(notes) { it.copy(isTrashed = false) }
            lastDeletedNotes = null
        }
    }

    fun restoreLastArchivedNotes() {
        lastArchivedNotes?.let { notes ->
            updateNotes(notes) { it.copy(isArchived = false) }
            lastArchivedNotes = null
        }
    }

    fun restoreLastUnArchivedNotes() {
        lastUnArchivedNotes?.let { notes ->
            updateNotes(notes) { it.copy(isArchived = true, isPinned = false) }
            lastUnArchivedNotes = null
        }
    }

    fun restoreLastPinnedUnArchivedNotes() {
        lastPinnedUnArchivedNotes?.let { notes ->
            updateNotes(notes) { it.copy(isArchived = true, isPinned = false) }
            lastPinnedUnArchivedNotes = null
        }
    }
}*/


