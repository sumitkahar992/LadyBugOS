package com.despicable.core.data.repository

import com.despicable.core.model.Checklist
import com.despicable.core.model.HabitItem
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    suspend fun getAllNotesData(): List<NoteComplete>
    suspend fun getAllTagsData(): List<Tag>

    // Note Operations
    fun getAllNotes(): Flow<List<Note>>
    fun getAllNotesWithTags(): Flow<List<NoteComplete>>
    fun getNoteWithTagsById(id: Long): Flow<NoteComplete?>
    suspend fun insertNoteWithTagsChecklist(
        note: Note,
        tagIds: List<Long>,
        checklistItems: List<Checklist>,
        habitItems: List<HabitItem>
    ): Long

    suspend fun updateNoteWithTagsChecklist(
        note: Note, tagIds: List<Long>, checklistItems: List<Checklist>,
        updateTimestamp: Boolean = true
    )

    suspend fun updateNotes(notes: List<Note>)
    suspend fun deleteNote(note: Note)
    suspend fun emptyTrashWithTags()


    // Tag Operations
    fun getAllTags(): Flow<List<Tag>>
    suspend fun insertTag(tag: Tag): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)

    // Reminder Operations
    suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?)
    suspend fun deleteReminder(noteId: Long)
    fun getUpcomingReminders(): Flow<List<NoteComplete>>
    fun getCompletedReminders(): Flow<List<NoteComplete>>


    suspend fun updateNoteChecklist(noteId: Long, isChecklist: Boolean)

    // widgets
    suspend fun getChecklistItem(noteId: Long, itemId: Long): Checklist?
    suspend fun toggleChecklistItem(noteId: Long, itemId: Long)

    // Checklist operations
    suspend fun insertChecklistItem(item: Checklist): Long
    fun getChecklistItemsByNoteId(noteId: Long): Flow<List<Checklist>>
    suspend fun updateChecklistItem(item: Checklist)
    suspend fun deleteChecklistItemsByNoteId(noteId: Long)
    suspend fun deleteChecklistItem(itemId: Long)

    suspend fun updateAllChecklistItems(items: List<Checklist>)


    // Notification Operations
    suspend fun updateNoteStatus(noteId: Long, isDone: Boolean)

    // method: 1
    fun searchNotes(query: String): Flow<List<NoteComplete>>
    fun getPinnedNotes(): Flow<List<NoteComplete>>
    fun getArchivedNotes(): Flow<List<NoteComplete>>
    fun getTrashedNotes(): Flow<List<NoteComplete>>
    fun getNoteById(id: Long): Flow<Note?>


    // method: 2
    fun getNoteCompleteById(id: Long): Flow<NoteComplete?>
    fun getArchivedNotesWithTagsAndChecklist(): Flow<List<NoteComplete>>
    fun getTrashedNotesWithTagsAndChecklist(): Flow<List<NoteComplete>>
    fun getUpcomingRemindersWithTagsAndChecklist(): Flow<List<NoteComplete>>
    fun getCompletedRemindersWithTagsAndChecklist(): Flow<List<NoteComplete>>


    // New atomic checklist operations
    /*    suspend fun addChecklistItem(noteId: Long, content: String, position: Int): Checklist
        suspend fun removeChecklistItem(noteId: Long, position: Int): List<Checklist>
        suspend fun toggleChecklistItem(noteId: Long, itemId: Long): Checklist
        suspend fun updateChecklistItemContent(noteId: Long, itemId: Long, content: String): Checklist
        suspend fun reorderChecklistItems(noteId: Long, fromPosition: Int, toPosition: Int): List<Checklist>


        suspend fun smartUpdateNoteWithTagsChecklist(
            note: Note,
            tagIds: List<Long>,
            checklistItems: List<Checklist>
        ): Boolean*/

}