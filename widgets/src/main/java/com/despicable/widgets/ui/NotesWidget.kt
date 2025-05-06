package com.despicable.widgets.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.despicable.core.designsystem.DarkNoteColors
import com.despicable.core.designsystem.LightNoteColors
import com.despicable.core.designsystem.component.formatReminderDate
import com.despicable.widgets.R
import com.despicable.widgets.data.ConfigWidgetActivity
import com.despicable.widgets.data.CoroutineDispatchers
import com.despicable.widgets.data.NoteWidgetRepository
import com.despicable.widgets.data.WidgetUpdater
import com.despicable.widgets.mapper.toNote
import com.despicable.widgets.model.WidgetChecklistItem
import com.despicable.widgets.model.WidgetContent
import com.despicable.widgets.model.WidgetKeys
import com.despicable.widgets.ui.ToggleChecklistItemCallback.Companion.itemIdKey
import com.despicable.widgets.ui.ToggleChecklistItemCallback.Companion.noteIdKey
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class NoteWidget : GlanceAppWidget() {
    override var stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                NoteWidgetContent(prefs, context, widgetId)
            }
        }
    }
}


@Composable
fun NoteWidgetContent(
    prefs: Preferences,
    context: Context,
    widgetId: Int
) {
    // Extract preferences
    val noteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()
    val noteHeader = prefs[WidgetKeys.Prefs.noteHeader]
    val isDeleted = prefs[WidgetKeys.Prefs.isDeleted] ?: false
    val colorId = prefs[WidgetKeys.Prefs.noteColor] ?: 0

    // Parse reminder date
    val reminderDateString = prefs[WidgetKeys.Prefs.noteReminderDate]
    val formattedDate = reminderDateString?.toInstantOrNull() ?: Instant.DISTANT_PAST

    // Parse checklist items
    val checklistJson = prefs[WidgetKeys.Prefs.checklistItems] ?: "[]"
    val checklistItems = remember(checklistJson) {
        parseChecklistItems(checklistJson)
    }

    // Determine content type
    val isChecklist = prefs[WidgetKeys.Prefs.isChecklist] ?: false
    val widgetContent = if (isChecklist && checklistItems.isNotEmpty()) {
        WidgetContent.Checklist(checklistItems)
    } else {
        val noteBody = prefs[WidgetKeys.Prefs.noteBody] ?: ""
        WidgetContent.Text(noteBody)
    }


    // Background with color and corner radius
    val backgroundColor = ColorProvider(
        day = getColorFromPalette(colorId, isLight = true),
        night = getColorFromPalette(colorId, isLight = false)
    )

    /**
     * As corner radius is only available from Android 12+
     * For lower versions adding a background with a  filled shape of same
     * background color and corner radius.
     */
    val backgroundModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceModifier.cornerRadius(16.dp).background(backgroundColor)
    } else {
        GlanceModifier.background(ImageProvider(R.drawable.rounded_corner))
    }

    Box(
        modifier = GlanceModifier
            .appWidgetBackground()
            .fillMaxSize()
            .then(backgroundModifier)
            .padding(16.dp)
    ) {
        if (!isDeleted && noteId != null && noteHeader != null) {
            when (widgetContent) {
                is WidgetContent.Text -> {
                    Log.e("WIDGET", "NoteWidgetContent:${widgetContent.text} ")

                    SelectedNote(
                        noteHeader = noteHeader,
                        noteBody = widgetContent.text,
                        reminderDate = formattedDate,
                        noteId = noteId,
                        widgetId = widgetId
                    )
                }

                is WidgetContent.Checklist -> {
                    Log.e("WIDGET", "NoteWidgetContent:${widgetContent.items} ")

                    ChecklistNote(
                        noteHeader = noteHeader,
                        checklistItems = widgetContent.items,
                        reminderDate = formattedDate,
                        noteId = noteId,
                        widgetId = widgetId
                    )
                }
            }
        } else {
            ZeroState(widgetId) // Show ZeroState if note details are missing
        }
    }
}


private fun String.toInstantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (e: Exception) {
    Log.e("WIDGET", "Failed to parse Instant from string: $this", e)
    null
}

private fun parseChecklistItems(json: String): List<WidgetChecklistItem> {
    return try {
        Json.decodeFromString<List<WidgetChecklistItem>>(json)
    } catch (e: Exception) {
        Log.e("WIDGET", "Failed to decode checklist items: $json", e)
        emptyList()
    }
}


