package com.example.ladybugos.model

import android.database.SQLException
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Keep
@Serializable
@Entity(tableName = "notes")
data class Note(
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
        title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
}


@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Transaction // Add this for nested operations
    suspend fun insertNoteWithTags(note: Note, tagIds: List<Long>): Long {
        val noteId = insertNote(note)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
        return noteId
    }

    @Transaction // Add this for nested operations
    suspend fun updateNoteWithTags(note: Note, tagIds: List<Long>) {
        updateNote(note)
        deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagCrossRef(note.id, tagId))
        }
    }

    @Update
    suspend fun updateNotes(notes: List<Note>)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<Note?>


    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // TAGS
    @Transaction
    @Query("SELECT * FROM notes")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteWithTagsById(id: Long): Flow<NoteWithTags?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTagCrossRefs(noteId: Long)

    @Transaction
    suspend fun deleteNoteAndTag(note: Note) {
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
    fun getUpcomingReminders(currentTime: Long): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE reminderDate <= :currentTime OR isDone = 1")
    fun getCompletedReminders(currentTime: Long): Flow<List<NoteWithTags>>


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId IN (SELECT id FROM notes WHERE isTrashed = 1)")
    suspend fun deleteTrashNoteTagCrossRefs()

    @Transaction
    suspend fun emptyTrashWithTags() {
        deleteTrashNoteTagCrossRefs()
        emptyTrash()
    }

    // Back & Restore


    @Query("SELECT * FROM notes")
    fun getAllNotesStream(): Flow<List<Note>>


    @Transaction
    suspend fun mergeBackupData(
        notes: List<Note>,
        tags: List<Tag>,
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
    suspend fun getAllTagsSync(): List<Tag>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<Note>

    private suspend fun processTags(
        backupTags: List<Tag>,
        existingTagsMap: Map<String, Tag>
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
        backupNotes: List<Note>,
        existingNotesMap: Map<String, Note>
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

    private fun hasNotePropertiesChanged(existingNote: Note, backupNote: Note): Boolean {
        return existingNote.lightColor != backupNote.lightColor ||
                existingNote.isPinned != backupNote.isPinned ||
                existingNote.isArchived != backupNote.isArchived ||
                existingNote.isTrashed != backupNote.isTrashed ||
                existingNote.reminderDate != backupNote.reminderDate ||
                existingNote.isDone != backupNote.isDone
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefsWithIgnore(crossRefs: List<NoteTagCrossRef>)


}


@Database(
    entities = [Note::class, Tag::class, NoteTagCrossRef::class, ChecklistItem::class],
    version = 2
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagCrossRefDao(): NoteTagCrossRefDao
    abstract fun checklistDao(): ChecklistItemDao
}

val colorPalette = listOf(
    Color(0xFFFFDAC1), Color(0xFFC5E2D2), Color(0xFFB2EBF2),
    Color(0xFFFFE082), Color(0xFFD7CCC8), Color(0xFFDCD3FF), Color(0xFFFFE5C0),
    Color(0xFFF1E0FF), Color(0xFFF48FB1), Color(0xFFFFF289), Color(0xFFADD8E6),
    Color(0xFFE6F3E3), Color(0xFFC7CEEA), Color(0xFFD1C4E9), Color(0xFFFADAD9),
    Color(0xFFE6D2AA), Color(0xFFC8E6C9), Color(0xFFFFECB3), Color(0xFF78E9DA),
    Color(0xFFBFE3D3), Color(0xFFB5EAD7), Color(0xFFF7D1BA), Color(0xFFFF9AA2),
    Color(0xFFFFB7B2), Color(0xFFFDFFB6), Color(0xFFBDB2FF),
    Color(0xFFA0E7E5), Color(0xFFF0DEFD), Color(0xFFE2F0CB), Color(0xFFD5F4E6),
    Color(0xFFF09EFF), Color(0xFFB8F6EA), Color(0xFFFFF1C9), Color(0xFFFFCFDF),
    Color(0xFFE1F8DC), Color(0xFFFFE6E6), Color(0xFFC9F3E4), Color(0xFFF8E1A6),
    Color(0xFFE8D3EF), Color(0xFFE3F2FD), Color(0xFFF0F4E3), Color(0xFFFDE2E4),
    Color(0xFFD4E9DA), Color(0xFFE4F3EA), Color(0xFFA5D6A7), Color(0xFF81D4FA),
    Color(0xFF60E3F3), Color(0xFFCE93D8), Color(0xFFFFAB91), Color(0xFFE6EE9C),
    Color(0xFFC5E1A5), Color(0xFFD0E0E3), Color(0xFFBED2E6),
)


// Darken extension function for Dark Theme
fun Color.darken(factor: Float): Color {
    return this.copy(
        red = (this.red * factor).coerceIn(0f, 1f),
        green = (this.green * factor).coerceIn(0f, 1f),
        blue = (this.blue * factor).coerceIn(0f, 1f)
    )
}

fun getRelativeTimeAgo(updateDateString: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    return try {
        val updateTime = LocalDateTime.parse(updateDateString, inputFormatter)
        val now = LocalDateTime.now()
        val duration = Duration.between(updateTime, now)

        when {
            duration.toMinutes() < 1 -> "just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
            duration.toHours() < 4 -> "${duration.toHours()} hour ago" // Show hours if updated within 3 hours
            updateTime.toLocalDate().isEqual(
                now.toLocalDate().minusDays(1)
            ) -> updateTime.format(dateFormatter) // Show date if updated yesterday
            duration.toHours() < 24 -> updateTime.format(timeFormatter) // Show time if updated within 24 hours
            else -> updateTime.format(dateFormatter) // Show date for older updates
        }
    } catch (e: Exception) {
        updateDateString // Fallback in case of parsing error
    }
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
