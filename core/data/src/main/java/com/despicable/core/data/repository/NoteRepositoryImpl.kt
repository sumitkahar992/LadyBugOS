package com.despicable.core.data.repository

import com.despicable.core.data.model.toDomain
import com.despicable.core.data.model.toEntity
import com.despicable.core.data.model.toNoteCompleteDomainList
import com.despicable.core.data.model.toTagDomainList
import com.despicable.core.database.dao.ChecklistDao
import com.despicable.core.database.dao.HabitDao
import com.despicable.core.database.dao.NoteDao
import com.despicable.core.database.dao.TagDao
import com.despicable.core.model.Checklist
import com.despicable.core.model.HabitItem
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val checklistDao: ChecklistDao,
    private val habitDao: HabitDao,
    private val reminderScheduler: ReminderScheduler
) : NoteRepository {

    // Note Operations
    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllActiveNotes().map { it.map { entity -> entity.toDomain() } }

    override fun getNoteById(id: Long): Flow<Note> =
        noteDao.getNoteById(id).map { it.toDomain() }


    override fun getNoteCompleteById(id: Long): Flow<NoteComplete?> =
        noteDao.getNoteCompleteById(id).map { it?.toDomain() }

    override suspend fun insertNoteWithTagsChecklist(
        note: Note,
        tagIds: List<Long>,
        checklistItems: List<Checklist>,
        habitItems: List<HabitItem>
    ): Long = withContext(Dispatchers.IO) {

        // Always ensure a current timestamp when inserting
        val noteWithCurrentTime = note.copy(
            creationDate = Clock.System.now(),
            updateDate = Clock.System.now()
        )

        noteDao.insertNoteWithTagsAndChecklist(
            note = noteWithCurrentTime.toEntity(),
            tagIds = tagIds,
            checklistItems = checklistItems.map { it.toEntity() },
            habitItems = habitItems.map { it.toEntity() },
            checklistDao = checklistDao,
            habitDao = habitDao

        )
    }

    override suspend fun updateNoteWithTagsChecklist(
        note: Note,
        tagIds: List<Long>,
        checklistItems: List<Checklist>,
        updateTimestamp: Boolean
    ) = withContext(Dispatchers.IO) {

        // Only update timestamp when appropriate
        val noteToSave = if (updateTimestamp) {
            note.copy(updateDate = Clock.System.now())
        } else {
            note
        }

        noteDao.updateNoteWithTagsAndChecklist(
            noteToSave.toEntity(),
            tagIds,
            checklistItems.map { it.toEntity() },
            checklistDao
        )
    }

    override suspend fun updateNotes(notes: List<Note>) = withContext(Dispatchers.IO) {
        noteDao.updateNotes(notes.map { it.toEntity() })
    }

    override suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteCompletely(note.id)
    }


    override suspend fun emptyTrashWithTags() = withContext(Dispatchers.IO) {
        noteDao.emptyTrash() // The cascade delete will handle tag cross-references
    }

    // Tag Operations
    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { it.toTagDomainList() }

    override fun getAllNotesWithTags(): Flow<List<NoteComplete>> =
        noteDao.getAllActiveNotesWithTags().map { it.toNoteCompleteDomainList() }

    override fun getChecklistItemsByNoteId(noteId: Long): Flow<List<Checklist>> =
        noteDao.getChecklistItemsByNoteId(noteId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertTag(tag: Tag): Long = withContext(Dispatchers.IO) {
        tagDao.insertTag(tag.toEntity())
    }

    override suspend fun updateTag(tag: Tag) = withContext(Dispatchers.IO) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun deleteTag(tag: Tag) = withContext(Dispatchers.IO) {
        tagDao.deleteTagAndCrossRefs(tag.toEntity())
    }

    // Reminder Operations
    override suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val isDone = reminderDate != null && reminderDate <= currentTime

            // Update reminder and reset isDone status
            noteDao.updateNoteReminderAndIsDone(noteId, reminderDate, isDone)

            val updatedNote = noteDao.getNoteById(noteId).first()
            updatedNote.let {
                if (reminderDate != null && reminderDate > currentTime) {
                    reminderScheduler.scheduleReminder(it.toDomain())
                } else {
                    reminderScheduler.cancelReminder(noteId)
                }
            }
        }
    }

    override suspend fun deleteReminder(noteId: Long) = withContext(Dispatchers.IO) {
        noteDao.updateNoteReminder(noteId, null)
        reminderScheduler.cancelReminder(noteId)
    }

    override fun getUpcomingReminders(): Flow<List<NoteComplete>> =
        noteDao.getUpcomingReminders().map { it.toNoteCompleteDomainList() }

    override fun getCompletedReminders(): Flow<List<NoteComplete>> =
        noteDao.getCompletedReminders().map { it.toNoteCompleteDomainList() }


    override suspend fun toggleChecklistItem(noteId: Long, itemId: Long): Boolean {
        val rowsAffected = checklistDao.toggleChecklistItem(noteId, itemId)
        return rowsAffected > 0
    }


    override suspend fun insertChecklistItem(item: Checklist): Long = withContext(Dispatchers.IO) {
        checklistDao.insertChecklistItem(item.toEntity())
    }


    override suspend fun updateChecklistItem(item: Checklist) = withContext(Dispatchers.IO) {
        checklistDao.updateChecklistItem(item.toEntity())
    }


    override suspend fun deleteChecklistItem(itemId: Long) = withContext(Dispatchers.IO) {
        checklistDao.deleteChecklistItem(itemId)
    }

    override suspend fun updateAllChecklistItems(items: List<Checklist>) =
        withContext(Dispatchers.IO) {
            checklistDao.updateChecklistItems(items.map { it.toEntity() })
        }

    // Status Operations
    override suspend fun updateNoteStatus(noteId: Long, isDone: Boolean) =
        withContext(Dispatchers.IO) {
            noteDao.updateNoteStatus(noteId, isDone)
        }


    override fun getArchivedNotesWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getArchivedNotes().map {
            it.toNoteCompleteDomainList()
        }

    override fun getTrashedNotesWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getTrashedNotes().flatMapLatest { notes ->
            val noteIds = notes.map { it.note.id }

            if (noteIds.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            flowOf(notes.toNoteCompleteDomainList())

        }


    override fun getUpcomingRemindersWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getUpcomingReminders().flatMapLatest { notes ->
            if (notes.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            flowOf(notes.toNoteCompleteDomainList())
        }

    override fun getCompletedRemindersWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getCompletedReminders().flatMapLatest { notes ->
            if (notes.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            flowOf(notes.toNoteCompleteDomainList())
        }

}
































