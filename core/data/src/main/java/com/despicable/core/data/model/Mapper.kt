package com.despicable.core.data.model

import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.NoteTagRefEntity
import com.despicable.core.database.model.NoteWithTagsEntity
import com.despicable.core.database.model.TagEntity
import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import com.despicable.core.database.model.NoteComplete as DbNoteComplete

/*
    Extension functions for mapping between domain and entity models
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

// Checklist Mappings
fun Checklist.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    noteId = noteId,
    content = content,
    isChecked = isChecked,
    position = position
)

fun ChecklistEntity.toDomain(): Checklist = Checklist(
    id = id,
    noteId = noteId,
    content = content,
    isChecked = isChecked,
    position = position
)

// Relationship Mappings
fun NoteWithTagsEntity.toDomain(): NoteWithTags = NoteWithTags(
    note = note.toDomain(),
    tags = tags.map { it.toDomain() }
)

fun DbNoteComplete.toDomain(): NoteComplete = NoteComplete(
    note = note.toDomain(),
    checklistItems = checklistItems.map { it.toDomain() },
    tags = tags.map { it.toDomain() }
)

// List Mappings
fun List<Note>.toNoteEntityList(): List<NoteEntity> = map { it.toEntity() }
fun List<NoteEntity>.toNoteDomainList(): List<Note> = map { it.toDomain() }
fun List<Tag>.toTagEntityList(): List<TagEntity> = map { it.toEntity() }
fun List<TagEntity>.toTagDomainList(): List<Tag> = map { it.toDomain() }
fun List<NoteWithTagsEntity>.toNoteTagsDomainList(): List<NoteWithTags> = map { it.toDomain() }
fun List<DbNoteComplete>.toNoteCompleteDomainList(): List<NoteComplete> = map { it.toDomain() }

// Nullable Mappings
fun NoteEntity?.toDomainOrNull(): Note? = this?.toDomain()
fun TagEntity?.toDomainOrNull(): Tag? = this?.toDomain()
fun ChecklistEntity?.toDomainOrNull(): Checklist? = this?.toDomain()
fun NoteWithTagsEntity?.toDomainOrNull(): NoteWithTags? = this?.toDomain()
fun DbNoteComplete?.toDomainOrNull(): NoteComplete? = this?.toDomain()

// Helper for creating note tag cross references
fun createNoteTagCrossRef(noteId: Long, tagId: Long): NoteTagRefEntity =
    NoteTagRefEntity(noteId = noteId, tagId = tagId)

















