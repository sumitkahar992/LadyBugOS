package com.example.ladybugos.repository

import com.example.ladybugos.model.ChecklistItem
import com.example.ladybugos.model.ChecklistItemDao
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteDao
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.TagDao
import com.example.ladybugos.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val notificationHelper: NotificationHelper,
    private val checklistItemDao: ChecklistItemDao
) {

    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { notes ->
            notes.sortedByDescending { it.updateDate }
        }
    }

    fun getNoteById(id: Long) = noteDao.getNoteById(id)

    suspend fun updateNotes(notes: List<Note>) = noteDao.updateNotes(notes)

    suspend fun emptyTrashWithTags() = noteDao.emptyTrashWithTags()

    // Tag-related operations
    /*
        fun getAllNotesWithTags() = noteDao.getAllNotesWithTags()
        fun getAllNotesWithTags(): Flow<List<NoteWithTags>> {
            return noteDao.getAllNotesWithTags().map { notesWithTags ->
                notesWithTags.sortedByDescending { it.note.id }
            }
        }

    fun getAllNotesWithTags(): Flow<List<NoteWithTags>> {
        return noteDao.getAllNotesWithTags().map { notesWithTags ->
            notesWithTags.sortedWith(
                compareByDescending<NoteWithTags> { it.note.isPinned }
                    .thenByDescending { it.note.id }
                    .thenByDescending { it.note.updateDate } // if you have a lastModified field
            )
        }
    }


    fun getAllNotesWithTags(): Flow<List<NoteWithTags>> {
        return noteDao.getAllNotesWithTags().map { notesWithTags ->
            notesWithTags.sortedWith(
                compareByDescending<NoteWithTags> { it.note.isPinned }
                    .thenByDescending { it.note.pinnedDate ?: 0L } // Sort pinned notes by pin date
                    .thenByDescending { it.note.id } // Then by ID for unpinned notes
            )
        }
    }
    */
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>> {
        return noteDao.getAllNotesWithTags().map { notesWithTags ->
            notesWithTags.groupBy { it.note.isPinned }
                .let { grouped ->
                    // Sort pinned notes by pinnedDate
                    (grouped[true]?.sortedByDescending { it.note.pinnedDate } ?: emptyList()) +
                            // Sort unpinned notes by ID
                            (grouped[false]?.sortedByDescending { it.note.id } ?: emptyList())
                }
        }
    }
    fun getAllTags() = tagDao.getAllTags()
    fun getNoteWithTagsById(id: Long) = noteDao.getNoteWithTagsById(id)


    suspend fun insertNoteWithTags(note: Note, tagIds: List<Long>): Long {
        return withContext(Dispatchers.IO) {
            noteDao.insertNoteWithTags(note, tagIds)
        }
    }

    suspend fun updateNoteWithTags(note: Note, tagIds: List<Long>) {
        withContext(Dispatchers.IO) {
            noteDao.updateNoteWithTags(note, tagIds)
        }
    }

    suspend fun insertTag(tag: Tag) = withContext(Dispatchers.IO) {
        tagDao.insertTag(tag)
    }

    suspend fun updateTag(tag: Tag) = withContext(Dispatchers.IO) {
        tagDao.updateTag(tag)
    }

    suspend fun deleteNote(note: Note) {
        // Delete both the note and associated tag cross-references
        noteDao.deleteNoteAndTag(note)
    }

    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTagAndCrossRefs(tag)
    }


    suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?) {

        val currentTime = System.currentTimeMillis()
        val isDone = reminderDate != null && reminderDate <= currentTime

        // Update reminder and reset isDone status
        noteDao.updateNoteReminderAndIsDone(noteId, reminderDate, isDone)

        val updatedNote = noteDao.getNoteById(noteId).first()
        updatedNote?.let {
            if (reminderDate != null) {
                notificationHelper.scheduleNotification(it)
            } else {
                notificationHelper.cancelNotification(noteId)
            }
        }
    }

    suspend fun deleteReminder(noteId: Long) {
        noteDao.deleteReminder(noteId)
    }

    // Checklist operations
    fun getChecklistItems(noteId: Long) = checklistItemDao.getChecklistItems(noteId)

    suspend fun insertChecklistItems(items: List<ChecklistItem>) =
        checklistItemDao.insertChecklistItems(items)

    suspend fun updateChecklistItem(item: ChecklistItem) =
        checklistItemDao.updateChecklistItem(item)

    suspend fun deleteCheckedItems(noteId: Long) =
        checklistItemDao.deleteCheckedItems(noteId)

    suspend fun reorderChecklistItems(noteId: Long, items: List<ChecklistItem>) {
        items.forEachIndexed { index, item ->
            checklistItemDao.updateChecklistItem(item.copy(position = index))
        }
    }

    suspend fun toggleNoteChecklist(note: Note, items: List<ChecklistItem>? = null) {
        noteDao.updateNote(note.copy(isChecklist = !note.isChecklist))
        if (items != null) {
            checklistItemDao.insertChecklistItems(items)
        }
    }

}




































