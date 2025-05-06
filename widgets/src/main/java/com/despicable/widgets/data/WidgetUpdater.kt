package com.despicable.widgets.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.despicable.core.model.Note
import com.despicable.widgets.mapper.toWidgetNote
import com.despicable.widgets.mapper.toWidgetNotes
import com.despicable.widgets.model.WidgetChecklistItem
import com.despicable.widgets.model.WidgetContent
import com.despicable.widgets.model.WidgetKeys
import com.despicable.widgets.model.WidgetNote
import com.despicable.widgets.ui.NoteWidget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class WidgetUpdater(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    private val glanceAppWidgetManager = GlanceAppWidgetManager(context)
    private val stateDefinition = PreferencesGlanceStateDefinition


    // Cache to store widget-note associations
    private val widgetNoteCache = mutableMapOf<GlanceId, Long>()

    init {
        // Initialize cache on creation
        CoroutineScope(dispatchers.io).launch {
            refreshWidgetNoteCache()
        }
    }

    //  WidgetUpdater class
    suspend fun updateChecklistItemOnly(
        glanceId: GlanceId,
        itemId: Long,
        isChecked: Boolean
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentItemsJson =
                prefs[WidgetKeys.Prefs.checklistItems] ?: return@updateAppWidgetState

            try {
                val currentItems =
                    Json.decodeFromString<List<WidgetChecklistItem>>(currentItemsJson)
                val updatedItems = currentItems.map { item ->
                    if (item.id == itemId) item.copy(isChecked = isChecked) else item
                }
                prefs[WidgetKeys.Prefs.checklistItems] = Json.encodeToString(updatedItems)
            } catch (e: Exception) {
                Log.e("WIDGET", "Failed to update checklist item: $e")
            }
        }

        // Only update widget after preference is updated
        NoteWidget().update(context, glanceId)
    }

    private suspend fun refreshWidgetNoteCache() {
        getGlanceIds().forEach { glanceId ->
            val noteId = getWidgetNoteId(glanceId)
            if (noteId != null) {
                widgetNoteCache[glanceId] = noteId
            }
        }
    }

    // Update only specific widgets for given notes
    suspend fun updateWidgetsForNotes(
        notes: List<Note>
    ) = withContext(dispatchers.io) {
        try {

            // Get relevant widget IDs for these notes only
            val noteIds = notes.map { it.id }.toSet()
            val relevantWidgets = widgetNoteCache.filter { it.value in noteIds }

            coroutineScope {
                relevantWidgets.forEach { (glanceId, noteId) ->
                    launch {
                        val note = notes.find { it.id == noteId }
                        updateWidgetState(glanceId, note?.toWidgetNote())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(
                "WIDGET",
                "Failed to update widgets for notes: ${notes.map { it.id }}",
                e
            )
        }
    }

    /*
      Handles undo operation for multiple deleted widgets while preserving
      original widget-note associations
     */
    suspend fun undoDeleteWidgets(
        notes: List<Note>
    ) {
        Log.e("WIDGET", "undoDeleteWidgets[${notes.map { it.id }}]")
        restoreMultipleWidgets(notes.toWidgetNotes())
    }

    /*
      Handles undo operation for a single deleted widget
     */
    suspend fun undoDeleteWidget(widgetNote: WidgetNote) {
        Log.e("WIDGET", "undoDeleteWidget[$widgetNote]")
        restoreWidget(widgetNote)
    }

    /*
      Data class to hold widget state information
     */
    private data class WidgetState(
        val glanceId: GlanceId,
        val noteId: Long?,
        val isDeleted: Boolean
    )


    /*
      Efficiently restores multiple widgets while preserving original associations
     */
    private suspend fun restoreMultipleWidgets(
        widgetNotes: List<WidgetNote>
    ) = withContext(dispatchers.io) {
        try {
            Log.e(
                "WIDGET",
                "restoreMultipleWidgets: Restoring widgets for notes ${widgetNotes.map { it.id }}"
            )

            // Step 1: Get all widget states with their associations
            val widgetStates = getGlanceIds().map { glanceId ->
                val prefs = getAppWidgetState(context, stateDefinition, glanceId)
                WidgetState(
                    glanceId = glanceId,
                    noteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull(),
                    isDeleted = prefs[WidgetKeys.Prefs.isDeleted] ?: false
                )
            }

            // Step 2: Create a map of deleted widgets with their original note IDs
            val deletedWidgetMap = widgetStates.filter { it.isDeleted }.associateBy { it.noteId }

            Log.e(
                "WIDGET",
                "restoreMultipleWidgets: Found ${deletedWidgetMap.size} deleted widgets"
            )

            // Step 3: Process each note and restore it to its original widget if possible
            coroutineScope {
                widgetNotes.forEach { widgetNote ->
                    // Find the widget that was originally associated with this note
                    val originalWidget = deletedWidgetMap[widgetNote.id]
                    if (originalWidget != null) {
                        launch {
                            Log.e(
                                "WIDGET",
                                "Restoring note ${widgetNote.id} to its original widget ${originalWidget.glanceId}"
                            )
                            updateWidgetState(
                                originalWidget.glanceId,
                                widgetNote
                            )
                        }
                    } else {
                        Log.e("WIDGET", "No original widget found for note ${widgetNote.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WIDGET", "Failed to restore multiple widgets: ${e.message}")
        }
    }

    /*
      Direct approach to restore single widget state
     */
    private suspend fun restoreWidget(widgetNote: WidgetNote) =
        withContext(dispatchers.io) {
            try {
                Log.e("WIDGET", "restoreWidget: Restoring widget for note ${widgetNote.id}")
                val glanceIds = getGlanceIds()

                // Find the widget that was originally associated with this note
                glanceIds.forEach { glanceId ->
                    val prefs = getAppWidgetState(context, stateDefinition, glanceId)
                    val isDeleted =
                        prefs[WidgetKeys.Prefs.isDeleted] ?: false
                    val widgetNoteId =
                        prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()

                    if (isDeleted && widgetNoteId == widgetNote.id) {
                        updateWidgetState(glanceId, widgetNote)
                        return@forEach
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "WIDGET",
                    "Failed to restore widget for note ${widgetNote.id}: ${e.message}"
                )
            }
        }

    /*
      Sealed interface to represent different update operations
     */
    sealed interface UpdateOperation {
        data class Single(val widgetNote: WidgetNote) : UpdateOperation
        data class Multiple(val widgetNote: List<WidgetNote>) : UpdateOperation
        data class Configuration(val glanceId: GlanceId, val widgetNote: WidgetNote) :
            UpdateOperation
    }

    /*
      Single entry point for all widget updates
     */
    private suspend fun executeUpdate(operation: UpdateOperation) = withContext(dispatchers.io) {
        try {
            Log.e("WIDGET", "executeUpdate: Starting operation: $operation")
            when (operation) {
                is UpdateOperation.Single -> handleSingleUpdate(operation.widgetNote)

                is UpdateOperation.Multiple -> handleMultipleUpdate(operation.widgetNote)

                is UpdateOperation.Configuration -> handleConfigUpdate(
                    operation.glanceId, operation.widgetNote
                )
            }
        } catch (e: Exception) {
            val errorMessage = when (operation) {
                is UpdateOperation.Single -> "Failed to update widget for note ${operation.widgetNote.id}"
                is UpdateOperation.Multiple -> "Failed to update all widgets"
                is UpdateOperation.Configuration -> "Failed to update widget from config for note ${operation.widgetNote.id}"
            }
            Log.e("WIDGET", "$errorMessage: ${e.message}")
        }
    }

    // Public API methods
    /*    suspend fun updateWidgetFromConfig(glanceId: GlanceId, note: Note) =
            executeUpdate(UpdateOperation.Configuration(glanceId, note))
            */

    /*    suspend fun updateWidgetFromConfig(
            glanceId: GlanceId,
            note: Note,
            checklistItems: List<Checklist> = emptyList()
        ) {
            // Update basic note info
            updateAppWidgetState(context, glanceId) { prefs ->
                setNotePreferences(prefs, note)

                // Also update checklist items if this is a checklist note
                if (note.noteType == NoteType.CHECKLIST && checklistItems.isNotEmpty()) {
                    val widgetItems = checklistItems.map { item ->
                        WidgetChecklistItem(
                            id = item.id,
                            content = item.content,
                            isChecked = item.isChecked,
                            position = item.position
                        )
                    }
                    prefs[WidgetKeys.Prefs.checklistItems] = Json.encodeToString(widgetItems)
                }
            }

            NoteWidget().update(context, glanceId)
        }*/

    // Widget Updater class
    suspend fun updateWidgetFromConfig(
        glanceId: GlanceId,
        widgetNote: WidgetNote
    ) {

        Log.e("WIDGET", "updateWidgetFromConfig: [${widgetNote.content}]")
        Log.e("WIDGET", "updateWidgetFromConfig: [${widgetNote.id}]")

        updateAppWidgetState(context, glanceId) { prefs ->
            setNotePreferences(prefs, widgetNote)
        }

        NoteWidget().update(context, glanceId)
    }

    suspend fun updateAllWidgets(
        widgetNotes: List<WidgetNote>,
    ) = executeUpdate(UpdateOperation.Multiple(widgetNotes))

    /*
      Regular update operation for non-undo scenarios
     */
    suspend fun updateSingleWidget(note: Note) =
        executeUpdate(UpdateOperation.Single(note.toWidgetNote()))


    // Private handler methods
    private suspend fun handleSingleUpdate(
        widgetNote: WidgetNote
    ) {
        Log.e("WIDGET", "handleSingleUpdate: Processing note ${widgetNote.id}")
        val glanceIds = getGlanceIds()

        glanceIds.forEach { glanceId ->
            if (isWidgetAssociatedWithNote(glanceId, widgetNote.id)) {
                updateWidgetState(glanceId, widgetNote)
            }
        }
    }

    private suspend fun handleMultipleUpdate(noteCompletes: List<WidgetNote>) {
        // Build a map for quick lookup
        val noteCompleteMap = noteCompletes.associateBy { it.id }

        getGlanceIds().forEach { glanceId ->
            val noteId = getWidgetNoteId(glanceId)
            val associatedNoteComplete = noteCompleteMap[noteId]
            updateWidgetState(glanceId, associatedNoteComplete)
        }
    }

    private suspend fun handleConfigUpdate(
        glanceId: GlanceId,
        widgetNote: WidgetNote

    ) {
        updateWidgetState(glanceId, widgetNote)
    }

    // Helper methods
    private suspend fun getGlanceIds(): List<GlanceId> =
        glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)


    private suspend fun isWidgetAssociatedWithNote(glanceId: GlanceId, noteId: Long): Boolean =
        getWidgetNoteId(glanceId) == noteId

    private suspend fun getWidgetNoteId(glanceId: GlanceId): Long? = getAppWidgetState(
        context, stateDefinition, glanceId
    )[WidgetKeys.Prefs.noteId]?.toLongOrNull()


    private suspend fun updateWidgetState(
        glanceId: GlanceId,
        widgetNote: WidgetNote?
    ) {
        Log.e("WIDGET", "updateWidgetState: Starting update for note: ${widgetNote?.id}")
        updateAppWidgetState(context, glanceId) { prefs ->
            if (widgetNote != null) {
                setNotePreferences(prefs, widgetNote)
            } else {
                clearNotePreferences(prefs)
            }
        }
        NoteWidget().update(context, glanceId)
    }


    private val emptyChecklistJsonString = "[]"

    private fun setNotePreferences(
        prefs: MutablePreferences,
        widgetNote: WidgetNote
    ) = prefs.run {
        val content = widgetNote.content

        // Set basic note metadata
        set(WidgetKeys.Prefs.noteId, widgetNote.id.toString())
        set(WidgetKeys.Prefs.noteHeader, widgetNote.title)
        set(WidgetKeys.Prefs.noteReminderDate, widgetNote.reminderDate.toString())
        set(WidgetKeys.Prefs.noteColor, widgetNote.color)
        set(WidgetKeys.Prefs.isDeleted, false)

        // Handle content-specific preferences
        when (content) {
            is WidgetContent.Text -> {
                set(WidgetKeys.Prefs.noteBody, content.text)
                set(WidgetKeys.Prefs.checklistItems, emptyChecklistJsonString)
                set(WidgetKeys.Prefs.isChecklist, false)
            }

            is WidgetContent.Checklist -> {
                set(WidgetKeys.Prefs.noteBody, "")
                val itemsJson = if (content.items.isNotEmpty()) {
                    Json.encodeToString(content.items)
                } else {
                    emptyChecklistJsonString
                }
                set(WidgetKeys.Prefs.checklistItems, itemsJson)
                set(WidgetKeys.Prefs.isChecklist, true)
            }
        }

        Log.e(
            "WIDGET",
            "setNotePreferences: Set isChecklist=${content is WidgetContent.Checklist} for note ${widgetNote.id}"
        )
    }

    private val widgetPreferenceKeysToClear = listOf(
        WidgetKeys.Prefs.noteHeader,
        WidgetKeys.Prefs.noteBody,
        WidgetKeys.Prefs.noteReminderDate,
        WidgetKeys.Prefs.noteColor,
        WidgetKeys.Prefs.checklistItems
    )

    private fun clearNotePreferences(prefs: MutablePreferences) = prefs.run {
        widgetPreferenceKeysToClear.forEach { remove(it) }
        // Don't remove noteId as we need it to maintain the association
        set(WidgetKeys.Prefs.isDeleted, true)
        set(WidgetKeys.Prefs.isChecklist, false)
    }


    /*
        private fun setNotePreferences(
        prefs: MutablePreferences,
        note: WidgetNote
    ) {
        with(prefs) {
            set(WidgetKeys.Prefs.noteId, note.id.toString())
            set(WidgetKeys.Prefs.noteHeader, note.title)
            set(WidgetKeys.Prefs.noteReminderDate, note.reminderDate.toString())
            set(WidgetKeys.Prefs.noteColor, note.color)
            set(WidgetKeys.Prefs.isDeleted, false)


            // Handle note body and checklist items based on content type
            when (note.content) {
                is WidgetContent.Text -> {
                    set(WidgetKeys.Prefs.noteBody, note.content.text)
                    // Clear any existing checklist items when switching to text

                    // Set checklist flag
                    set(WidgetKeys.Prefs.isChecklist, false)
                    set(
                        WidgetKeys.Prefs.checklistItems,
                        Json.encodeToString<List<WidgetChecklistItem>>(emptyList())
                    )
                }

                is WidgetContent.Checklist -> {
                    // Empty string for text content since we're using checklist items
                    set(WidgetKeys.Prefs.noteBody, "")
                    // Set checklist flag
                    set(WidgetKeys.Prefs.isChecklist, true)

                    // Only update checklist items if we have items to update
                    val widgetItems = if (note.content.items.isNotEmpty()) {
                        note.content.items
                    } else {
                        emptyList()
                    }
                    set(WidgetKeys.Prefs.checklistItems, Json.encodeToString(widgetItems))
                }
            }
            Log.e(
                "WIDGET",
                "setNotePreferences: Set isChecklist=${note.content is WidgetContent.Checklist} for note ${note.id}"
            )

        }
    }

    private fun clearNotePreferences(prefs: MutablePreferences) {
        with(prefs) {
            // Don't remove noteId as we need it to maintain the association
            listOf(
                WidgetKeys.Prefs.noteHeader,
                WidgetKeys.Prefs.noteBody,
                WidgetKeys.Prefs.noteReminderDate,
                WidgetKeys.Prefs.noteColor,
                WidgetKeys.Prefs.checklistItems
            ).forEach { remove(it) }
            set(WidgetKeys.Prefs.isDeleted, true)
            set(WidgetKeys.Prefs.isChecklist, false)
        }
    }



       private fun setNotePreferences(
            prefs: MutablePreferences,
            noteComplete: NoteComplete
        ) {
            val note = noteComplete.note
            val checklistItems = noteComplete.checklistItems


            with(prefs) {
                set(WidgetKeys.Prefs.noteId, note.id.toString())
                set(WidgetKeys.Prefs.noteHeader, note.title)
                set(WidgetKeys.Prefs.noteReminderDate, note.reminderDate.toString())
                set(WidgetKeys.Prefs.noteColor, note.lightColor)
                set(WidgetKeys.Prefs.isDeleted, false)
                // Set checklist flag
                val isChecklist = note.noteType == NoteType.CHECKLIST
                set(WidgetKeys.Prefs.isChecklist, isChecklist)

                // Handle note body and checklist items based on content type
                when (note.content) {
                    is NoteContent.Text -> {
                        set(WidgetKeys.Prefs.noteBody, (note.content as NoteContent.Text).text)
                        // Clear any existing checklist items when switching to text
                        set(
                            WidgetKeys.Prefs.checklistItems,
                            Json.encodeToString<List<WidgetChecklistItem>>(emptyList())
                        )
                    }

                    is NoteContent.ChecklistItems -> {
                        // Empty string for text content since we're using checklist items
                        set(WidgetKeys.Prefs.noteBody, "")

                        // Only update checklist items if we have items to update
                        val widgetItems = if (isChecklist && checklistItems.isNotEmpty()) {
                            checklistItems.map { it.toWidgetChecklistItem() }
                        } else {
                            emptyList()
                        }
                        set(WidgetKeys.Prefs.checklistItems, Json.encodeToString(widgetItems))
                    }
                }
                Log.e(
                    "WIDGET",
                    "setNotePreferences: Set isChecklist=${note.noteType == NoteType.CHECKLIST} for note ${note.id}"
                )

            }
        }

        private fun clearNotePreferences(prefs: MutablePreferences) {
            with(prefs) {
                // Don't remove noteId as we need it to maintain the association
                listOf(
                    WidgetKeys.Prefs.noteHeader,
                    WidgetKeys.Prefs.noteBody,
                    WidgetKeys.Prefs.noteReminderDate,
                    WidgetKeys.Prefs.noteColor,
                    WidgetKeys.Prefs.checklistItems
                ).forEach { remove(it) }
                set(WidgetKeys.Prefs.isDeleted, true)
                set(WidgetKeys.Prefs.isChecklist, false)
            }
        }
        */
}


interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val main = Dispatchers.Main
    override val io = Dispatchers.IO
    override val default = Dispatchers.Default
}

/*

class WidgetUpdater(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) : KoinComponent {

    // Get noteDao from Koin
    private val repo: NoteRepository by inject()

    private val glanceAppWidgetManager = GlanceAppWidgetManager(context)
    private val stateDefinition = PreferencesGlanceStateDefinition


    // Cache to store widget-note associations
    private val widgetNoteCache = mutableMapOf<GlanceId, Long>()

    // Maintain a map of checklist observers for each widget
    private val checklistObservers = mutableMapOf<GlanceId, Job>()

    init {
        // Initialize cache on creation
        CoroutineScope(dispatchers.io).launch {
            refreshWidgetNoteCache()
        }
    }

    private suspend fun refreshWidgetNoteCache() {
        getGlanceIds().forEach { glanceId ->
            val noteId = getWidgetNoteId(glanceId)
            if (noteId != null) {
                widgetNoteCache[glanceId] = noteId
            }
        }
    }

    // Update only specific widgets for given notes
    suspend fun updateWidgetsForNotes(notes: List<Note>) = withContext(dispatchers.io) {
        try {
            // Get relevant widget IDs for these notes only
            val noteIds = notes.map { it.id }.toSet()
            val relevantWidgets = widgetNoteCache.filter { it.value in noteIds }

            coroutineScope {
                relevantWidgets.forEach { (glanceId, noteId) ->
                    launch {
                        val note = notes.find { it.id == noteId }
                        updateWidgetState(glanceId, note)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update widgets for notes: ${notes.map { it.id }}")
        }
    }

    /*
      Handles undo operation for multiple deleted widgets while preserving
      original widget-note associations
     */
    suspend fun undoDeleteWidgets(notes: List<Note>) {
        Timber.tag("DEBUG").d("undoDeleteWidgets[${notes.map { it.id }}]")
        restoreMultipleWidgets(notes)
    }

    /*
      Handles undo operation for a single deleted widget
     */
    suspend fun undoDeleteWidget(note: Note) {
        Timber.tag("DEBUG").d("undoDeleteWidget[$note]")
        restoreWidget(note)
    }

    /*
      Data class to hold widget state information
     */
    private data class WidgetState(
        val glanceId: GlanceId,
        val noteId: Long?,
        val isDeleted: Boolean
    )

    /*
      Efficiently restores multiple widgets while preserving original associations
     */
    private suspend fun restoreMultipleWidgets(notes: List<Note>) = withContext(dispatchers.io) {
        try {
            Timber.tag("DEBUG")
                .d("restoreMultipleWidgets: Restoring widgets for notes ${notes.map { it.id }}")

            // Step 1: Get all widget states with their associations
            val widgetStates = getGlanceIds().map { glanceId ->
                val prefs = getAppWidgetState(context, stateDefinition, glanceId)
                WidgetState(
                    glanceId = glanceId,
                    noteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull(),
                    isDeleted = prefs[WidgetKeys.Prefs.isDeleted] ?: false
                )
            }

            // Step 2: Create a map of deleted widgets with their original note IDs
            val deletedWidgetMap = widgetStates.filter { it.isDeleted }.associateBy { it.noteId }

            Timber.tag("DEBUG")
                .d("restoreMultipleWidgets: Found ${deletedWidgetMap.size} deleted widgets")

            // Step 3: Process each note and restore it to its original widget if possible
            coroutineScope {
                notes.forEach { note ->
                    // Find the widget that was originally associated with this note
                    val originalWidget = deletedWidgetMap[note.id]
                    if (originalWidget != null) {
                        launch {
                            Timber.tag("DEBUG")
                                .d("Restoring note ${note.id} to its original widget ${originalWidget.glanceId}")
                            updateWidgetState(originalWidget.glanceId, note)
                        }
                    } else {
                        Timber.tag("DEBUG").d("No original widget found for note ${note.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore multiple widgets: ${e.message}")
        }
    }

    /*
      Direct approach to restore single widget state
     */
    private suspend fun restoreWidget(note: Note) = withContext(dispatchers.io) {
        try {
            Timber.tag("DEBUG").d("restoreWidget: Restoring widget for note ${note.id}")
            val glanceIds = getGlanceIds()

            // Find the widget that was originally associated with this note
            glanceIds.forEach { glanceId ->
                val prefs = getAppWidgetState(context, stateDefinition, glanceId)
                val isDeleted =
                    prefs[WidgetKeys.Prefs.isDeleted] ?: false
                val widgetNoteId =
                    prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()

                if (isDeleted && widgetNoteId == note.id) {
                    updateWidgetState(glanceId, note)
                    return@forEach
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore widget for note ${note.id}: ${e.message}")
        }
    }

    /*
      Sealed interface to represent different update operations
     */
    private sealed interface UpdateOperation {
        data class Single(val note: Note) : UpdateOperation
        data class Multiple(val notes: List<Note>) : UpdateOperation
        data class Configuration(val glanceId: GlanceId, val note: Note) : UpdateOperation
        data class ChecklistToggle(
            val glanceId: GlanceId,
            val noteId: Long,
            val checklistItem: Checklist
        ) : UpdateOperation
    }


    // Start observing checklist changes for a widget
    private fun startChecklistObserver(glanceId: GlanceId, noteId: Long) {
        checklistObservers[glanceId]?.cancel() // Cancel any existing observer

        checklistObservers[glanceId] = CoroutineScope(dispatchers.io).launch {
            repo.getChecklistItemsByNoteId(noteId)
                .collect { checklistItems ->
                    updateWidgetStateWithChecklist(glanceId, noteId, checklistItems)
                }
        }
    }

    // Stop observing checklist changes for a widget
    private fun stopChecklistObserver(glanceId: GlanceId) {
        checklistObservers[glanceId]?.cancel()
        checklistObservers.remove(glanceId)
    }

    private suspend fun updateWidgetStateWithChecklist(
        glanceId: GlanceId,
        noteId: Long,
        checklistItems: List<Checklist>
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            // Encode checklist items to JSON
            val widgetChecklist = checklistItems
                .sortedBy { it.position }
                .map { item ->
                    WidgetChecklistItem(
                        id = item.id,
                        content = item.content,
                        isChecked = item.isChecked,
                        position = item.position
                    )
                }
            prefs[WidgetKeys.Prefs.checklistItems] = Json.encodeToString(widgetChecklist)
        }
        NoteWidget().update(context, glanceId)
    }

    /*
      Single entry point for all widget updates
     */
    private suspend fun executeUpdate(operation: UpdateOperation) = withContext(dispatchers.io) {
        try {
            Timber.tag("DEBUG").d("executeUpdate: Starting operation: $operation")
            when (operation) {
                is UpdateOperation.Single -> handleSingleUpdate(operation.note)
                is UpdateOperation.Multiple -> handleMultipleUpdate(operation.notes)
                is UpdateOperation.Configuration -> handleConfigUpdate(
                    operation.glanceId, operation.note
                )

                is UpdateOperation.ChecklistToggle -> handleChecklistToggle(
                    operation.glanceId,
                    operation.noteId,
                    operation.checklistItem
                )
            }
        } catch (e: Exception) {
            val errorMessage = when (operation) {
                is UpdateOperation.Single -> "Failed to update widget for note ${operation.note.id}"
                is UpdateOperation.Multiple -> "Failed to update all widgets"
                is UpdateOperation.Configuration -> "Failed to update widget from config for note ${operation.note.id}"
                is UpdateOperation.ChecklistToggle -> "Failed to toggle checklist item"
            }
            Timber.e(e, "$errorMessage: ${e.message}")
        }
    }


    // Modified to handle checklist toggle
    suspend fun toggleChecklistItem(glanceId: GlanceId, checklistItem: Checklist) {
        val updatedItem = checklistItem.copy(isChecked = !checklistItem.isChecked)
        executeUpdate(UpdateOperation.ChecklistToggle(glanceId, checklistItem.noteId, updatedItem))
    }

    private suspend fun handleChecklistToggle(
        glanceId: GlanceId,
        noteId: Long,
        checklistItem: Checklist
    ) {
        try {
            repo.updateChecklistItem(checklistItem)
            // The Flow collector will automatically update the widget
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle checklist item: ${checklistItem.id}")
        }
    }

    // Public API methods
    suspend fun updateWidgetFromConfig(glanceId: GlanceId, note: Note) =
        executeUpdate(UpdateOperation.Configuration(glanceId, note))

    suspend fun updateAllWidgets(notes: List<Note>) = executeUpdate(UpdateOperation.Multiple(notes))

    /*
      Regular update operation for non-undo scenarios
     */
    suspend fun updateSingleWidget(note: Note) = executeUpdate(UpdateOperation.Single(note))


    // Private handler methods
    private suspend fun handleSingleUpdate(note: Note) {
        Timber.tag("DEBUG").d("handleSingleUpdate: Processing note ${note.id}")
        val glanceIds = getGlanceIds()

        glanceIds.forEach { glanceId ->
            if (isWidgetAssociatedWithNote(glanceId, note.id)) {
                updateWidgetState(glanceId, note)
            }
        }
    }

    private suspend fun handleMultipleUpdate(notes: List<Note>) {
        getGlanceIds().forEach { glanceId ->
            val noteId = getWidgetNoteId(glanceId)
            val associatedNote = notes.find { it.id == noteId }
            updateWidgetState(glanceId, associatedNote)
        }
    }

    private suspend fun handleConfigUpdate(glanceId: GlanceId, note: Note) {
        updateWidgetState(glanceId, note)
    }

    // Helper methods
    private suspend fun getGlanceIds(): List<GlanceId> =
        glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)


    private suspend fun isWidgetAssociatedWithNote(glanceId: GlanceId, noteId: Long): Boolean =
        getWidgetNoteId(glanceId) == noteId

    private suspend fun getWidgetNoteId(glanceId: GlanceId): Long? = getAppWidgetState(
        context, stateDefinition, glanceId
    )[WidgetKeys.Prefs.noteId]?.toLongOrNull()


    private suspend fun updateWidgetState(glanceId: GlanceId, note: Note?) {
        Timber.tag("DEBUG")
            .d("updateWidgetState: Starting update for note: ${note?.id}, isChecklist: ${note?.isChecklist}")

        if (note != null && !note.isTrashed && !note.isArchived) {
            if (note.isChecklist) {
                startChecklistObserver(glanceId, note.id)
            } else {
                stopChecklistObserver(glanceId)
            }

            updateAppWidgetState(context, glanceId) { prefs ->
                setNotePreferences(prefs, note)
            }
        } else {
            stopChecklistObserver(glanceId)
            updateAppWidgetState(context, glanceId) { prefs ->
                clearNotePreferences(prefs)
            }
        }

        NoteWidget().update(context, glanceId)
    }


    private fun setNotePreferences(prefs: MutablePreferences, note: Note) {
        with(prefs) {
            set(WidgetKeys.Prefs.noteId, note.id.toString())
            set(WidgetKeys.Prefs.noteHeader, note.title)
            set(WidgetKeys.Prefs.noteBody, note.content)
            set(WidgetKeys.Prefs.noteLastUpdate, note.updateDate)
            set(WidgetKeys.Prefs.noteColor, note.lightColor)
            set(WidgetKeys.Prefs.isDeleted, false)

            set(WidgetKeys.Prefs.isChecklist, note.isChecklist)

            if (!note.isChecklist) {
                set(WidgetKeys.Prefs.noteBody, note.content)
            }

        }
    }


    // Clean up method to call when widgets are removed
    fun cleanUp() {
        checklistObservers.values.forEach { it.cancel() }
        checklistObservers.clear()
    }

    private fun clearNotePreferences(prefs: MutablePreferences) {
        with(prefs) {
            // Don't remove noteId as we need it to maintain the association
            listOf(
                WidgetKeys.Prefs.noteHeader,
                WidgetKeys.Prefs.noteBody,
                WidgetKeys.Prefs.noteLastUpdate,
                WidgetKeys.Prefs.noteColor,
                WidgetKeys.Prefs.checklistItems,
                WidgetKeys.Prefs.isChecklist,
            ).forEach { remove(it) }
            set(WidgetKeys.Prefs.isDeleted, true)
        }
    }
}


interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val main = Dispatchers.Main
    override val io = Dispatchers.IO
    override val default = Dispatchers.Default
}

// Data class for widget-specific checklist item representation
@Serializable
data class WidgetChecklistItem(
    val id: Long,
    val content: String,
    val isChecked: Boolean,
    val position: Int
)

 */