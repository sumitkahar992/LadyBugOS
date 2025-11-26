package com.despicable.core.data.model

import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.HabitCompletionEntity
import com.despicable.core.database.model.HabitEntity
import com.despicable.core.database.model.NoteCompleteEntity
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.TagEntity
import com.despicable.core.model.Checklist
import com.despicable.core.model.HabitCompletion
import com.despicable.core.model.HabitItem
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.Tag
import com.despicable.core.database.model.NoteCompleteEntity as DbNoteComplete

/*
    Extension functions for mapping between domain and entity models
 */

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    creationDate = creationDate,
    updateDate = updateDate,
    lightColor = lightColor,
    isPinned = isPinned,
    pinnedDate = pinnedDate,
    isArchived = isArchived,
    isTrashed = isTrashed,
    reminderDate = reminderDate,
    isDone = isDone,
    noteType = noteType
)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    creationDate = creationDate,
    updateDate = updateDate,
    lightColor = lightColor,
    isPinned = isPinned,
    pinnedDate = pinnedDate,
    isArchived = isArchived,
    isTrashed = isTrashed,
    reminderDate = reminderDate,
    isDone = isDone,
    noteType = noteType
)

fun NoteCompleteEntity.toDomains(): NoteComplete = NoteComplete(
    note = note.toDomain(),
    checklistItems = checklistItems.map { it.toDomain() },
    habitItems = habitItems.map { it.toDomain() },
    tags = tags.map { it.toDomain() }
)

fun NoteComplete.toEntities(): NoteCompleteEntity = NoteCompleteEntity(
    note = note.toEntity(),
    checklistItems = checklistItems.map { it.toEntity() },
    habitItems = habitItems.map { it.toEntity() },
    tags = tags.map { it.toEntity() }
)

fun HabitEntity.toDomain(): HabitItem = HabitItem(
    id = id,
    noteId = noteId,
    habitName = habitName,
    targetDays = targetDays,
    completedDays = completedDays,
    startDate = startDate,
    lastUpdated = lastUpdated,
    completions = completions.map { it.toDomain() }
)

fun HabitItem.toEntity(): HabitEntity = HabitEntity(
    id = id,
    noteId = noteId,
    habitName = habitName,
    targetDays = targetDays,
    completedDays = completedDays,
    startDate = startDate,
    lastUpdated = lastUpdated,
    completions = completions.map { it.toEntity() }
)

fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
    id = id,
    habitId = habitId,
    date = date,
    isCompleted = isCompleted
)

fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
    id = id,
    habitId = habitId,
    date = date,
    isCompleted = isCompleted
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


fun DbNoteComplete.toDomain(): NoteComplete = NoteComplete(
    note = note.toDomain(),
    checklistItems = checklistItems.map { it.toDomain() },
    tags = tags.map { it.toDomain() }
)

// List Mappings
fun List<TagEntity>.toTagDomainList(): List<Tag> = map { it.toDomain() }
fun List<DbNoteComplete>.toNoteCompleteDomainList(): List<NoteComplete> = map { it.toDomain() }












