package com.despicable.notifications

import android.content.Context
import com.despicable.core.model.Note
import com.despicable.core.model.NoteContent

class NotificationHelper(private val context: Context) {

    fun scheduleNotification(note: Note) {
        note.reminderDate?.let { reminderDate ->

            val contentString = when (val content = note.content) {
                is NoteContent.Text -> content.text
                is NoteContent.ChecklistItems -> {
                    if (content.items.isEmpty()) {
                        "Empty checklist"
                    } else {
                        content.items.joinToString("\n") {
                            if (it.isChecked) "✓ ${it.content}" else "• ${it.content}"
                        }
                    }
                }
            }

            NotificationWorker.scheduleNotification(
                context,
                note.id,
                note.title,
                contentString,
                reminderDate
            )
        }
    }


    fun cancelNotification(noteId: Long) {
        NotificationWorker.cancelNotification(context, noteId)
    }

}