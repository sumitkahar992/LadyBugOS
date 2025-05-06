package com.despicable.widgets.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.collection.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.despicable.core.data.repository.NoteRepository
import com.despicable.widgets.mapper.toWidgetNote
import com.despicable.widgets.model.WidgetNote
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val DATASTORE_NAME = "widget_preferences"
private const val WIDGET_KEY_PREFIX = "widget_"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

class NoteWidgetRepository @Inject constructor(
    private val context: Context,
    private val repo: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    private val widgetKeyCache = LruCache<Int, Preferences.Key<Long>>(100).apply {
        maxSize()
    }

    private val dataStore = context.dataStore

    /*   Sealed interface for Widget operations results   */
    sealed interface WidgetResult {
        data class Success(val widgetId: Int) : WidgetResult
        data class Error(val exception: Exception, val widgetId: Int? = null) : WidgetResult
    }

    /*   Gets or creates a preferences key for a widget   */
    private fun getWidgetKey(widgetId: Int): Preferences.Key<Long> =
        widgetKeyCache[widgetId] ?: longPreferencesKey("${WIDGET_KEY_PREFIX}$widgetId").also {
            widgetKeyCache.put(widgetId, it)
        }

    /*   Saves widget note association and updates widget   */
    suspend fun saveWidgetNoteId(widgetId: Int, noteId: Long): WidgetResult =
        withContext(dispatchers.io) {
            try {
                // Save to DataStore
                dataStore.edit { preferences ->
                    preferences[getWidgetKey(widgetId)] = noteId
                }

                // Update widget if note exists
                repo.getNoteCompleteById(noteId)
                    .firstOrNull()
                    ?.let { note ->
                        widgetUpdater.updateSingleWidget(note.note)
                    } ?: throw NoSuchElementException("Note not found: $noteId")

                WidgetResult.Success(widgetId)
            } catch (e: Exception) {
                Log.e("WIDGET", "Failed to save widget note ID: widgetId=$widgetId, noteId=$noteId")
                WidgetResult.Error(e, widgetId)
            }
        }

    /*
      Restores all widgets and cleans up orphaned data
     */
    suspend fun restoreWidgets(): WidgetResult = withContext(dispatchers.io) {
        try {
            Log.e("WIDGET", "[restoreWidgets()] -")
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
            Log.e("WIDGET", "Failed to restore widgets")
            WidgetResult.Error(e)
        }
    }

    /*
      Updates all widgets with current note data
     */
    private suspend fun updateAllWidgets() {
        repo.getAllNotesWithTags()
            .first()
            .let { noteComplete ->

                widgetUpdater.updateAllWidgets(noteComplete.map { it.note.toWidgetNote() })
            }
    }

    /*
      Cleans up orphaned widget data
     */
    private suspend fun cleanupOrphanedWidgets() {
        val activeWidgetIds = getActiveWidgetIds()
        val storedWidgetIds = getStoredWidgetIds()
        val orphanedIds = storedWidgetIds - activeWidgetIds

        if (orphanedIds.isNotEmpty()) {
            removeOrphanedWidgets(orphanedIds)
        }
    }


    /*
            Gets currently active widget IDs
    */
    private fun getActiveWidgetIds(): Set<Int> {
        val widgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, NotesWidgetReceiver::class.java)
        return widgetManager.getAppWidgetIds(componentName).toSet()
    }


    /*
          Gets stored widget IDs from preferences
    */
    private suspend fun getStoredWidgetIds(): Set<Int> =
        dataStore.data.first().asMap()
            .mapNotNull { (key, _) ->
                key.name.removePrefix(WIDGET_KEY_PREFIX).toIntOrNull()
            }
            .toSet()


    /*
           Removes orphaned widget data
    */
    private suspend fun removeOrphanedWidgets(orphanedIds: Set<Int>) {
        dataStore.edit { preferences ->
            orphanedIds.forEach { widgetId ->
                preferences.remove(getWidgetKey(widgetId))
                widgetKeyCache.remove(widgetId)
            }
        }
    }

    /*
            Handles widget cleanup when note is permanently deleted
    */
    suspend fun handleNoteDeleted(noteId: Long): WidgetResult = withContext(dispatchers.io) {
        try {
            // Remove from DataStore
            dataStore.edit { preferences ->
                preferences.asMap()
                    .filter { (_, value) -> value as? Long == noteId }
                    .forEach { (key, _) ->
                        preferences.remove(key)
                        // Clear from cache if present
                        key.name.removePrefix(WIDGET_KEY_PREFIX)
                            .toIntOrNull()
                            ?.let { widgetKeyCache.remove(it) }
                    }
            }


            WidgetResult.Success(-1)
        } catch (e: Exception) {
            Log.e("WIDGET", "Failed to cleanup widgets for deleted note: noteId=$noteId")
            WidgetResult.Error(e)
        }
    }

    // widget repo
    suspend fun getNoteCompleteById(noteId: Long): WidgetNote? {
        return withContext(dispatchers.io) {
            repo.getNoteCompleteById(noteId).firstOrNull()?.toWidgetNote()
        }
    }

    suspend fun toggleChecklistItem(noteId: Long, itemId: Long): Boolean {
        return withContext(dispatchers.io) {
            repo.toggleChecklistItem(noteId, itemId)
        }
    }


}