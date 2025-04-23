package com.despicable.widgets.mapper

import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteContent
import com.despicable.widgets.model.WidgetChecklistItem
import com.despicable.widgets.model.WidgetNote
import com.despicable.widgets.ui.NotesTagsChecklist

fun WidgetChecklistItem.toDomainChecklist(noteId: Long): Checklist = Checklist(
    id = id,
    noteId = noteId,
    content = content,
    isChecked = isChecked,
    position = position
)


fun Checklist.toWidgetChecklistItem(): WidgetChecklistItem {
    return WidgetChecklistItem(
        id = id,
        content = content,
        isChecked = isChecked,
        position = position
    )
}


fun WidgetNote.toDomainNote(): Note = Note(
    id = id.toLongOrNull() ?: 0L,
    title = title,
    content = when (content) {
        is NoteContent.Text -> NoteContent.Text(content.text)
        is NoteContent.ChecklistItems -> NoteContent.ChecklistItems(
            content.items.map {
                Checklist(
                    id = it.id,
                    content = it.content,
                    isChecked = it.isChecked,
                    position = it.position,
                    noteId = it.noteId
                )
            }
        )
    },
    creationDate = creationDate,
    updateDate = lastUpdate,
    lightColor = color,
    reminderDate = reminderDate,
    noteType = noteType
)

// Extension function to convert NotesTagsChecklist to WidgetNote
fun NotesTagsChecklist.toWidgetNote(): WidgetNote {
    return WidgetNote(
        id = note.id.toString(),
        title = note.title,
        content = when (note.content) {
            is NoteContent.Text -> NoteContent.Text((note.content as NoteContent.Text).text)
            is NoteContent.ChecklistItems -> NoteContent.ChecklistItems(
                checklistItems.map { it.toDomainChecklist(note.id) }
            )
        },
        noteType = note.noteType,
        lastUpdate = note.updateDate,
        creationDate = note.creationDate,
        reminderDate = note.reminderDate,
        color = note.lightColor
    )
}





