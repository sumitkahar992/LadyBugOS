package com.despicable.widgets.mapper

import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.NoteContent
import com.despicable.core.model.NoteType
import com.despicable.widgets.model.WidgetChecklistItem
import com.despicable.widgets.model.WidgetContent
import com.despicable.widgets.model.WidgetNote
import kotlinx.datetime.Clock


fun Checklist.toWidgetChecklistItem() = WidgetChecklistItem(id, content, isChecked)


fun Note.toWidgetNote(): WidgetNote {
    val widgetContent = when (content) {
        is NoteContent.Text -> {
            val textContent = (content as NoteContent.Text).text
            WidgetContent.Text(textContent)
        }

        is NoteContent.ChecklistItems -> {
            val checklistItems = (content as NoteContent.ChecklistItems).items
                .map { it.toWidgetChecklistItem() }
            WidgetContent.Checklist(checklistItems)
        }
    }

    return WidgetNote(
        id = id,
        title = title,
        content = widgetContent,
        reminderDate = reminderDate,
        color = lightColor
    )
}

fun NoteComplete.toWidgetNote(): WidgetNote {
    val note = this.note

    // Map the content based on the note's content type
    val widgetContent = when (note.content) {
        is NoteContent.Text -> {
            val text = (note.content as NoteContent.Text).text
            WidgetContent.Text(text)
        }

        is NoteContent.ChecklistItems -> {
            // Use checklistItems from NoteComplete (not from Note.content)
            val items = this.checklistItems.map { item ->
                WidgetChecklistItem(
                    id = item.id,
                    content = item.content,
                    isChecked = item.isChecked
                )
            }
            WidgetContent.Checklist(items)
        }
    }

    return WidgetNote(
        id = note.id,
        title = note.title,
        content = widgetContent,
        reminderDate = note.reminderDate,
        color = note.lightColor
    )
}

fun WidgetNote.toNote(): Note {
    val now = Clock.System.now()

    return Note(
        id = id,
        title = title,
        content = when (content) {
            is WidgetContent.Text -> NoteContent.Text(content.text)
            is WidgetContent.Checklist -> NoteContent.ChecklistItems(
                items = content.items.mapIndexed { index, item ->
                    Checklist(
                        id = item.id,
                        noteId = id,
                        content = item.content,
                        isChecked = item.isChecked,
                        position = index // Assign position based on list order
                    )
                }
            )
        },
        creationDate = now,
        updateDate = now,
        lightColor = color,
        reminderDate = reminderDate,
        noteType = when (content) {
            is WidgetContent.Text -> NoteType.TEXT
            is WidgetContent.Checklist -> NoteType.CHECKLIST
        }
    )
}

fun List<WidgetNote>.toNotes(): List<Note> {
    return map { it.toNote() }
}

fun List<Note>.toWidgetNotes(): List<WidgetNote> {
    return map { it.toWidgetNote() }
}


/*

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
        color = note.lightColor,
        isDone = note.isDone
    )
}

*/


/*
fun WidgetChecklistItem.toDomainChecklist(noteId: Long): Checklist = Checklist(
    id = id,
    noteId = noteId,
    content = content,
    isChecked = isChecked,
    position = position
)
*/
