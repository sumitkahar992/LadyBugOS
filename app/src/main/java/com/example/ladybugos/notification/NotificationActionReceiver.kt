package com.example.ladybugos.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.ladybugos.MainActivity
import com.example.ladybugos.notification.NotificationWorker.Companion.KEY_NOTE_ID
import org.koin.core.component.KoinComponent

class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {
//    private val noteRepository: NoteRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(KEY_NOTE_ID)?.toLongOrNull() ?: -1L



        if (noteId != -1L) {

            // Dismiss the notification
            val notificationManager =
                ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.cancel(noteId.toInt())


            // Open the Edit Note Screen
            val editNoteIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(KEY_NOTE_ID, noteId)
                putExtra("REMINDER_ACTION", intent.action)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(editNoteIntent)
        }
    }


}


