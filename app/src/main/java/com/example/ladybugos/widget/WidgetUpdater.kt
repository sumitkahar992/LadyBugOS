package com.example.ladybugos.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.ladybugos.model.Note
import com.example.ladybugos.widget.model.WidgetKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber


class WidgetUpdater(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    private val glanceAppWidgetManager = GlanceAppWidgetManager(context)
    private val stateDefinition = PreferencesGlanceStateDefinition

    /**
     * Sealed interface to represent different update operations
     */
    private sealed interface UpdateOperation {
        data class Single(val note: Note) : UpdateOperation
        data class Multiple(val notes: List<Note>) : UpdateOperation
        data class Configuration(val glanceId: GlanceId, val note: Note) : UpdateOperation
    }

    /**
     * Single entry point for all widget updates
     */
    private suspend fun executeUpdate(operation: UpdateOperation) = withContext(dispatchers.io) {
        try {
            when (operation) {
                is UpdateOperation.Single -> handleSingleUpdate(operation.note)
                is UpdateOperation.Multiple -> handleMultipleUpdate(operation.notes)
                is UpdateOperation.Configuration -> handleConfigUpdate(
                    operation.glanceId,
                    operation.note
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

    suspend fun updateAllWidgets(notes: List<Note>) =
        executeUpdate(UpdateOperation.Multiple(notes))

    suspend fun updateSingleWidget(note: Note) =
        executeUpdate(UpdateOperation.Single(note))


    // Private handler methods
    private suspend fun handleSingleUpdate(note: Note) {
        getGlanceIds().forEach { glanceId ->
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

    private suspend fun getWidgetNoteId(glanceId: GlanceId): Long? =
        getAppWidgetState(
            context,
            stateDefinition,
            glanceId
        )[WidgetKeys.Prefs.noteId]?.toLongOrNull()

    private suspend fun updateWidgetState(glanceId: GlanceId, note: Note?) {
        updateAppWidgetState(context, glanceId) { prefs ->
            if (note != null && !note.isTrashed) {
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
            listOf(
                WidgetKeys.Prefs.noteId,
                WidgetKeys.Prefs.noteHeader,
                WidgetKeys.Prefs.noteBody,
                WidgetKeys.Prefs.noteLastUpdate,
                WidgetKeys.Prefs.noteColor
            ).forEach { remove(it) }
            set(WidgetKeys.Prefs.isDeleted, true)
        }
    }
}

// Coroutine dispatchers interface remains the same
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
) {
    private val glanceAppWidgetManager = GlanceAppWidgetManager(context)
    private val stateDefinition = PreferencesGlanceStateDefinition

    // Add this new method specifically for widget configuration
    suspend fun updateWidgetFromConfig(glanceId: GlanceId, note: Note) =
        withContext(dispatchers.io) {
            try {
                updateWidgetState(glanceId, note)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update widget from config for note ${note.id}: ${e.message}")
            }
        }

    // Main update functions
    suspend fun updateAllWidgets(notes: List<Note>) = withContext(dispatchers.io) {
        try {
            val glanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateWidgetWithNotes(glanceId, notes)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update all widgets: ${e.message}")
        }
    }

    suspend fun updateSingleWidget(note: Note) = withContext(dispatchers.io) {
        try {
            val glanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateWidgetWithNote(glanceId, note)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update widget for note ${note.id}: ${e.message}")
        }
    }

    // Update from EditScreen with immediate reflection
    suspend fun updateWidgetsFromEdit(note: Note) = withContext(dispatchers.io) {
        try {
            val glanceIds = glanceAppWidgetManager.getGlanceIds(NoteWidget::class.java)
            glanceIds.forEach { glanceId ->
                val prefs = getAppWidgetState(context, stateDefinition, glanceId)
                val widgetNoteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()

                if (widgetNoteId == note.id) {
                    updateWidgetState(glanceId, note)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update widgets from edit for note ${note.id}: ${e.message}")
        }
    }

    // Private helper functions
    private suspend fun updateWidgetWithNotes(glanceId: GlanceId, notes: List<Note>) {
        val prefs = getAppWidgetState(context, stateDefinition, glanceId)
        val noteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()
        val note = notes.find { it.id == noteId }

        updateWidgetState(glanceId, note)
    }

    private suspend fun updateWidgetWithNote(glanceId: GlanceId, note: Note) {
        val prefs = getAppWidgetState(context, stateDefinition, glanceId)
        val widgetNoteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()

        if (widgetNoteId == note.id) {
            updateWidgetState(glanceId, note)
        }
    }

    private suspend fun updateWidgetState(glanceId: GlanceId, note: Note?) {
        updateAppWidgetState(context, glanceId) { prefs ->
            if (note != null && !note.isTrashed) {
                setNotePreferences(prefs, note)
            } else {
                clearNotePreferences(prefs)
            }
        }
        NoteWidget().update(context, glanceId)
//        NoteWidget().updateAll(context)
    }

    private fun setNotePreferences(prefs: MutablePreferences, note: Note) {
        prefs.apply {
            set(WidgetKeys.Prefs.noteId, note.id.toString())
            set(WidgetKeys.Prefs.noteHeader, note.title)
            set(WidgetKeys.Prefs.noteBody, note.content)
            set(WidgetKeys.Prefs.noteLastUpdate, note.updateDate)
            set(WidgetKeys.Prefs.noteColor, note.lightColor)
            set(WidgetKeys.Prefs.isDeleted, false)
        }
    }

    private fun clearNotePreferences(prefs: MutablePreferences) {
        prefs.apply {
            remove(WidgetKeys.Prefs.noteId)
            remove(WidgetKeys.Prefs.noteHeader)
            remove(WidgetKeys.Prefs.noteBody)
            remove(WidgetKeys.Prefs.noteLastUpdate)
            remove(WidgetKeys.Prefs.noteColor)
            set(WidgetKeys.Prefs.isDeleted, true)
        }
    }
}


// Coroutine dispatchers interface remains the same
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

*/
