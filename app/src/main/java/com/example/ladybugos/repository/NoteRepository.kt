package com.example.ladybugos.repository

import androidx.compose.ui.graphics.toArgb
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteDao
import com.example.ladybugos.model.NoteTagCrossRef
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.TagDao
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val notificationHelper: NotificationHelper
) {

    val getAllNotes: Flow<List<Note>> = noteDao.getAllNotes()


    suspend fun updateNotes(notes: List<Note>) = noteDao.updateNotes(notes)
    suspend fun delete(note: Note) = noteDao.deleteNote(note)
    fun getNoteById(id: Long): Flow<Note?> = noteDao.getNoteById(id)
    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)
    suspend fun emptyTrash() = noteDao.emptyTrash()

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
        noteDao.deleteNoteAndCrossRefs(note)
    }

    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTagAndCrossRefs(tag)
    }


    private fun getRandomColor(): Int = colorPalette.random().toArgb()

    // Reminder
    suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        noteDao.updateNoteReminder(noteId, reminderDate)
    }

    fun getUpcomingReminders(): Flow<List<Note>> {
        return noteDao.getUpcomingReminders(System.currentTimeMillis())
    }


    // Notification
    fun scheduleReminder(note: Note) {
        notificationHelper.scheduleNotification(note)
    }

    fun cancelReminder(noteId: Long) {
        notificationHelper.cancelNotification(noteId)
    }

}







































