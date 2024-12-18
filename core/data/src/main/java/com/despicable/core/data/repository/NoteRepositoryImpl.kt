package com.despicable.core.data.repository

import com.despicable.core.data.model.toDomainOrNull
import com.despicable.core.data.model.toEntity
import com.despicable.core.data.model.toNoteDomainList
import com.despicable.core.data.model.toNoteEntityList
import com.despicable.core.data.model.toNoteTagsDomainList
import com.despicable.core.data.model.toTagDomainList
import com.despicable.core.database.dao.NoteDao
import com.despicable.core.database.dao.TagDao
import com.despicable.core.database.model.ChecklistItem
import com.despicable.core.database.model.ChecklistItemDao
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val checklistItemDao: ChecklistItemDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { it.toNoteDomainList() }

    override fun getNoteById(id: Long): Flow<Note?> =
        noteDao.getNoteById(id).map { it.toDomainOrNull() }

    override fun getAllNotesWithTags(): Flow<List<NoteWithTags>> =
        noteDao.getAllNotesWithTags().map { it.toNoteTagsDomainList() }

    override fun getNoteWithTagsById(id: Long): Flow<NoteWithTags?> =
        noteDao.getNoteWithTagsById(id).map { it.toDomainOrNull() }

    override suspend fun insertNoteWithTags(note: Note, tagIds: List<Long>): Long =
        withContext(Dispatchers.IO) {
            noteDao.insertNoteWithTags(note.toEntity(), tagIds)
        }

    override suspend fun updateNoteWithTags(note: Note, tagIds: List<Long>) =
        withContext(Dispatchers.IO) {
            noteDao.updateNoteWithTags(note.toEntity(), tagIds)
        }

    override suspend fun updateNotes(notes: List<Note>) =
        withContext(Dispatchers.IO) {
            noteDao.updateNotes(notes.toNoteEntityList())
        }

    override suspend fun deleteNote(note: Note) =
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteAndTag(note.toEntity())
        }

    override suspend fun emptyTrash() =
        withContext(Dispatchers.IO) {
            noteDao.emptyTrashWithTags()
        }

    override suspend fun emptyTrashWithTags() = noteDao.emptyTrashWithTags()

    // Tag Operations
    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { it.toTagDomainList() }

    override suspend fun insertTag(tag: Tag): Long =
        withContext(Dispatchers.IO) {
            tagDao.insertTag(tag.toEntity())
        }

    override suspend fun updateTag(tag: Tag) =
        withContext(Dispatchers.IO) {
            tagDao.updateTag(tag.toEntity())
        }

    override suspend fun deleteTag(tag: Tag) =
        withContext(Dispatchers.IO) {
            tagDao.deleteTagAndCrossRefs(tag.toEntity())
        }

    // Reminder Operations
    override suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val isDone = reminderDate != null && reminderDate <= currentTime
            noteDao.updateNoteReminderAndIsDone(noteId, reminderDate, isDone)
        }
    }

    override suspend fun deleteReminder(noteId: Long) =
        withContext(Dispatchers.IO) {
            noteDao.deleteReminder(noteId)
        }

    override fun getUpcomingReminders(): Flow<List<NoteWithTags>> =
        noteDao.getUpcomingReminders(System.currentTimeMillis()).map { it.toNoteTagsDomainList() }

    override fun getCompletedReminders(): Flow<List<NoteWithTags>> =
        noteDao.getCompletedReminders(System.currentTimeMillis()).map { it.toNoteTagsDomainList() }

    // Checklist Operations
    override fun getChecklistItems(noteId: Long) =
        checklistItemDao.getChecklistItems(noteId)

    override suspend fun insertChecklistItems(items: List<ChecklistItem>) =
        withContext(Dispatchers.IO) {
            checklistItemDao.insertChecklistItems(items)
        }

    override suspend fun updateChecklistItem(item: ChecklistItem) =
        withContext(Dispatchers.IO) {
            checklistItemDao.updateChecklistItem(item)
        }

    override suspend fun deleteCheckedItems(noteId: Long) =
        withContext(Dispatchers.IO) {
            checklistItemDao.deleteCheckedItems(noteId)
        }

    override suspend fun reorderChecklistItems(noteId: Long, items: List<ChecklistItem>) =
        withContext(Dispatchers.IO) {
            items.forEachIndexed { index, item ->
                checklistItemDao.updateChecklistItem(item.copy(position = index))
            }
        }

    override suspend fun toggleNoteChecklist(note: Note, items: List<ChecklistItem>?): Unit =
        withContext(Dispatchers.IO) {
            noteDao.updateNote(note.toEntity().copy(isChecklist = !note.isChecklist))
            items?.let { checklistItemDao.insertChecklistItems(it) }
        }
}




































