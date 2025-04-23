package com.despicable.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.despicable.core.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    // Get noteDao from Koin
    private val repository: NoteRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1)
        val noteTitle = inputData.getString(KEY_NOTE_TITLE) ?: "Reminder"
        val noteContent = inputData.getString(KEY_NOTE_CONTENT) ?: ""

        return@withContext if (noteId != -1L) {
            showNotification(noteId, noteTitle, noteContent)
            updateNoteStatus(noteId)
            Result.success()
        } else {
            Result.failure()
        }
    }

    private fun showNotification(noteId: Long, title: String, content: String) {
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)

        // Create notification channel for API >= 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val intent = homeIntent(noteId).apply {
//            putExtra("noteId", noteId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.notes)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(content)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(noteId.toInt(), notification)
    }

    private suspend fun updateNoteStatus(noteId: Long) {
        withContext(Dispatchers.IO) {
            try {
                repository.updateNoteStatus(noteId, true)
                Result.success()
            } catch (e: Exception) {
                Result.failure()
                Timber.e(e, "Failed to update note status")
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "ReminderChannel"
        const val CHANNEL_NAME = "Reminder Notifications"
        const val KEY_NOTE_ID = "noteId"
        const val KEY_NOTE_TITLE = "note_title"
        const val KEY_NOTE_CONTENT = "note_content"

        fun scheduleNotification(
            context: Context,
            noteId: Long,
            noteTitle: String,
            noteContent: String,
            reminderTime: Instant
        ) {
            val delayDuration = reminderTime - Clock.System.now()
            if (delayDuration > 0.milliseconds) {
                val inputData = workDataOf(
                    KEY_NOTE_ID to noteId,
                    KEY_NOTE_TITLE to noteTitle,
                    KEY_NOTE_CONTENT to noteContent
                )
                val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delayDuration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "notification_$noteId",
                    ExistingWorkPolicy.REPLACE,
                    notificationWork
                )
            }
        }

        fun cancelNotification(context: Context, noteId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("notification_$noteId")
        }
    }
}

