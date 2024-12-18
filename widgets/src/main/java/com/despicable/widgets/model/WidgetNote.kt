package com.despicable.widgets.model

import android.appwidget.AppWidgetManager
import androidx.annotation.Keep
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WidgetNote(
    val id: String,
    val title: String,
    val content: String,
    val lastUpdate: String,
    val reminderDate: String,
    val color: Int,
)


object WidgetKeys {
    object Prefs {
        val noteId = stringPreferencesKey("noteId")
        val noteHeader = stringPreferencesKey("noteHeader")
        val noteBody = stringPreferencesKey("noteBody")
        val noteLastUpdate = stringPreferencesKey("noteLastUpdate")
        val noteColor = intPreferencesKey("noteColor") // New key for storing the note color
        val isDeleted = booleanPreferencesKey("isDeleted")

        const val NOTE_ID_EXTRA = "NoteIdExtra"

        val tempNoteId =
            stringPreferencesKey("temp_note_id")  // Make sure this is stringPreferencesKey, not booleanPreferencesKey as shown in the example

    }
}

object WidgetConstants {
    const val INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID
}