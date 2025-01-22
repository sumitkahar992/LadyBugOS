package com.despicable.core.database.dao

import android.database.SQLException
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
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.NoteTagRefEntity
import com.despicable.core.database.model.NoteWithTagsEntity
import com.despicable.core.database.model.TagEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Transaction // Add this for nested operations
    suspend fun insertNoteWithTags(note: NoteEntity, tagIds: List<Long>): Long {
        val noteId = insertNote(note)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(noteId, tagId))
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
        // Update the note
        updateNote(note)

        // Update tags
        deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagRefEntity(note.id, tagId))
        }

        // Update checklist items
        deleteAllChecklistItemsForNote(note.id)
        checklistItems.forEach { checklistItem ->
            insertChecklistItem(checklistItem.copy(noteId = note.id))
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
