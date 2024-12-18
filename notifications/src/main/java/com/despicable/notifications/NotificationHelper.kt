package com.despicable.notifications

import android.content.Context
import com.despicable.core.database.model.NoteEntity

class NotificationHelper(private val context: Context) {

    fun scheduleNotification(note: NoteEntity) {
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