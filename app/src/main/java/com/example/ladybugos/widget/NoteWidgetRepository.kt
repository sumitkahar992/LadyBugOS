package com.example.ladybugos.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.collection.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ladybugos.repository.NoteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private const val DATASTORE_NAME = "widget_preferences"
private const val WIDGET_KEY_PREFIX = "widget_"

/**
 * DataStore extension property with improved error handling
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

/**
 * Repository for managing widget preferences and state
 */
class NoteWidgetRepository @Inject constructor(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    private val widgetKeyCache = LruCache<Int, Preferences.Key<Long>>(100).apply {
        maxSize()
    }

    private val dataStore = context.dataStore

    /**
     * Sealed interface for Widget operations results
     */
    sealed interface WidgetResult {
        data class Success(val widgetId: Int) : WidgetResult
        data class Error(val exception: Exception, val widgetId: Int? = null) : WidgetResult
    }

    /**
     * Gets or creates a preferences key for a widget
     */
    private fun getWidgetKey(widgetId: Int): Preferences.Key<Long> =
        widgetKeyCache[widgetId] ?: longPreferencesKey("${WIDGET_KEY_PREFIX}$widgetId").also {
            widgetKeyCache.put(widgetId, it)
        }

    /**
     * Saves widget note association and updates widget
     */
    suspend fun saveWidgetNoteId(widgetId: Int, noteId: Long): WidgetResult =
        withContext(dispatchers.io) {
            try {
                // Save to DataStore
                dataStore.edit { preferences ->
                    preferences[getWidgetKey(widgetId)] = noteId
                }

                // Update widget if note exists
                noteRepository.getNoteById(noteId)
                    .firstOrNull()
                    ?.let { note ->
                        widgetUpdater.updateSingleWidget(note)
                    } ?: throw NoSuchElementException("Note not found: $noteId")

                WidgetResult.Success(widgetId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save widget note ID: widgetId=$widgetId, noteId=$noteId")
                WidgetResult.Error(e, widgetId)
            }
        }

    /**
     * Restores all widgets and cleans up orphaned data
     */
    suspend fun restoreWidgets(): WidgetResult = withContext(dispatchers.io) {
        try {
            Timber.tag("DEBUG").d("[restoreWidgets()] -")
            coroutineScope {
                // Launch widget updates and cleanup in parallel
                val updateJob = async { updateAllWidgets() }
                val cleanupJob = async { cleanupOrphanedWidgets() }

                // Wait for both operations to complete
                updateJob.await()
                cleanupJob.await()
            }

            WidgetResult.Success(-1) // Using -1 to indicate batch operation
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore widgets")
            WidgetResult.Error(e)
        }
    }

    /**
     * Updates all widgets with current note data
     */
    private suspend fun updateAllWidgets() {
        noteRepository.getAllNotes()
            .first()
            .let { notes ->
                widgetUpdater.updateAllWidgets(notes)
            }
    }

    /**
     * Cleans up orphaned widget data
     */
    private suspend fun cleanupOrphanedWidgets() {
        val activeWidgetIds = getActiveWidgetIds()
        val storedWidgetIds = getStoredWidgetIds()
        val orphanedIds = storedWidgetIds - activeWidgetIds

        if (orphanedIds.isNotEmpty()) {
            removeOrphanedWidgets(orphanedIds)
        }
    }

    /**
     * Gets currently active widget IDs
     */
    private fun getActiveWidgetIds(): Set<Int> {
        val widgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, NotesWidgetReceiver::class.java)
        return widgetManager.getAppWidgetIds(componentName).toSet()
    }

    /**
     * Gets stored widget IDs from preferences
     */
    private suspend fun getStoredWidgetIds(): Set<Int> =
        dataStore.data.first().asMap()
            .mapNotNull { (key, _) ->
                key.name.removePrefix(WIDGET_KEY_PREFIX).toIntOrNull()
            }
            .toSet()

    /**
     * Removes orphaned widget data
     */
    private suspend fun removeOrphanedWidgets(orphanedIds: Set<Int>) {
        dataStore.edit { preferences ->
            orphanedIds.forEach { widgetId ->
                preferences.remove(getWidgetKey(widgetId))
                widgetKeyCache.remove(widgetId)
            }
        }
    }
}