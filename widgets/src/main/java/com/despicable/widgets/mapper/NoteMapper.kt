package com.despicable.widgets.mapper

import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.widgets.model.WidgetChecklistItem
import com.despicable.widgets.model.WidgetNote
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun WidgetChecklistItem.toDomainChecklist(noteId: Long): Checklist = Checklist(
    id = id,
    noteId = noteId,
    content = content,
    isChecked = isChecked,
    position = position
)


fun WidgetNote.toDomainNote(): Note = Note(
    id = id.toLongOrNull() ?: 0L,
    title = title,
    content = content,
    updateDate = lastUpdate.toLong(),
    lightColor = color,
    isChecklist = isChecklist,
    reminderDate = if (reminderDate.isNotEmpty()) {
        try {
            LocalDateTime.parse(
                reminderDate,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    } else null
)



