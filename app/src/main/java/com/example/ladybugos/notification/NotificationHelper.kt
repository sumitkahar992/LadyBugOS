package com.example.ladybugos.notification

import android.content.Context
import com.example.ladybugos.model.Note

class NotificationHelper(private val context: Context) {

    fun scheduleNotification(note: Note) {
        note.reminderDate?.let { reminderDate ->
            NotificationWorker.scheduleNotification(
                context,
                note.id,
                note.title,
                note.content,
                reminderDate
            )
        }
    }


    fun cancelNotification(noteId: Long) {
        NotificationWorker.cancelNotification(context, noteId)
    }

}