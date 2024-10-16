package com.example.ladybugos.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.example.ladybugos.model.Note
import com.example.ladybugos.ui.theme.LadyBugOSTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@AndroidEntryPoint
class ConfigWidgetActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val result = Intent()
    private val viewModel: NoteSelectionViewModel by viewModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setupActivity()
        setContent {
            LadyBugOSTheme {
                NoteSelectionContent(
                    viewModel = viewModel,
                    onNoteSelected = ::handleSelectNote
                )
            }
        }
    }


    private fun handleSelectNote(note: Note?) {
        if (note == null) return
        setResult(RESULT_OK, result)
        finish()
        updateWidgetState(note)
    }

    private fun updateWidgetState(note: Note) = lifecycleScope.launch(Dispatchers.IO) {
        val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(widgetId)
        updateAppWidgetState(application.applicationContext, glanceId) { prefs ->
            prefs[WidgetKeys.Prefs.noteId] = note.id.toString()
            prefs[WidgetKeys.Prefs.noteHeader] = note.title
            prefs[WidgetKeys.Prefs.noteBody] = note.content
            prefs[WidgetKeys.Prefs.noteLastUpdate] = note.updateDate
            prefs[WidgetKeys.Prefs.noteColor] = note.lightColor
        }
        NoteWidget().update(application.applicationContext, glanceId)
    }

    private fun setupActivity() {
        setResult(RESULT_CANCELED, result)
        getWidgetId()
        initResult()

    }

    private fun getWidgetId() {
        widgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: return
        Timber.tag("DEBUG").d("ConfigWidgetActivity[widgetId]=[$widgetId]")
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()
    }

    private fun initResult() = result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)


}








