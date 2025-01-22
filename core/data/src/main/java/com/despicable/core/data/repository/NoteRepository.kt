package com.despicable.core.data.repository

import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    // Note Operations
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>
    fun getNoteWithTagsById(id: Long): Flow<NoteWithTags?>
    suspend fun insertNoteWithTags(note: Note, tagIds: List<Long>): Long
    suspend fun updateNoteWithTags(note: Note, tagIds: List<Long>)
    suspend fun updateNotes(notes: List<Note>)
    suspend fun deleteNote(note: Note)
    suspend fun emptyTrash()
    suspend fun emptyTrashWithTags()


    // Tag Operations
    fun getAllTags(): Flow<List<Tag>>
    suspend fun insertTag(tag: Tag): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)

    // Reminder Operations
    suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?)
    suspend fun deleteReminder(noteId: Long)
    fun getUpcomingReminders(): Flow<List<NoteWithTags>>
    fun getCompletedReminders(): Flow<List<NoteWithTags>>


    suspend fun updateNoteChecklist(noteId: Long, isChecklist: Boolean)


    // Checklist operations
    suspend fun insertChecklistItem(item: Checklist): Long
    fun getChecklistItemsByNoteId(noteId: Long): Flow<List<Checklist>>
    suspend fun updateChecklistItem(item: Checklist)
    suspend fun deleteChecklistItemsByNoteId(noteId: Long)
    suspend fun deleteChecklistItem(itemId: Long)

    suspend fun updateAllChecklistItems(items: List<Checklist>)


    // Notification Operations
    suspend fun updateNoteStatus(noteId: Long, isDone: Boolean)


}