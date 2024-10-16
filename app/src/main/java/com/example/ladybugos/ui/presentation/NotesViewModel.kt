package com.example.ladybugos.ui.presentation


import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.model.generateDummyData
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.theme.Theme
import com.example.ladybugos.widget.NoteWidget
import com.example.ladybugos.widget.WidgetKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class NotesViewModel(
    private val repo: SettingsRepo,
    private val widgetUpdater: WidgetUpdater,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {

    val theme: StateFlow<Theme> = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(_allNotes, _searchQuery) { notes, query ->
        notes.filter { it.matchesSearch(query) && !it.isArchived && !it.isTrashed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())


    val archivedNotes: StateFlow<List<Note>> = combine(_allNotes, _searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes.filter { it.isArchived && !it.isTrashed }
        } else {
            notes.filter { note ->
                !note.isTrashed && note.matchesSearch(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())


    /*    // General search for all notes
        val searchNotes: StateFlow<List<Note>> = _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                useCases.getAllNotes()
            } else {
                useCases.searchNotes(query)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val notes: StateFlow<List<Note>> = combine(_allNotes, _searchQuery) { notes, query ->
            notes.filter { it.matchesSearch(query) && !it.isArchived && !it.isTrashed }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

        val archivedNotes: StateFlow<List<Note>> = _allNotes.map { notes ->
            notes.filter { it.isArchived && !it.isTrashed }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())


        // Search specifically for archived notes
        val searchResults: StateFlow<List<Note>> = searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                archivedNotes // Default to archived notes
            } else {
                _allNotes.map { notes ->
                    notes.filter { note ->
                        note.title.contains(query, ignoreCase = true) ||
                                note.content.contains(query, ignoreCase = true)
                    }.filter { it.isArchived }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())*/


    private var lastDeletedNotes: List<Note>? = null
    private var lastArchivedNotes: List<Note>? = null
    private var lastUnArchivedNotes: List<Note>? = null
    private var lastPinnedUnArchivedNotes: List<Note>? = null

    // Original note state for editing
    var originalNote = MutableStateFlow(Note())
    private var tagIds = emptyList<Long>()

    // TAG
    private val notesWithTags = noteRepository.getAllNotesWithTags().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val tags = noteRepository.getAllTags().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )


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

    // Reminder
    private val _upcomingReminders = MutableStateFlow<List<Note>>(emptyList())
    val upcomingReminders: StateFlow<List<Note>> = _upcomingReminders.asStateFlow()


    init {
        Timber.tag("DEBUG").d("[TAGS]=[${tags.value.size}]")
        initializeViewModel(savedStateHandle)

        viewModelScope.launch(Dispatchers.IO) {
            generateDummyData(noteRepository)
        }

        viewModelScope.launch {
            noteRepository.getUpcomingReminders().collect { reminders ->
                _upcomingReminders.value = reminders
            }
        }

    }

    fun updateNoteReminder(
        noteId: Long,
        reminderDate: Long?
    ) {
        viewModelScope.launch {
            noteRepository.updateNoteReminder(noteId, reminderDate)

            val updatedNote = noteRepository.getNoteById(noteId).first()
            updatedNote?.let {
                if (reminderDate != null) {
                    noteRepository.scheduleReminder(it)
                } else {
                    noteRepository.cancelReminder(noteId)
                }
            }

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

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
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

    private fun initializeViewModel(savedStateHandle: SavedStateHandle) {
        viewModelScope.launch {
            handleDeletedId(savedStateHandle)
            observeNotes()
        }
    }

    private fun handleDeletedId(savedStateHandle: SavedStateHandle) {
        val deletedId = savedStateHandle.toRoute<Screen.NoteList>().deletedId
//        val deletedId = savedStateHandle.get<Long>("id")
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

    /*
       fun restoreDeletedNote() {
        originalNote.value = originalNote.value.copy(isTrashed = false)
        updateNoteAndWidgets(originalNote.value)
    }

    fun restoreArchivedNotes() {
        originalNote.value = originalNote.value.copy(isArchived = false)
        updateNoteAndWidgets(originalNote.value)
    }

    private fun updateNoteAndWidgets(note: Note) {
        viewModelScope.launch {
            useCases.updateNote(note)
            widgetUpdater.updateAllWidgets(_allNotes.value)
        }
    }
     */

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


    fun updateTheme(theme: Theme) {
        viewModelScope.launch { repo.setTheme(theme) }
    }


}

val pastelColors = listOf(
    Color(0xFFFFB3BA), // Light Pink
    Color(0xFFFFDFBA), // Light Peach
    Color(0xFFFFFFBA), // Light Yellow
    Color(0xFFBAFFBA), // Light Green
    Color(0xFFBAE1FF), // Light Blue
    Color(0xFFD0BAFF), // Light Purple
    Color(0xFFFFC6FF), // Light Magenta
    Color(0xFFDCDCDC), // Light Gray
    Color(0xFFFFDAB9), // Light Orange
    Color(0xFFE6E6FA), // Lavender
    Color(0xFFF0FFF0), // Honeydew
    Color(0xFFF5F5DC)  // Beige
)


/*

class NotesViewModel(
    private val useCases: NotesUseCasesInterface,
    savedStateHandle: SavedStateHandle,
    private val repo: SettingsRepo,
    context: Context,
) : ViewModel() {

    // StateFlow to hold theme settings
    val theme: StateFlow<Theme> = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

    // StateFlow to hold notes
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    // Store last deleted notes for potential undo
    private var lastDeletedNotes: List<Note>? = null
    private val widgetUpdater = WidgetUpdater(context)

    // Search query state
    var searchQuery by mutableStateOf("")
        private set

    // Original note state for editing
    var originalNote = MutableStateFlow(Note())


    init {
        initializeViewModel(savedStateHandle)
    }

    private fun initializeViewModel(savedStateHandle: SavedStateHandle) {
        viewModelScope.launch {
            handleDeletedId(savedStateHandle)
//            loadInitialNotes()
            observeNotes()
        }
    }

    private fun handleDeletedId(savedStateHandle: SavedStateHandle) {
        val deletedId = savedStateHandle.toRoute<Screen.NoteList>().deletedId
//        val deletedId = savedStateHandle.get<Long>("id")
        if (deletedId != null) loadNoteById(deletedId)
        Timber.tag("DEBUG").d("NotesViewModel_[originalNote]=[${originalNote.value}]")
        Timber.tag("DEBUG").d("NotesViewModel_[deletedId]=[$deletedId]")
    }

    private fun observeNotes() {
        viewModelScope.launch {
            useCases.getAllNotes().collectLatest { notesList ->
                _notes.value = notesList
                widgetUpdater.updateAllWidgets(notesList)
            }
        }
    }

    private fun loadNoteById(id: Long) {
        viewModelScope.launch {
            useCases.getNoteById(id).firstOrNull()?.let { loadedNote ->
                originalNote.value = loadedNote
            }
        }
    }

    private suspend fun loadInitialNotes() {
        if (useCases.getAllNotes().firstOrNull().isNullOrEmpty()) {
            loadDummyData().forEach { useCases.addNote(it) }
        }
    }

    private fun loadDummyData(): List<Note> {
        return (1..100).map { id ->
            Note(
                id = id.toLong(),
                title = "Sample Note $id",
                content = "This is the content of sample note.",
                updateDate = "2024-10-01",
                lightColor = colorPalette.random().toArgb(),
                isPinned = id % 5 == 0,
                isArchived = id % 7 == 0
            )
        }
    }

    fun updateTheme(theme: Theme) {
        viewModelScope.launch { repo.setTheme(theme) }
    }

    fun pinNotes(notesToPin: List<Note>) {
        updateNoteStatus(notesToPin, isPinned = true)
    }

    fun unpinNotes(notesToUnpin: List<Note>) {
        updateNoteStatus(notesToUnpin, isPinned = false)
    }


    // Using batch updates for pinning/unpinning
    fun updateNoteStatus(
        notesToUpdate: List<Note>,
        isPinned: Boolean? = null,
        isArchived: Boolean? = null
    ) {
        viewModelScope.launch {
            val updatedNotes = notesToUpdate.map { note ->
                note.copy(
                    isPinned = isPinned ?: note.isPinned,
                    isArchived = isArchived ?: note.isArchived
                )
            }
            useCases.updateNotes(updatedNotes) // Batch update
        }
    }


    // New method for searching notes
    fun searchNotes(query: String) {
        viewModelScope.launch {
            useCases.searchNotes(query).collectLatest { filteredNotes ->
                _notes.value = filteredNotes
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun archiveNotes(notesToArchive: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notesToArchive.map { note ->
                if (note.isPinned) {
                    // Unpin the note and move it to archive
                    note.copy(isArchived = true, isPinned = false, isTrashed = false)
                } else {
                    // Move note to archive without changes
                    note.copy(isArchived = true, isTrashed = false)
                }
            }
            useCases.updateNotes(updatedNotes)
            lastDeletedNotes = notesToArchive // Store archive notes for potential undo
            widgetUpdater.updateAllWidgets(_notes.value)

        }
    }

    fun unArchiveNotes(notesToUnArchive: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notesToUnArchive.map { note ->
                note.copy(isArchived = false, isTrashed = false)

            }
            useCases.updateNotes(updatedNotes)
            lastDeletedNotes = notesToUnArchive // Store archive notes for potential undo
            widgetUpdater.updateAllWidgets(_notes.value)

        }
    }


    fun deleteNotes(notesToDelete: List<Note>) {
        viewModelScope.launch {
            val updatedNotes = notesToDelete.map { note ->
                if (note.isPinned) {
                    // Unpin the note and move it to trash
                    note.copy(isTrashed = true, isPinned = false, isArchived = false)
                } else {
                    // Move note to trash without changes
                    note.copy(isTrashed = true, isArchived = false)
                }
            }
            useCases.updateNotes(updatedNotes) // Batch update to optimize performance
            lastDeletedNotes = notesToDelete // Store original notes for potential undo
            widgetUpdater.updateAllWidgets(_notes.value)
        }
    }

    fun restoreLastDeletedNotes() {
        viewModelScope.launch {
            lastDeletedNotes?.let { notes ->
                val restoredNotes = notes.map { note ->
                    // Restore the note to its original trashed and pinned state
                    note.copy(isTrashed = false, isPinned = note.isPinned)
                }
                useCases.updateNotes(restoredNotes) // Batch update for performance
                lastDeletedNotes = null
                widgetUpdater.updateAllWidgets(_notes.value)
            }
        }
    }


    fun restoreLastArchivedNotes() {
        viewModelScope.launch {
            lastDeletedNotes?.let { notes ->
                val restoredNotes = notes.map { note ->
                    note.copy(isArchived = false, isPinned = note.isPinned)
                }
                useCases.updateNotes(restoredNotes)
            }
            lastDeletedNotes = null
            widgetUpdater.updateAllWidgets(_notes.value)
        }
    }


    fun restoreDeletedNote() {
        viewModelScope.launch {
            originalNote.value = originalNote.value.copy(isTrashed = false, isPinned = false)
            useCases.updateNote(originalNote.value)
            widgetUpdater.updateAllWidgets(_notes.value)
        }
    }

    fun restoreArchivedNotes() {
        viewModelScope.launch {
            originalNote.value = originalNote.value.copy(isArchived = false, isPinned = false)
            useCases.updateNote(originalNote.value)
            widgetUpdater.updateAllWidgets(_notes.value)
        }
    }


    fun setNoteReminder(note: Note) {}
}
*/


class WidgetUpdater(private val context: Context) {
    private val glanceAppWidgetManager = GlanceAppWidgetManager(context)

    suspend fun updateAllWidgets(notes: List<Note>) {
        val glanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)
        glanceIds.forEach { updateWidget(it, notes) }
    }

    private suspend fun updateWidget(glanceId: GlanceId, notes: List<Note>) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val noteId = prefs[WidgetKeys.Prefs.noteId]
            val noteToDisplay = notes.find { it.id.toString() == noteId }
            updateWidgetPrefs(prefs, noteToDisplay)
        }
        NoteWidget().update(context, glanceId)
    }

    suspend fun updateSingleWidget(note: Note) {
        val allGlanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)
        allGlanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                if (prefs[WidgetKeys.Prefs.noteId] == note.id.toString()) {
                    updateWidgetPrefs(prefs, note)
                }
            }
            NoteWidget().update(context, glanceId)
        }
    }

    private fun updateWidgetPrefs(prefs: MutablePreferences, note: Note?) {
        if (note != null && !note.isTrashed) {
            prefs[WidgetKeys.Prefs.noteHeader] = note.title
            prefs[WidgetKeys.Prefs.noteBody] = note.content
            prefs[WidgetKeys.Prefs.noteLastUpdate] = note.updateDate
            prefs[WidgetKeys.Prefs.noteColor] = note.lightColor
            prefs[WidgetKeys.Prefs.isDeleted] = false
        } else {
            prefs.remove(WidgetKeys.Prefs.noteHeader)
            prefs.remove(WidgetKeys.Prefs.noteBody)
            prefs.remove(WidgetKeys.Prefs.noteLastUpdate)
            prefs.remove(WidgetKeys.Prefs.noteColor)
            prefs[WidgetKeys.Prefs.isDeleted] = true
        }
    }


}

