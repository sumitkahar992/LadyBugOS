package com.despicable.widgets.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
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
import com.despicable.core.model.getRelativeTimeAgo
import com.despicable.widgets.R
import com.despicable.widgets.data.ConfigWidgetActivity
import com.despicable.widgets.data.NoteWidgetRepository
import com.despicable.widgets.data.WidgetUpdater
import com.despicable.widgets.model.WidgetChecklistItem
import com.despicable.widgets.model.WidgetKeys
import com.despicable.widgets.ui.ToggleChecklistItemCallback.Companion.itemIdKey
import com.despicable.widgets.ui.ToggleChecklistItemCallback.Companion.noteIdKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber


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
    widgetId: Int,

    ) {
    val noteId = prefs[WidgetKeys.Prefs.noteId]?.toLongOrNull()
    val noteHeader = prefs[WidgetKeys.Prefs.noteHeader]
    val noteBody = prefs[WidgetKeys.Prefs.noteBody]
    val updatedAtString = prefs[WidgetKeys.Prefs.noteLastUpdate]
    // Get the color index directly instead of creating a Color
    val colorId = prefs[WidgetKeys.Prefs.noteColor] ?: 0
    val isDeleted = prefs[WidgetKeys.Prefs.isDeleted] ?: false

    val isChecklist = prefs[WidgetKeys.Prefs.isChecklist] ?: false
    val checklistItems = prefs[WidgetKeys.Prefs.checklistItems]?.let {
        try {
            // First try to decode as WidgetChecklistItem and convert to ChecklistItem
            val widgetItems = Json.decodeFromString<List<WidgetChecklistItem>>(it)
            widgetItems.map { item ->
                WidgetChecklistItem(
                    id = item.id,
                    content = item.content,
                    isChecked = item.isChecked,
                    position = item.position
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode checklist items: $it")
            emptyList()
        }
    } ?: emptyList()


    // Background color based on system theme
    val backgroundColor = ColorProvider(
        day = getColorFromPalette(colorId, isLight = true),
        night = getColorFromPalette(colorId, isLight = false)
    )

    /**
     * As corner radius is only available from Android 12+
     * For lower versions adding a background with a  filled shape of same
     * background color and corner radius.
     */
    val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceModifier
            .cornerRadius(16.dp)
            .background(backgroundColor)
    } else {
        GlanceModifier
            .background(ImageProvider(R.drawable.rounded_corner))
    }


    Timber.tag("DEBUG").d("[ WIDGEET ] isCheckList = $isChecklist")
    Timber.tag("DEBUG").d("[ WIDGEET ] checklistItems = $checklistItems")

// Parse updatedAtString into an Instant
    val formattedDate = updatedAtString?.let {
        try {
            Instant.parse(it) // Converts the string to an Instant
        } catch (e: Exception) {
            null // Parsing failed
        }
    } ?: Instant.DISTANT_PAST // Default value if null or invalid



    Box(
        modifier = GlanceModifier
            .appWidgetBackground()
            .fillMaxSize()
            .then(background)
            .padding(16.dp)

    ) {
        if (!isDeleted && noteId != null && noteHeader != null) {
            if (isChecklist) {
                ChecklistNote(
                    noteHeader = noteHeader,
                    checklistItems = checklistItems,
                    updatedAt = formattedDate,
                    noteId = noteId,
                    widgetId = widgetId,
                )
            } else {
                SelectedNote(
                    noteHeader = noteHeader,
                    noteBody = "$noteBody",
                    updatedAt = formattedDate,
                    noteId = noteId,
                    widgetId = widgetId
                )
            }
        } else {
            ZeroState(widgetId) // Show ZeroState if note details are missing
        }
    }
}


@Composable
fun ChecklistNote(
    noteHeader: String,
    checklistItems: List<WidgetChecklistItem>,
    updatedAt: Instant,
    noteId: Long,
    widgetId: Int

) {
    val formattedUpdateAt = getRelativeTimeAgo(updatedAt)

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
                Text(
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(AndroidDestinations.homeIntent(noteId)))
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = "Edited: $formattedUpdateAt",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(
                            day = Color.DarkGray,
                            night = Color.LightGray
                        )
                    )
                )
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
            /*       .clickable(
                       actionStartActivity(
                           AndroidDestinations.toggleChecklistItem(
                               noteId = noteId,
                               itemId = item.id,
                               widgetId = widgetId
                           )
                       )
                   ),*/
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
    private val widgetUpdater: WidgetUpdater by inject() // Inject WidgetUpdater

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[noteIdKey]
        val itemId = parameters[itemIdKey]

        if (noteId == null || itemId == null) {
            return
        }

        // Toggle the checklist item in the database
        repository.toggleChecklistItem(noteId, itemId)

        // Fetch the updated note and checklist items
        val updatedNote = repository.getNoteById(noteId).firstOrNull()
        val checklistItems = repository.getChecklistItems(noteId) // Ensure this method exists

        if (updatedNote != null && checklistItems != null) {
            // Update the widget state with the full note and checklist data
            widgetUpdater.updateWidgetFromConfig(glanceId, updatedNote, checklistItems)
        } else if (checklistItems != null) {
            // Fallback: update only the checklist items if the note fetch fails
            widgetUpdater.updateWidgetStateWithChecklist(glanceId, noteId, checklistItems)
        } else {
            // Log an error if data can't be fetched
            Timber.e("Failed to fetch updated data for note #$noteId after toggling checklist item")
        }
    }

    companion object {
        val noteIdKey = ActionParameters.Key<Long>("noteId")
        val itemIdKey = ActionParameters.Key<Long>("itemId")
    }
}