@Composable
fun ChecklistNote(
    noteHeader: String,
    checklistItems: List<WidgetChecklistItem>,
    reminderDate: Instant,
    noteId: Long,
    widgetId: Int

) {
    val formattedDate = remember(reminderDate) {
        formatReminderDate(reminderDate)
    }
    Box(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            item {
                Text(
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(AndroidDestinations.homeIntent(noteId)))
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    text = noteHeader,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ColorProvider(
                            day = Color(0xFF3E3E3E),
                            night = Color(0xFFF6F6F6)
                        )
                    )
                )
            }

            items(checklistItems) { item ->
                ChecklistItemRow(
                    item = item,
                    noteId = noteId,
                    widgetId = widgetId
                )
            }


            item {
                WidgetReminder(noteId, formattedDate)
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: WidgetChecklistItem,
    noteId: Long,
    widgetId: Int

) {

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Image(
            modifier = GlanceModifier
                .size(20.dp)
                .clickable(
                    actionRunCallback<ToggleChecklistItemCallback>(
                        parameters = actionParametersOf(
                            noteIdKey to noteId,
                            itemIdKey to item.id,
                        )
                    )
                ),
            provider = ImageProvider(
                if (item.isChecked) R.drawable.check_box
                else R.drawable.uncheck_box
            ),
            contentDescription = if (item.isChecked) "Checked" else "Unchecked",
            colorFilter = ColorFilter.tint(
                ColorProvider(
                    day = Color.DarkGray,
                    night = Color.LightGray
                )
            )
        )

        // Item text
        Text(
            modifier = GlanceModifier
                .padding(start = 8.dp)
                .defaultWeight(),
            text = item.content,
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(
                    day = if (item.isChecked) Color.Gray else Color(0xFF3E3E3E),
                    night = if (item.isChecked) Color.Gray else Color(0xFFF6F6F6)
                ),
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
        )
    }
}


class ToggleChecklistItemCallback : ActionCallback, KoinComponent {
    private val repository: NoteWidgetRepository by inject()
    private val dispatchers: CoroutineDispatchers by inject()
    private val widgetUpdater: WidgetUpdater by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[noteIdKey] ?: return
        val itemId = parameters[itemIdKey] ?: return

        withContext(dispatchers.io) {
            // Toggle item in DB (single operation)
            repository.toggleChecklistItem(noteId, itemId)

            repository.getNoteCompleteById(noteId)?.toNote()?.let {
                // Update all widgets associated with the note
                widgetUpdater.updateSingleWidget(it)

            } ?: run {
                Log.e("WIDGET", "Note not found for noteId=$noteId after toggle attempt")
            }
        }
    }

    companion object {
        val noteIdKey = ActionParameters.Key<Long>("noteId")
        val itemIdKey = ActionParameters.Key<Long>("itemId")
    }
}

@Composable
fun SelectedNote(
    noteHeader: String,
    noteBody: String,
    reminderDate: Instant,
    noteId: Long,
    widgetId: Int
) {
    val formattedDate = remember(reminderDate) {
        formatReminderDate(reminderDate)
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
        ) {
            item {
                Text(
                    modifier = GlanceModifier
                        .clickable(
                            actionStartActivity(AndroidDestinations.homeIntent(noteId))
                        )
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    text = noteHeader,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ColorProvider(
                            day = Color(0xFF3E3E3E),
                            night = Color(0xFFF6F6F6)
                        )
                    )
                )
            }
            item {
                Spacer(
                    modifier = GlanceModifier
                        .clickable(
                            actionStartActivity(AndroidDestinations.homeIntent(noteId))
                        )
                        .fillMaxWidth().height(5.dp)
                )
            }
            item {

                Text(
                    modifier = GlanceModifier
                        .clickable(
                            actionStartActivity(AndroidDestinations.homeIntent(noteId))
                        )
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    text = noteBody,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(
                            day = Color(0xFF3E3E3E),
                            night = Color(0xFFF6F6F6)
                        )
                    )
                )
            }

            item {
                WidgetReminder(noteId, formattedDate)

            }
        }
    }

}

@Composable
private fun WidgetReminder(noteId: Long, formattedDate: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp).clickable(
            actionStartActivity(AndroidDestinations.homeIntent(noteId))
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            provider = ImageProvider(R.drawable.alarm),
            contentDescription = "Tinted icon",
            modifier = GlanceModifier.size(22.dp).padding(end = 6.dp),
            colorFilter = ColorFilter.tint(
                ColorProvider(
                    day = Color.DarkGray,
                    night = Color.LightGray
                )
            )
        )


        Text(
            text = formattedDate,
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(
                    day = Color.DarkGray,
                    night = Color.LightGray
                )
            )
        )
    }
}


@Composable
fun ZeroState(widgetId: Int) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No note selected",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = GlanceTheme.colors.onSurface
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Button(
            text = "Select Note",
            onClick = actionStartActivity<ConfigWidgetActivity>(
                parameters = actionParametersOf(
                    ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID) to widgetId
                )
            )
        )
    }
}


object AndroidDestinations {

    /*   Returns the [Intent] to the home screen with an optional note ID.  */
    fun homeIntent(noteId: Long? = null): Intent {

        val uriBuilder = Uri.Builder()
            .scheme("note")
            .authority("com.despicable.ladybugos")

        noteId?.let {
            uriBuilder.appendPath("notes")
            uriBuilder.appendPath(it.toString())
        }

        return Intent(Intent.ACTION_VIEW, uriBuilder.build())
    }


}


// Simplify getColorFromPalette to use colorId directly instead of trying to map it
private fun getColorFromPalette(colorId: Int, isLight: Boolean): Color {
    val colorPalette = if (isLight) LightNoteColors else DarkNoteColors

    return if (colorId >= 0 && colorId < colorPalette.size) {
        colorPalette[colorId]
    } else {
        // Fallback to default colors
        if (isLight) Color.White else Color(0xFF202124)
    }
}

