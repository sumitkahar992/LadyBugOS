package com.despicable.widgets.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.despicable.model.Note
import com.despicable.widgets.model.WidgetKeys
import com.despicable.widgets.ui.NoteWidget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


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
        val glanceId: GlanceId, val noteId: Long?, val isDeleted: Boolean
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
            }
        } catch (e: Exception) {
            val errorMessage = when (operation) {
                is UpdateOperation.Single -> "Failed to update widget for note ${operation.note.id}"
                is UpdateOperation.Multiple -> "Failed to update all widgets"
                is UpdateOperation.Configuration -> "Failed to update widget from config for note ${operation.note.id}"
            }
            Timber.e(e, "$errorMessage: ${e.message}")
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
            .d("updateWidgetState: Starting update for note: ${note?.id}, isTrashed: ${note?.isTrashed}, isArchived: ${note?.isArchived}")
        updateAppWidgetState(context, glanceId) { prefs ->
            if (note != null && !note.isTrashed && !note.isArchived) {
                setNotePreferences(prefs, note)
            } else {
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
        }
    }

    private fun clearNotePreferences(prefs: MutablePreferences) {
        with(prefs) {
            // Don't remove noteId as we need it to maintain the association
            listOf(
                WidgetKeys.Prefs.noteHeader,
                WidgetKeys.Prefs.noteBody,
                WidgetKeys.Prefs.noteLastUpdate,
                WidgetKeys.Prefs.noteColor
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

