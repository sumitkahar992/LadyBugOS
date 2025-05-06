package com.despicable.widgets.data

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import com.despicable.core.model.NoteComplete
import com.despicable.widgets.mapper.toWidgetNote
import com.despicable.widgets.model.WidgetConstants
import com.despicable.widgets.ui.NoteSelectionContent
import com.despicable.widgets.ui.NoteSelectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel


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

    private fun handleNoteSelection(noteComplete: NoteComplete?) {
        if (noteComplete == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                updateWidget(noteComplete)
                // Set result and finish on main thread
                withContext(Dispatchers.Main) {
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                    finish()
                }
            } catch (e: Exception) {
                Log.e("WIDGET", "Failed to update widget")
                // Handle error appropriately
            }
        }
    }

    /*    private suspend fun updateWidget(note: WidgetNote) {
            val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(widgetId)

            // Use widgetUpdater instead of direct update
            widgetUpdater.updateWidgetFromConfig(glanceId, note.toDomainNote())
        }*/

    // ConfigWidgetActivity
    private suspend fun updateWidget(noteComplete: NoteComplete) {
        val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(widgetId)


        Log.e("WIDGET", "noteComplete: [${noteComplete.note}]")
        Log.e("WIDGET", "noteComplete: [${noteComplete.checklistItems}]")

        // Use widgetUpdater to update the widget
        widgetUpdater.updateWidgetFromConfig(
            glanceId,
            noteComplete.toWidgetNote()
        )
    }
}



