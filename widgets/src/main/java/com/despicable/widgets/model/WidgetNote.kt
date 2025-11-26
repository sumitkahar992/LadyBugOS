package com.despicable.widgets.model

import android.appwidget.AppWidgetManager
import androidx.annotation.Keep
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WidgetNote(
    val id: Long,
    val title: String,
    val content: WidgetContent,
    val reminderDate: Instant?,
    val color: Int,
)

@Serializable
sealed class WidgetContent {
    @Serializable
    data class Text(val text: String) : WidgetContent()

    @Serializable
    data class Checklist(val items: List<WidgetChecklistItem>) : WidgetContent()
}

@Keep
@Serializable
data class WidgetChecklistItem(
    val id: Long,
    val content: String,
    val isChecked: Boolean,
)


object WidgetKeys {
    object Prefs {
        val noteId = stringPreferencesKey("noteId")
        val noteHeader = stringPreferencesKey("noteHeader")
        val noteBody = stringPreferencesKey("noteBody")
        val noteReminderDate = stringPreferencesKey("noteReminderDate")
        val noteColor = intPreferencesKey("noteColor") // New key for storing the note color
        val isDeleted = booleanPreferencesKey("isDeleted")


        val isChecklist = booleanPreferencesKey("is_checklist")
        val checklistItems = stringPreferencesKey("checklist_items")
    }
}

object WidgetConstants {
    const val INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID
}