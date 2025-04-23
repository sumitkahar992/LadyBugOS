package com.despicable.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.withTransaction
import com.despicable.core.database.NoteDatabase
import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.HabitEntity
import com.despicable.core.database.model.NoteCompleteEntity
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.NoteTagRefEntity
import com.despicable.core.database.model.NoteWithTagsEntity
import com.despicable.core.database.model.TagEntity
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.NoteType
import com.despicable.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first


@Dao
interface NoteDao {


    // Fetch all notes with their checklist items
    @Transaction
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesData(): List<NoteCompleteEntity>

    // Fetch all tags
    @Query("SELECT * FROM tags")
    suspend fun getAllTagsData(): List<TagEntity>


    // Basic Note Operations
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updateDate DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Update
    suspend fun updateNotes(notes: List<NoteEntity>)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Relationship Queries
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updateDate DESC")
    fun getAllActiveNotesWithTags(): Flow<List<NoteCompleteEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteWithTagsById(id: Long): Flow<NoteCompleteEntity?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteCompleteById(id: Long): Flow<NoteCompleteEntity?>

    // Filtered Queries
    @Transaction
    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isArchived = 0 AND isTrashed = 0 ORDER BY pinnedDate DESC")
    fun getPinnedNotes(): Flow<List<NoteCompleteEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updateDate DESC")
    fun getArchivedNotes(): Flow<List<NoteCompleteEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updateDate DESC")
    fun getTrashedNotes(): Flow<List<NoteCompleteEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE reminderDate IS NOT NULL AND reminderDate > :currentTime AND isDone = 0 AND isTrashed = 0 ORDER BY reminderDate ASC")
    fun getUpcomingReminders(currentTime: Long = System.currentTimeMillis()): Flow<List<NoteCompleteEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE reminderDate IS NOT NULL AND isDone = 1 AND isTrashed = 0 ORDER BY reminderDate DESC")
    fun getCompletedReminders(): Flow<List<NoteCompleteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<NoteTagRefEntity>)

    // Composite Operations
    @Transaction
    suspend fun insertNoteWithTagsAndChecklist(
        note: NoteEntity,
        tagIds: List<Long>,
        checklistItems: List<ChecklistEntity> = emptyList(),
        habitItems: List<HabitEntity> = emptyList(),
        checklistDao: ChecklistDao,
        habitDao: HabitDao
    ): Long {
        // Insert the base note
        val noteId = insertNote(note)

        // Insert tag cross-references
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(noteId, tagId))
        }

        // Insert checklist items  &  habit items
        when(note.noteType) {
            NoteType.TEXT -> {}
            NoteType.CHECKLIST -> insertChecklistItems(noteId, checklistItems, checklistDao)
        }


        checklistItems.forEachIndexed { index, item ->
            checklistDao.insertChecklistItem(item.copy(noteId = noteId, position = index))
        }

        return noteId
    }


    /**
     * Helper to insert checklist items
     */
    private suspend fun insertChecklistItems(
        noteId: Long,
        checklistItems: List<ChecklistEntity>,
        checklistDao: ChecklistDao
    ) {
        checklistItems.forEachIndexed { index, item ->
            checklistDao.insertChecklistItem(item.copy(noteId = noteId, position = index))
        }
    }

    /**
     * Helper to insert habit tracker items
     */
    private suspend fun insertHabitItems(
        noteId: Long,
        habitItems: List<HabitEntity>,
        habitDao: HabitDao
    ) {
        habitItems.forEachIndexed { index, item ->
            habitDao.insertHabitItem(item.copy(noteId = noteId))
        }
    }

    @Transaction
    suspend fun updateNoteWithTagsAndChecklist(
        note: NoteEntity,
        tagIds: List<Long>,
        checklistItems: List<ChecklistEntity>,
        checklistDao: ChecklistDao
    ) {
        updateNote(note)

        // Update tags
        deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(note.id, tagId))
        }

        // Update checklist items
        checklistDao.deleteAllChecklistItemsForNote(note.id)
        checklistItems.forEachIndexed { index, item ->
            checklistDao.insertChecklistItem(item.copy(noteId = note.id, position = index))
        }
    }

    @Transaction
    suspend fun deleteNoteCompletely(noteId: Long) {
        // Due to CASCADE delete, this will delete related checklist items and tag cross-references
        deleteNoteById(noteId)
    }

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // Tag Cross-Reference Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagRefEntity)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)



    // Status Updates
    @Query("UPDATE notes SET isDone = :isDone WHERE id = :noteId")
    suspend fun updateNoteStatus(noteId: Long, isDone: Boolean)

    @Query("UPDATE notes SET reminderDate = :reminderDate WHERE id = :noteId")
    suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?)

    // Search
    @Transaction
    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') AND isTrashed = 0 ORDER BY updateDate DESC")
    fun searchNotes(query: String): Flow<List<NoteCompleteEntity>>


    // Add this if it doesn't exist
    @Query("UPDATE notes SET reminderDate = :reminderDate, isDone = :isDone WHERE id = :noteId")
    suspend fun updateNoteReminderAndIsDone(noteId: Long, reminderDate: Long?, isDone: Boolean)

    // Add this if it doesn't exist
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position ASC")
    fun getChecklistItemsByNoteId(noteId: Long): Flow<List<ChecklistEntity>>

    // Add this method
    @Query("SELECT * FROM checklist_items WHERE noteId IN (:noteIds)")
    fun getChecklistItemsByNoteIds(noteIds: List<Long>): Flow<List<ChecklistEntity>>

    // BACK UP

    @Query("SELECT * FROM notes")
    fun getAllNotesForBackup(): Flow<List<NoteEntity>>

    @Transaction
    suspend fun mergeBackupData(
        notes: List<NoteEntity>,
        tags: List<TagEntity>,
        crossRefs: List<NoteTagRefEntity>,
        database: NoteDatabase
    ) {
        database.withTransaction {
            // Process tags first to establish tag ID mapping
            val tagIdMapping = processTags(tags, database)

            // Process notes to establish note ID mapping
            val existingNotes = getAllNotesForBackup().first() // Use getAllNotesForBackup instead
            val existingNotesMap = existingNotes.associateBy {
                "${it.title}${it.content}${it.updateDate}"
            }
            val noteIdMapping = processNotes(notes, existingNotesMap)

            // Process cross-references using the mappings
            processCrossRefs(crossRefs, noteIdMapping, tagIdMapping)
        }
    }

    private suspend fun processTags(
        backupTags: List<TagEntity>, database: NoteDatabase
    ): Map<Long, Long> {
        val tagIdMapping = mutableMapOf<Long, Long>()
        val existingTags = database.tagDao().getAllTags().first()
        val existingTagsMap = existingTags.associateBy { it.name }

        backupTags.forEach { backupTag ->
            val existingTag = existingTagsMap[backupTag.name]

            if (existingTag != null) {
                // Tag already exists, map the backup ID to existing ID
                tagIdMapping[backupTag.id] = existingTag.id
            } else {
                // Insert new tag
                val newTagId = database.tagDao().insertTag(backupTag.copy(id = 0))
                tagIdMapping[backupTag.id] = newTagId
            }
        }

        return tagIdMapping
    }

    private suspend fun processNotes(
        backupNotes: List<NoteEntity>,
        existingNotesMap: Map<String, NoteEntity>
    ): Map<Long, Long> {
        val noteIdMapping = mutableMapOf<Long, Long>()

        backupNotes.forEach { backupNote ->
            val noteKey = "${backupNote.title}${backupNote.content}${backupNote.updateDate}"
            val existingNote = existingNotesMap[noteKey]

            if (existingNote != null) {
                noteIdMapping[backupNote.id] = existingNote.id
                // Update if other properties changed
                if (hasNotePropertiesChanged(existingNote, backupNote)) {
                    updateNote(
                        existingNote.copy(
                            lightColor = backupNote.lightColor,
                            isPinned = backupNote.isPinned,
                            isArchived = backupNote.isArchived,
                            isTrashed = backupNote.isTrashed,
                            reminderDate = backupNote.reminderDate,
                            isDone = backupNote.isDone,
//                            isChecklist = backupNote.isChecklist
                        )
                    )
                }
            } else {
                // Insert new note
                val newNoteId = insertNote(backupNote.copy(id = 0))
                noteIdMapping[backupNote.id] = newNoteId
            }
        }

        return noteIdMapping
    }

    private suspend fun processCrossRefs(
        backupCrossRefs: List<NoteTagRefEntity>,
        noteIdMapping: Map<Long, Long>,
        tagIdMapping: Map<Long, Long>
    ) {
        val processedCrossRefs = backupCrossRefs.mapNotNull { crossRef ->
            val newNoteId = noteIdMapping[crossRef.noteId] ?: return@mapNotNull null
            val newTagId = tagIdMapping[crossRef.tagId] ?: return@mapNotNull null
            NoteTagRefEntity(
                noteId = newNoteId,
                tagId = newTagId
            )
        }

        // Insert all cross-references in a batch
        if (processedCrossRefs.isNotEmpty()) {
            insertCrossRefs(processedCrossRefs)
        }
    }

    private fun hasNotePropertiesChanged(
        existingNote: NoteEntity,
        backupNote: NoteEntity
    ): Boolean {
        return existingNote.lightColor != backupNote.lightColor ||
                existingNote.isPinned != backupNote.isPinned ||
                existingNote.isArchived != backupNote.isArchived ||
                existingNote.isTrashed != backupNote.isTrashed ||
                existingNote.reminderDate != backupNote.reminderDate ||
                existingNote.isDone != backupNote.isDone
//                existingNote.isChecklist != backupNote.isChecklist
    }



    @Query("SELECT * FROM note_tag_cross_ref")
    fun getAllCrossRefs(): Flow<List<NoteTagRefEntity>>


}


