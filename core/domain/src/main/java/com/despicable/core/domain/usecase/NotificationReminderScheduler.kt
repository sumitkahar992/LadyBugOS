package com.despicable.core.domain.usecase

import android.content.Context
import com.despicable.core.data.repository.ReminderScheduler
import com.despicable.core.model.Note
import com.despicable.notifications.NotificationHelper


class NotificationReminderScheduler(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) : ReminderScheduler {

    override fun scheduleReminder(note: Note) {
        notificationHelper.scheduleNotification(note)
    }

    override fun cancelReminder(noteId: Long) {
        notificationHelper.cancelNotification(noteId)
    }
}