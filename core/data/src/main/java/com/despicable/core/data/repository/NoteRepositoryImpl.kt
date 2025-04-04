package com.despicable.core.data.repository

import com.despicable.core.data.model.toDomain
import com.despicable.core.data.model.toEntity
import com.despicable.core.data.model.toNoteTagsDomainList
import com.despicable.core.data.model.toTagDomainList
import com.despicable.core.database.dao.ChecklistDao
import com.despicable.core.database.dao.NoteDao
import com.despicable.core.database.dao.TagDao
import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val checklistDao: ChecklistDao,
    private val reminderScheduler: ReminderScheduler
) : NoteRepository {

    // Note Operations
    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllActiveNotes().map { it.map { entity -> entity.toDomain() } }

    override fun getNoteById(id: Long): Flow<Note?> =
        noteDao.getNoteById(id).map { it?.toDomain() }

    override fun getAllNotesWithTags(): Flow<List<NoteWithTags>> =
        noteDao.getAllActiveNotesWithTags().map { it.toNoteTagsDomainList() }

    override fun getNoteWithTagsById(id: Long): Flow<NoteWithTags?> =
        noteDao.getNoteWithTagsById(id).map { it?.toDomain() }

    override fun getNoteCompleteById(id: Long): Flow<NoteComplete?> =
        noteDao.getNoteCompleteById(id).map { it?.toDomain() }

    override suspend fun insertNoteWithTagsChecklist(
        note: Note,
        tagIds: List<Long>,
        checklistItems: List<Checklist>
    ): Long = withContext(Dispatchers.IO) {

        // Always ensure a current timestamp when inserting
        val noteWithCurrentTime = note.copy(updateDate = System.currentTimeMillis())

        noteDao.insertNoteWithTagsAndChecklist(
            noteWithCurrentTime.toEntity(),
            tagIds,
            checklistItems.map { it.toEntity() },
            checklistDao
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
            note.copy(updateDate = System.currentTimeMillis())
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
            updatedNote?.let {
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

    override fun getUpcomingReminders(): Flow<List<NoteWithTags>> =
        noteDao.getUpcomingReminders().map { it.toNoteTagsDomainList() }

    override fun getCompletedReminders(): Flow<List<NoteWithTags>> =
        noteDao.getCompletedReminders().map { it.toNoteTagsDomainList() }

    // Checklist Operations
    override suspend fun updateNoteChecklist(noteId: Long, isChecklist: Boolean) =
        withContext(Dispatchers.IO) {
            checklistDao.updateNoteChecklist(noteId, isChecklist)
        }

    override suspend fun insertChecklistItem(item: Checklist): Long = withContext(Dispatchers.IO) {
        checklistDao.insertChecklistItem(item.toEntity())
    }

    override fun getChecklistItemsByNoteId(noteId: Long): Flow<List<Checklist>> =
        noteDao.getChecklistItemsByNoteId(noteId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun updateChecklistItem(item: Checklist) = withContext(Dispatchers.IO) {
        checklistDao.updateChecklistItem(item.toEntity())
    }

    override suspend fun deleteChecklistItemsByNoteId(noteId: Long) = withContext(Dispatchers.IO) {
        checklistDao.deleteAllChecklistItemsForNote(noteId)
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

    // Search Operations
    override fun searchNotes(query: String): Flow<List<NoteWithTags>> =
        noteDao.searchNotes(query).map { it.toNoteTagsDomainList() }

    // Filtered Note Operations
    override fun getPinnedNotes(): Flow<List<NoteWithTags>> =
        noteDao.getPinnedNotes().map { it.toNoteTagsDomainList() }

    override fun getArchivedNotes(): Flow<List<NoteWithTags>> =
        noteDao.getArchivedNotes().map { it.toNoteTagsDomainList() }

    override fun getTrashedNotes(): Flow<List<NoteWithTags>> =
        noteDao.getTrashedNotes().map { it.toNoteTagsDomainList() }

    override fun getArchivedNotesWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getArchivedNotes().mapLatest { notes ->
            coroutineScope {
                notes.map { noteWithTags ->
                    async {
                        val checklistItems = if (noteWithTags.note.isChecklist) {
                            try {
                                noteDao.getChecklistItemsByNoteId(noteWithTags.note.id).first()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } else emptyList()

                        NoteComplete(
                            note = noteWithTags.note.toDomain(),
                            tags = noteWithTags.tags.map { it.toDomain() },
                            checklistItems = checklistItems.map { it.toDomain() }
                        )
                    }
                }.awaitAll()
            }
        }

    override fun getTrashedNotesWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getTrashedNotes().flatMapLatest { notes ->
            val noteIds = notes.map { it.note.id }

            if (noteIds.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            noteDao.getChecklistItemsByNoteIds(noteIds).map { allChecklistItems ->
                // Group checklist items by noteId
                val checklistItemsByNoteId = allChecklistItems.groupBy { it.noteId }

                // Map notes with their checklist items
                notes.map { noteWithTags ->
                    val items = if (noteWithTags.note.isChecklist) {
                        checklistItemsByNoteId[noteWithTags.note.id] ?: emptyList()
                    } else emptyList()

                    NoteComplete(
                        note = noteWithTags.note.toDomain(),
                        tags = noteWithTags.tags.map { it.toDomain() },
                        checklistItems = items.map { it.toDomain() }
                    )
                }
            }
        }

    override fun getUpcomingRemindersWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getUpcomingReminders().flatMapLatest { notes ->
            if (notes.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            val noteIds = notes.map { it.note.id }

            // Combine with checklist items for all notes at once
            noteDao.getChecklistItemsByNoteIds(noteIds).map { allChecklistItems ->
                // Group checklist items by noteId for efficient lookup
                val checklistItemsByNoteId = allChecklistItems.groupBy { it.noteId }

                // Map each note to NoteComplete
                notes.map { noteWithTags ->
                    val checklistItems = if (noteWithTags.note.isChecklist) {
                        checklistItemsByNoteId[noteWithTags.note.id] ?: emptyList()
                    } else {
                        emptyList()
                    }

                    NoteComplete(
                        note = noteWithTags.note.toDomain(),
                        checklistItems = checklistItems.map { it.toDomain() },
                        tags = noteWithTags.tags.map { it.toDomain() }
                    )
                }
            }
        }

    override fun getCompletedRemindersWithTagsAndChecklist(): Flow<List<NoteComplete>> =
        noteDao.getCompletedReminders().flatMapLatest { notes ->
            if (notes.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            val noteIds = notes.map { it.note.id }

            // Combine with checklist items for all notes at once
            noteDao.getChecklistItemsByNoteIds(noteIds).map { allChecklistItems ->
                // Group checklist items by noteId for efficient lookup
                val checklistItemsByNoteId = allChecklistItems.groupBy { it.noteId }

                // Map each note to NoteComplete
                notes.map { noteWithTags ->
                    val checklistItems = if (noteWithTags.note.isChecklist) {
                        checklistItemsByNoteId[noteWithTags.note.id] ?: emptyList()
                    } else {
                        emptyList()
                    }

                    NoteComplete(
                        note = noteWithTags.note.toDomain(),
                        checklistItems = checklistItems.map { it.toDomain() },
                        tags = noteWithTags.tags.map { it.toDomain() }
                    )
                }
            }
        }

    /*    override suspend fun addChecklistItem(
            noteId: Long,
            content: String,
            position: Int
        ): Checklist = withContext(Dispatchers.IO) {
            withMutexTimeout(checklistMutex) {
                // Create new item
                val newItem = Checklist(
                    noteId = noteId,
                    content = content,
                    position = position
                )

                // Get existing items
                val existingItems = getChecklistItemsByNoteId(noteId).firstOrNull() ?: emptyList()

                // Shift positions of existing items
                for (i in existingItems.indices.reversed()) {
                    if (i >= position) {
                        val item = existingItems[i]
                        updateChecklistItem(item.copy(position = i + 1))
                    }
                }

                // Insert new item
                val insertedId = insertChecklistItem(newItem)
                return@withMutexTimeout newItem.copy(id = insertedId)
            }
        }

        override suspend fun reorderChecklistItems(
            noteId: Long,
            fromPosition: Int,
            toPosition: Int
        ): List<Checklist> = withContext(Dispatchers.IO) {
            withMutexTimeout(checklistMutex) {
                val items = getChecklistItemsByNoteId(noteId).firstOrNull() ?: return@withMutexTimeout emptyList()

                if (fromPosition >= items.size || toPosition >= items.size) {
                    return@withMutexTimeout items
                }

                val mutableItems = items.toMutableList()
                val item = mutableItems.removeAt(fromPosition)
                mutableItems.add(toPosition, item)

                // Update only affected positions for better performance
                val startIdx = minOf(fromPosition, toPosition)
                val endIdx = maxOf(fromPosition, toPosition)

                for (i in startIdx..endIdx) {
                    if (mutableItems[i].position != i) {
                        updateChecklistItem(mutableItems[i].copy(position = i))
                    }
                }

                return@withMutexTimeout mutableItems
            }
        }

        override suspend fun toggleChecklistItem(
            noteId: Long,
            itemId: Long
        ): Checklist = withContext(Dispatchers.IO) {
            withMutexTimeout(checklistMutex) {
                val items = getChecklistItemsByNoteId(noteId).firstOrNull() ?: emptyList()
                val item = items.find { it.id == itemId } ?: throw IllegalArgumentException("Item not found")

                val updatedItem = item.copy(isChecked = !item.isChecked)
                updateChecklistItem(updatedItem)

                return@withMutexTimeout updatedItem
            }
        }

        override suspend fun removeChecklistItem(
            noteId: Long,
            position: Int
        ): List<Checklist> = withContext(Dispatchers.IO) {
            withMutexTimeout(checklistMutex) {
                val items = getChecklistItemsByNoteId(noteId).firstOrNull() ?: return@withMutexTimeout emptyList()

                if (position >= items.size) return@withMutexTimeout items

                val itemToRemove = items[position]
                deleteChecklistItem(itemToRemove.id)

                // Update positions for remaining items
                val remainingItems = items.filterIndexed { i, _ -> i != position }
                    .mapIndexed { index, item ->
                        if (item.position != index) {
                            updateChecklistItem(item.copy(position = index))
                            item.copy(position = index)
                        } else item
                    }

                return@withMutexTimeout remainingItems
            }
        }

        override suspend fun updateChecklistItemContent(
            noteId: Long,
            itemId: Long,
            content: String
        ): Checklist = withContext(Dispatchers.IO) {
            withMutexTimeout(checklistMutex) {
                val items = getChecklistItemsByNoteId(noteId).firstOrNull() ?: emptyList()
                val item = items.find { it.id == itemId } ?: throw IllegalArgumentException("Item not found")

                val updatedItem = item.copy(content = content)
                updateChecklistItem(updatedItem)

                return@withMutexTimeout updatedItem
            }
        }


        // Add a smart note update method that handles change detection
        override suspend fun smartUpdateNoteWithTagsChecklist(
            note: Note,
            tagIds: List<Long>,
            checklistItems: List<Checklist>
        ): Boolean = withContext(Dispatchers.IO) {
            withMutexTimeout(noteMutex) {
                // Check if there are actual changes
                val originalNote = getNoteById(note.id).firstOrNull()
                val hasChanges = originalNote?.let { original ->
                    note.title != original.title ||
                            note.content != original.content ||
                            note.isChecklist != original.isChecklist ||
                            note.lightColor != original.lightColor
                } ?: true // If note doesn't exist yet, treat as changed

                if (hasChanges) {
                    // Update with timestamp change
                    updateNoteWithTagsChecklist(
                        note.copy(updateDate = System.currentTimeMillis()),
                        tagIds,
                        checklistItems,
                        true
                    )
                    true
                } else {
                    // No changes, just update without changing timestamp
                    updateNoteWithTagsChecklist(note, tagIds, checklistItems, false)
                    false
                }
            }
        }*/


}


/*

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val checklistDao: ChecklistItemDao,
    private val reminderScheduler: ReminderScheduler
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { it.toNoteDomainList() }

    override fun getNoteById(id: Long): Flow<Note?> =
        noteDao.getNoteById(id).map { it.toDomainOrNull() }

    override fun getAllNotesWithTags(): Flow<List<NoteWithTags>> =
        noteDao.getAllNotesWithTags().map { it.toNoteTagsDomainList() }

    override fun getNoteWithTagsById(id: Long): Flow<NoteWithTags?> =
        noteDao.getNoteWithTagsById(id).map { it.toDomainOrNull() }

    override suspend fun insertNoteWithTagsChecklist(
        note: Note,
        tagIds: List<Long>,
        checklistItems: List<Checklist>
    ): Long =
        withContext(Dispatchers.IO) {
            noteDao.insertNoteWithTags(note.toEntity(), tagIds, checklistItems.toEntityList())
        }

    override suspend fun updateNoteWithTagsChecklist(
        note: Note,
        tagIds: List<Long>,
        checklistItems: List<Checklist>
    ) =
        withContext(Dispatchers.IO) {
            noteDao.updateNoteWithTagsAndChecklist(
                note.toEntity(),
                tagIds,
                checklistItems.toEntityList()
            )
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

            // Update reminder and reset isDone status
            noteDao.updateNoteReminderAndIsDone(noteId, reminderDate, isDone)

            val updatedNote = noteDao.getNoteById(noteId).first()
            updatedNote?.let {
                if (reminderDate != null) {
                    reminderScheduler.scheduleReminder(it.toDomain())
                } else {
                    reminderScheduler.cancelReminder(noteId)
                }
            }
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

    override suspend fun updateNoteChecklist(noteId: Long, isChecklist: Boolean) =
        withContext(Dispatchers.IO) {
            noteDao.updateNoteChecklist(noteId, isChecklist)
        }

    // Checklist operations
    override suspend fun insertChecklistItem(item: Checklist): Long =
        checklistDao.insertChecklistItem(item.toEntity())

    override fun getChecklistItemsByNoteId(noteId: Long): Flow<List<Checklist>> =
        checklistDao.getChecklistItemsByNoteId(noteId).map { it.toDomainList() }

    override suspend fun updateChecklistItem(item: Checklist) =
        checklistDao.updateChecklistItem(item.toEntity())

    override suspend fun deleteChecklistItemsByNoteId(noteId: Long) =
        checklistDao.deleteChecklistItemsByNoteId(noteId)

    override suspend fun deleteChecklistItem(itemId: Long) =
        checklistDao.deleteChecklistItem(itemId)

    override suspend fun updateAllChecklistItems(items: List<Checklist>) {
        checklistDao.updateChecklistItems(items.toEntityList())
    }

    // Checklist Operations
    */
/*    override fun getChecklistItems(noteId: Long) =
            checklistItemDao.getChecklistItems(noteId)

        override suspend fun insertChecklistItems(items: List<ChecklistEntity>) =
            withContext(Dispatchers.IO) {
                checklistItemDao.insertChecklistItems(items)
            }

        override suspend fun updateChecklistItem(item: ChecklistEntity) =
            withContext(Dispatchers.IO) {
                checklistItemDao.updateChecklistItem(item)
            }

        override suspend fun deleteCheckedItems(noteId: Long) =
            withContext(Dispatchers.IO) {
                checklistItemDao.deleteCheckedItems(noteId)
            }

        override suspend fun reorderChecklistItems(noteId: Long, items: List<ChecklistEntity>) =
            withContext(Dispatchers.IO) {
                items.forEachIndexed { index, item ->
                    checklistItemDao.updateChecklistItem(item.copy(position = index))
                }
            }

        override suspend fun toggleNoteChecklist(note: Note, items: List<ChecklistEntity>?): Unit =
            withContext(Dispatchers.IO) {
                noteDao.updateNote(note.toEntity().copy(isChecklist = !note.isChecklist))
                items?.let { checklistItemDao.insertChecklistItems(it) }
            }*//*


    override suspend fun updateNoteStatus(noteId: Long, isDone: Boolean) {
        noteDao.updateNoteDoneStatus(noteId, isDone)
    }
}


*/


































