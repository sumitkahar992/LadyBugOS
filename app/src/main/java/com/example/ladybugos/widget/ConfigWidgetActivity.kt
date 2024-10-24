package com.example.ladybugos.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.ladybugos.model.Note
import com.example.ladybugos.ui.theme.LadyBugOSTheme
import com.example.ladybugos.widget.model.WidgetConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@AndroidEntryPoint
class ConfigWidgetActivity : ComponentActivity() {
    private val viewModel: NoteSelectionViewModel by viewModel()
    private var widgetId = WidgetConstants.INVALID_WIDGET_ID
    private lateinit var widgetUpdater: WidgetUpdater


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize widgetUpdater
        widgetUpdater = WidgetUpdater(applicationContext)

        if (!setupWidget()) {
            return
        }
        setContent {
            LadyBugOSTheme {
                NoteSelectionContent(
                    viewModel = viewModel,
                    widgetId = widgetId,
                    onNoteSelected = ::handleNoteSelection
                )
            }
        }
    }

    private fun setupWidget(): Boolean {
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Always set result first, even if invalid, to ensure proper widget lifecycle
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )

        // Check if widget ID is valid
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return false
        }

        return true
    }

    private fun handleNoteSelection(note: Note?) {
        if (note == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                updateWidget(note)
                // Set result and finish on main thread
                withContext(Dispatchers.Main) {
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                    finish()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update widget")
                // Handle error appropriately
            }
        }
    }

    private suspend fun updateWidget(note: Note) {
        val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(widgetId)

        // Use widgetUpdater instead of direct update
        widgetUpdater.updateWidgetFromConfig(glanceId, note)
    }
}









