package com.despicable.core.data.model

import com.despicable.database.model.NoteEntity
import com.despicable.database.model.NoteWithTagsEntity
import com.despicable.database.model.TagEntity
import com.despicable.model.Note
import com.despicable.model.NoteWithTags
import com.despicable.model.Tag

/**
 * Extension functions for mapping between domain and entity models
 */

// Note Mappings
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

// Tag Mappings
fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name
)

// NoteWithTags Mappings
fun NoteWithTagsEntity.toDomain(): NoteWithTags = NoteWithTags(
    note = note.toDomain(),
    tags = tags.map { it.toDomain() }
)

// List Mappings
fun List<Note>.toNoteEntityList(): List<NoteEntity> = map { it.toEntity() }
fun List<NoteEntity>.toNoteDomainList(): List<Note> = map { it.toDomain() }
fun List<Tag>.toTagEntityList(): List<TagEntity> = map { it.toEntity() }
fun List<TagEntity>.toTagDomainList(): List<Tag> = map { it.toDomain() }
fun List<NoteWithTagsEntity>.toNoteTagsDomainList(): List<NoteWithTags> = map { it.toDomain() }

// Nullable Mappings
fun NoteEntity?.toDomainOrNull(): Note? = this?.toDomain()
fun TagEntity?.toDomainOrNull(): Tag? = this?.toDomain()
fun NoteWithTagsEntity?.toDomainOrNull(): NoteWithTags? = this?.toDomain()