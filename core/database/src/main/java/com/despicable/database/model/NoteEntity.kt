package com.despicable.database.model

import android.database.SQLException
import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val updateDate: String = "",
    val lightColor: Int = 0,
    var isPinned: Boolean = false,
    val pinnedDate: Long? = null,
    var isArchived: Boolean = false,
    var isTrashed: Boolean = false,
    var reminderDate: Long? = null,
    var isDone: Boolean = false,
    var isChecklist: Boolean = false, // New field

) {
    fun matchesSearch(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
                content.contains(query, ignoreCase = true)
}


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
            insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
        return noteId
    }

    @Transaction // Add this for nested operations
    suspend fun updateNoteWithTags(note: NoteEntity, tagIds: List<Long>) {
        updateNote(note)
        deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagCrossRef(note.id, tagId))
        }
    }

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
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)


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
        notes: List<NoteEntity>,
        tags: List<TagEntity>,
        crossRefs: List<NoteTagCrossRef>
    ) {
        try {
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
        backupCrossRefs: List<NoteTagCrossRef>,
        noteIdMapping: Map<Long, Long>,
        tagIdMapping: Map<Long, Long>
    ) {
        val processedCrossRefs = backupCrossRefs.mapNotNull { crossRef ->
            val newNoteId = noteIdMapping[crossRef.noteId] ?: return@mapNotNull null
            val newTagId = tagIdMapping[crossRef.tagId] ?: return@mapNotNull null
            NoteTagCrossRef(
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
    suspend fun insertCrossRefsWithIgnore(crossRefs: List<NoteTagCrossRef>)


}


@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class, ChecklistItem::class],
    version = 2
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagCrossRefDao(): NoteTagCrossRefDao
    abstract fun checklistDao(): ChecklistItemDao
}


/*


// Function to format the update date for display
fun formatUpdateDate(
    dateString: String,
    inputPattern: String = "yyyy-MM-dd HH:mm:ss",
    outputPattern: String = "MMM dd, hh:mm a"
): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern(inputPattern)
        val outputFormatter = DateTimeFormatter.ofPattern(outputPattern)
        LocalDateTime.parse(dateString, inputFormatter).format(outputFormatter)
    } catch (e: Exception) {
        dateString // Fallback in case of parsing error
    }
}

fun main() {
    val testCases = listOf(
        "2024-09-29 23:31:21", // just now (assuming today is Sep 29)
        "2024-09-29 22:00:00", // minutes ago (assuming today is Sep 29)
        "2024-09-29 21:00:00", // minutes ago (assuming today is Sep 29)
        "2024-09-28 3:10:00", // should show time (e.g., "09:00 PM")
        "2024-09-28 2:00:00", // should show time (e.g., "08:00 PM")
        "2024-09-28 18:00:00", // should show date (e.g., "Sep 28")
        "2024-09-27 18:00:00", // should show date (e.g., "Sep 27")
        "2024-09-26 06:01 PM"  // should show full date (e.g., "Sep 26")
    )

    for (testCase in testCases) {
        println("Test case: $testCase")
        println("Result: ${getRelativeTimeAgo(testCase)}")
    }
}

*/
