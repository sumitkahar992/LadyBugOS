package com.despicable.widgets.mapper

import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.NoteWithTagsEntity
import com.despicable.core.database.model.TagEntity
import com.despicable.core.model.Note
import com.despicable.core.model.Tag
import com.despicable.widgets.model.WidgetNote
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.despicable.core.model.NoteWithTags as DomainNoteWithTags

/**
 * Mapper class to convert between domain and entity models
 */
object NoteMapper {

    fun Note.toEntity(): NoteEntity = NoteEntity(
        id = id,
        title = title,
        content = content,
        updateDate = updateDate,
        lightColor = lightColor,
        isPinned = isPinned,
        pinnedDate = pinnedDate,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        isChecklist = isChecklist
    )

    fun NoteEntity.toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        updateDate = updateDate,
        lightColor = lightColor,
        isPinned = isPinned,
        pinnedDate = pinnedDate,
        isArchived = isArchived,
        isTrashed = isTrashed,
        reminderDate = reminderDate,
        isDone = isDone,
        isChecklist = isChecklist
    )

    fun Tag.toEntity(): TagEntity = TagEntity(
        id = id,
        name = name
    )

    fun TagEntity.toDomain(): Tag = Tag(
        id = id,
        name = name
    )

    fun NoteWithTagsEntity.toDomain(): DomainNoteWithTags = DomainNoteWithTags(
        note = note.toDomain(),
        tags = tags.map { it.toDomain() }
    )

    // Extension function to convert list of entities to domain models

    // Widget Note Mapping Functions
    fun Note.toWidgetNote(): WidgetNote = WidgetNote(
        id = id.toString(),
        title = title,
        content = content,
        lastUpdate = updateDate.toString(),
        reminderDate = reminderDate?.let { timestamp ->
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } ?: "",
        color = lightColor,
        isChecklist = isChecklist
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

    fun NoteWithTagsEntity.toWidgetNote(): WidgetNote = note.toDomain().toWidgetNote()
}