/*    suspend fun getGlanceIdByNoteId(noteId: Long): GlanceId? {
          val glanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)

          glanceIds.forEach { glanceId ->
              val widgetNoteId = getNoteIdFromPreferences(glanceId)
              if (widgetNoteId == noteId.toString()) {
                  return glanceId
              }
          }
          return null
      }

  private suspend fun getNoteIdFromPreferences(glanceId: GlanceId): String? {
      val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
      return prefs[WidgetKeys.Prefs.noteId]
  }


class WidgetUpdater(private val context: Context) {

    private val glanceAppWidgetManager = GlanceAppWidgetManager(context)

    suspend fun updateWidgets(notes: List<Note>) {
        val glanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateWidget(glanceId, notes)
        }
    }

    suspend fun updateWidget(glanceId: GlanceId, notes: List<Note>) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val noteId = prefs[WidgetKeys.Prefs.noteId]
            val noteToDisplay = notes.find { it.id.toString() == noteId }

            if (noteToDisplay != null && !noteToDisplay.isTrashed) {
                // Update the widget with the selected note's details
                prefs[WidgetKeys.Prefs.noteHeader] = noteToDisplay.title
                prefs[WidgetKeys.Prefs.noteBody] = noteToDisplay.content
                prefs[WidgetKeys.Prefs.noteLastUpdate] = noteToDisplay.updateDate

                // Use the appropriate color based on theme
                prefs[WidgetKeys.Prefs.noteColor] = noteToDisplay.lightColor
                prefs[WidgetKeys.Prefs.isDeleted] = false

            } else {
                // Remove preferences if the note is deleted or no longer exists
                prefs.remove(WidgetKeys.Prefs.noteHeader)
                prefs.remove(WidgetKeys.Prefs.noteBody)
                prefs.remove(WidgetKeys.Prefs.noteLastUpdate)
                prefs.remove(WidgetKeys.Prefs.noteColor)
                prefs[WidgetKeys.Prefs.isDeleted] = true
                // Keep the noteId in case the note is restored later
            }
        }

        // Update the widget UI
        NoteWidget().update(context, glanceId)
    }
}

*/
