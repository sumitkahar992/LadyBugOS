package com.example.ladybugos.repository

import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteDao
import com.example.ladybugos.model.NoteTagCrossRef
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.TagDao
import com.example.ladybugos.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val notificationHelper: NotificationHelper
) {

    fun getAllNotes() = noteDao.getAllNotes()
    fun getNoteById(id: Long) = noteDao.getNoteById(id)

    suspend fun insertNotes(notes: List<Note>) = noteDao.insertNotes(notes)


    suspend fun updateNotes(notes: List<Note>) = noteDao.updateNotes(notes)
//    fun getNoteById(id: Long): Flow<Note?> = noteDao.getNoteById(id)
    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)
    suspend fun emptyTrashWithTags() = noteDao.emptyTrashWithTags()

    // Tag-related operations
    fun getAllNotesWithTags() = noteDao.getAllNotesWithTags()
    fun getAllTags() = tagDao.getAllTags()
    fun getNoteWithTagsById(id: Long) = noteDao.getNoteWithTagsById(id)

    suspend fun insertNote(note: Note, tagIds: List<Long>): Long {
        val noteId = noteDao.insertNote(note)
        tagIds.forEach { tagId ->
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
        return noteId
    }

    suspend fun updateNote(note: Note, tagIds: List<Long>) {
        noteDao.updateNote(note)
        noteDao.deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(note.id, tagId))
        }
    }

    suspend fun insertTag(tag: Tag) = tagDao.insertTag(tag)


    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

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


}







































