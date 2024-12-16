package com.despicable.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.despicable.notifications.NotificationWorker.Companion.KEY_NOTE_ID
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


            // Create deep link intent to open note
            val editNoteIntent = homeIntent(noteId).apply {
                putExtra(KEY_NOTE_ID, noteId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("REMINDER_ACTION", intent.action)

                // Ensure the intent is specifically for your app
                setPackage(context.packageName)
            }
            context.startActivity(editNoteIntent)
        }
    }





}


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






