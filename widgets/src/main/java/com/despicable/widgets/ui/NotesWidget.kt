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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.despicable.core.designsystem.darken
import com.despicable.core.model.getRelativeTimeAgo
import com.despicable.widgets.R
import com.despicable.widgets.data.ConfigWidgetActivity

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
    val noteId = prefs[com.despicable.widgets.model.WidgetKeys.Prefs.noteId]?.toLongOrNull()
    val noteHeader = prefs[com.despicable.widgets.model.WidgetKeys.Prefs.noteHeader]
    val noteBody = prefs[com.despicable.widgets.model.WidgetKeys.Prefs.noteBody]
    val updatedAt = prefs[com.despicable.widgets.model.WidgetKeys.Prefs.noteLastUpdate]
    val noteColor =
        prefs[com.despicable.widgets.model.WidgetKeys.Prefs.noteColor]?.let { Color(it) } // Retrieve note color
    val isDeleted = prefs[com.despicable.widgets.model.WidgetKeys.Prefs.isDeleted] ?: false

    // Background color based on system theme
    val backgroundColor = ColorProvider(
        day = noteColor ?: Color.LightGray,
        night = noteColor?.darken(0.4f) ?: Color.DarkGray
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

    Box(
        modifier = GlanceModifier
            .appWidgetBackground()
            .fillMaxSize()
            .then(background)
            .padding(16.dp)

    ) {
        if (!isDeleted && noteId != null && noteHeader != null) {
            SelectedNote(
                noteHeader = noteHeader,
                noteBody = "$noteBody",
                updatedAt = "$updatedAt",
                noteId = noteId,
                widgetId = widgetId
            )
        } else {
            ZeroState(widgetId) // Show ZeroState if note details are missing
        }
    }
}

/*@Composable
fun SelectedNote(
    noteId: String,
    noteHeader: String,
    noteBody: String,
    updatedAt: String,
    widgetId: Int
) {

//    val formattedUpdateAt = formatUpdateDate(updatedAt)
    val formattedUpdateAt = getRelativeTimeAgo(updatedAt)


    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionOpenNote(noteId, widgetId))

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
}*/

@Composable
fun SelectedNote(
    noteHeader: String,
    noteBody: String,
    updatedAt: String,
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
//        return Intent(Intent.ACTION_VIEW, Uri.parse(HOME_DEEP_LINK))
    }


    /*   Main activity's deep link  */
    private const val HOME_DEEP_LINK = "app://com.despicable.ladybugos"
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


