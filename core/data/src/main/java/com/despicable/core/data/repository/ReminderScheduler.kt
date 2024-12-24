package com.despicable.core.data.repository

import com.despicable.core.model.Note

interface ReminderScheduler {
    fun scheduleReminder(note: Note)
    fun cancelReminder(noteId: Long)
}