/*

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Transaction // Add this for nested operations
    suspend fun insertNoteWithTags(
        note: NoteEntity,
        tagIds: List<Long>,
        checklistItems: List<ChecklistEntity>
    ): Long {
        val noteId = insertNote(note)

        // Insert tags
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(noteId, tagId))
        }

        // Insert checklist items with correct note ID
        checklistItems.forEachIndexed { index, item ->
            insertChecklistItem(
                item.copy(
                    noteId = noteId,
                    position = index
                )
            )
        }

        return noteId
    }

    @Transaction // Add this for nested operations
    suspend fun updateNoteWithTags(note: NoteEntity, tagIds: List<Long>) {
        updateNote(note)
        deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(note.id, tagId))
        }
    }

    // CheckLists
    @Transaction
    suspend fun updateNoteWithTagsAndChecklist(
        note: NoteEntity,
        tagIds: List<Long>,
        checklistItems: List<ChecklistEntity>
    ) {
        updateNote(note)

        // Update tags
        deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(note.id, tagId))
        }

        // Update checklist items
        deleteAllChecklistItemsForNote(note.id)
        checklistItems.forEachIndexed { index, item ->
            insertChecklistItem(
                item.copy(
                    noteId = note.id,
                    position = index
                )
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(checklistItem: ChecklistEntity)

    @Update
    suspend fun updateChecklistItem(checklistItem: ChecklistEntity)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteAllChecklistItemsForNote(noteId: Long)
    //

    @Update
    suspend fun updateNotes(notes: List<NoteEntity>)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<NoteEntity?>


    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // TAGS
    @Transaction
    @Query("SELECT * FROM notes")
    fun getAllNotesWithTags(): Flow<List<NoteWithTagsEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteWithTagsById(id: Long): Flow<NoteWithTagsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagRefEntity)


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTagCrossRefs(noteId: Long)

    @Transaction
    suspend fun deleteNoteAndTag(note: NoteEntity) {
        deleteNoteTagCrossRefs(note.id)
        deleteNote(note)
    }


    @Query("UPDATE notes SET isDone = :isDone WHERE id = :noteId")
    suspend fun updateNoteDoneStatus(noteId: Long, isDone: Boolean)


    // Update NoteDao with a new query to reset isDone along with reminder
    @Query("UPDATE notes SET reminderDate = :reminderDate, isDone = :isDone WHERE id = :noteId")
    suspend fun updateNoteReminderAndIsDone(noteId: Long, reminderDate: Long?, isDone: Boolean)


    // Delete reminder
    @Query("UPDATE notes SET reminderDate = NULL, isDone = 0 WHERE id = :noteId")
    suspend fun deleteReminder(noteId: Long)

    @Transaction
    @Query("SELECT * FROM notes WHERE reminderDate > :currentTime AND isDone = 0")
    fun getUpcomingReminders(currentTime: Long): Flow<List<NoteWithTagsEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE reminderDate <= :currentTime OR isDone = 1")
    fun getCompletedReminders(currentTime: Long): Flow<List<NoteWithTagsEntity>>


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId IN (SELECT id FROM notes WHERE isTrashed = 1)")
    suspend fun deleteTrashNoteTagCrossRefs()

    @Transaction
    suspend fun emptyTrashWithTags() {
        deleteTrashNoteTagCrossRefs()
        emptyTrash()
    }

    // Back & Restore


    @Query("SELECT * FROM notes")
    fun getAllNotesStream(): Flow<List<NoteEntity>>


    @Transaction
    suspend fun mergeBackupData(
        database: NoteDatabase,
        notes: List<NoteEntity>,
        tags: List<TagEntity>,
        crossRefs: List<NoteTagRefEntity>
    ) {
        try {
            database.withTransaction {
                // Get existing data first
                val existingTags = getAllTagsSync()
                val existingNotes = getAllNotesSync()
                val existingTagsMap = existingTags.associateBy { it.name }
                val existingNotesMap = existingNotes.associateBy {
                    "${it.title}${it.content}${it.updateDate}"
                }

                // Process tags first
                val tagIdMapping = processTags(tags, existingTagsMap)

                // Process notes
                val noteIdMapping = processNotes(notes, existingNotesMap)

                // Process cross references
                processCrossRefs(crossRefs, noteIdMapping, tagIdMapping)
            }

        } catch (e: SQLException) {
            throw SQLException("Failed to merge backup data: ${e.message}")
        }
    }

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteEntity>

    private suspend fun processTags(
        backupTags: List<TagEntity>,
        existingTagsMap: Map<String, TagEntity>
    ): Map<Long, Long> {
        val tagIdMapping = mutableMapOf<Long, Long>()

        backupTags.forEach { backupTag ->
            val existingTag = existingTagsMap[backupTag.name]
            if (existingTag != null) {
                tagIdMapping[backupTag.id] = existingTag.id

            } else {
                // Insert new tag
                val newTagId = insertTag(backupTag.copy(id = 0))
                tagIdMapping[backupTag.id] = newTagId
            }
        }

        return tagIdMapping
    }

    private suspend fun processNotes(
        backupNotes: List<NoteEntity>,
        existingNotesMap: Map<String, NoteEntity>
    ): Map<Long, Long> {
        val noteIdMapping = mutableMapOf<Long, Long>()

        backupNotes.forEach { backupNote ->
            val noteKey = "${backupNote.title}${backupNote.content}${backupNote.updateDate}"
            val existingNote = existingNotesMap[noteKey]

            if (existingNote != null) {
                noteIdMapping[backupNote.id] = existingNote.id
                // Update if other properties changed
                if (hasNotePropertiesChanged(existingNote, backupNote)) {
                    updateNote(
                        existingNote.copy(
                            lightColor = backupNote.lightColor,
                            isPinned = backupNote.isPinned,
                            isArchived = backupNote.isArchived,
                            isTrashed = backupNote.isTrashed,
                            reminderDate = backupNote.reminderDate,
                            isDone = backupNote.isDone
                        )
                    )
                }
            } else {
                // Insert new note
                val newNoteId = insertNote(backupNote.copy(id = 0))
                noteIdMapping[backupNote.id] = newNoteId
            }
        }

        return noteIdMapping
    }

    private suspend fun processCrossRefs(
        backupCrossRefs: List<NoteTagRefEntity>,
        noteIdMapping: Map<Long, Long>,
        tagIdMapping: Map<Long, Long>
    ) {
        val processedCrossRefs = backupCrossRefs.mapNotNull { crossRef ->
            val newNoteId = noteIdMapping[crossRef.noteId] ?: return@mapNotNull null
            val newTagId = tagIdMapping[crossRef.tagId] ?: return@mapNotNull null
            NoteTagRefEntity(
                noteId = newNoteId,
                tagId = newTagId
            )
        }

        insertCrossRefsWithIgnore(processedCrossRefs)
    }

    private fun hasNotePropertiesChanged(
        existingNote: NoteEntity,
        backupNote: NoteEntity
    ): Boolean {
        return existingNote.lightColor != backupNote.lightColor ||
                existingNote.isPinned != backupNote.isPinned ||
                existingNote.isArchived != backupNote.isArchived ||
                existingNote.isTrashed != backupNote.isTrashed ||
                existingNote.reminderDate != backupNote.reminderDate ||
                existingNote.isDone != backupNote.isDone
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefsWithIgnore(crossRefs: List<NoteTagRefEntity>)


    @Query("UPDATE notes SET isChecklist = :isChecklist WHERE id = :noteId")
    suspend fun updateNoteChecklist(noteId: Long, isChecklist: Boolean)


}


*/