/*
@Composable
fun SelectedNote(
    noteHeader: String,
    noteBody: String,
    updatedAt: Instant,
    noteId: Long,
    widgetId: Int
) {
    val formattedUpdateAt = getRelativeTimeAgo(updatedAt)

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
//                        .openNote(noteId, widgetId)
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
//                        .openNote(noteId, widgetId)
                        .fillMaxWidth().height(5.dp)
                )
            }
            item {

                Text(
                    modifier = GlanceModifier
                        .clickable(
                            actionStartActivity(AndroidDestinations.homeIntent(noteId))
                        )
//                        .openNote(noteId, widgetId)
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
                Spacer(
//                    modifier = GlanceModifier.openNote(noteId, widgetId).fillMaxWidth().height(5.dp)
                )

            }

            item {
                Text(
                    modifier = GlanceModifier
                        .clickable(
                            actionStartActivity(AndroidDestinations.homeIntent(noteId))
                        )
                        //                        .openNote(noteId, widgetId)
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    text = "Edited: $formattedUpdateAt",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(
                            day = Color.DarkGray,
                            night = Color.LightGray
                        )
                    )
                )
            }
        }
    }

}
*/


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

@Composable
fun SelectedNote(
    noteHeader: String,
    noteBody: String,
    updatedAt: Instant,
    noteId: Long,
    widgetId: Int
) {

//    val formattedUpdateAt = formatUpdateDate(updatedAt)
    val formattedUpdateAt = getRelativeTimeAgo(updatedAt)


    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable {
                actionStartActivity(AndroidDestinations.homeIntent(noteId))
            }
    ) {
        Text(
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
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = noteBody,
            maxLines = 3,
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(
                    day = Color(0xFF3E3E3E),
                    night = Color(0xFFF6F6F6)
                )
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Edited: $formattedUpdateAt",
            style = TextStyle(
                fontSize = 11.sp,
                color = ColorProvider(
                    day = Color.DarkGray,
                    night = Color.LightGray
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
//        return Intent(Intent.ACTION_VIEW, Uri.parse(HOME_DEEP_LINK))
    }


    /*   Main activity's deep link  */
    private const val HOME_DEEP_LINK = "app://com.despicable.ladybugos"

//    fun toggleChecklistItem(noteId: Long, itemId: String, widgetId: Int): Intent {
//        return Intent(context, YourWidgetUpdateService::class.java).apply {
//            action = ACTION_TOGGLE_CHECKLIST_ITEM
//            putExtra(EXTRA_NOTE_ID, noteId)
//            putExtra(EXTRA_ITEM_ID, itemId)
//            putExtra(EXTRA_WIDGET_ID, widgetId)
//        }
//    }
}


/*private fun GlanceModifier.openNote(noteId: Long, widgetId: Int) =
    this.clickable(
        actionStartActivity<MainActivity>(
            parameters = actionParametersOf(
                ActionParameters.Key<Long>("noteId") to noteId,
                ActionParameters.Key<String>(AppWidgetManager.EXTRA_APPWIDGET_ID) to "$widgetId"
            )
        )
    )*/


/*private fun actionOpenNote(noteId: String?, widgetId: Int) = actionStartActivity<MainActivity>(
    parameters = actionParametersOf(
        ActionParameters.Key<String>("noteId") to (noteId ?: ""),
        ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID) to widgetId
    )
)*/


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

