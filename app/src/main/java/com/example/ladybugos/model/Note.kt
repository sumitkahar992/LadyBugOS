package com.example.ladybugos.model

import androidx.compose.ui.graphics.Color
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val updateDate: String = "",
    val lightColor: Int = 0,
    var isPinned: Boolean = false,
    var isArchived: Boolean = false,
    var isTrashed: Boolean = false,
    var reminderDate: Long? = null,
    var isDone: Boolean = false
) {
    fun matchesSearch(query: String): Boolean =
        title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
}

enum class RepeatInterval {
    DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Update
    suspend fun updateNotes(notes: List<Note>)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<Note>>

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

    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)


    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTagCrossRefs(noteId: Long)

    @Transaction
    suspend fun deleteNoteAndCrossRefs(note: Note) {
        deleteNoteTagCrossRefs(note.id)
        deleteNote(note)
    }

    // Reminder
    @Query("UPDATE notes SET reminderDate = :reminderDate WHERE id = :noteId")
    suspend fun updateNoteReminder(noteId: Long, reminderDate: Long?)

    @Query("SELECT * FROM notes WHERE reminderDate IS NOT NULL AND reminderDate > :currentTime ORDER BY reminderDate ASC")
    fun getUpcomingReminders(currentTime: Long): Flow<List<Note>>


    @Query("UPDATE notes SET isDone = :isDone WHERE id = :noteId")
    suspend fun updateNoteDoneStatus(noteId: Long, isDone: Boolean)

    @Query("SELECT isDone FROM notes WHERE id = :noteId")
    suspend fun isNoteDone(noteId: Long): Boolean



}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tags WHERE id = :id")
    fun getTagById(id: Long): Flow<Tag?>


    @Query("DELETE FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTagCrossRefs(tagId: Long)

    @Transaction
    suspend fun deleteTagAndCrossRefs(tag: Tag) {
        deleteTagCrossRefs(tag.id)
        deleteTag(tag)
    }
}


@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int
)

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"]),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"])
    ],
    indices = [Index(value = ["tagId"]), Index(value = ["noteId"])] // Index for tagName and noteId

)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)

data class NoteWithTags(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)


@Database(
    entities = [Note::class, Tag::class, NoteTagCrossRef::class],
    version = 1
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
}


val colorPalette = listOf(
    Color(0xFFE783F6), Color(0xFFFFDAC1), Color(0xFFC5E2D2), Color(0xFFB2EBF2),
    Color(0xFFFFE082), Color(0xFFD7CCC8), Color(0xFFDCD3FF), Color(0xFFFFE5C0),
    Color(0xFFF1E0FF), Color(0xFFF48FB1), Color(0xFFFFF176), Color(0xFFADD8E6),
    Color(0xFFE6F3E3), Color(0xFFC7CEEA), Color(0xFFD1C4E9), Color(0xFFFADAD9),
    Color(0xFFE6D2AA), Color(0xFFC8E6C9), Color(0xFFFFECB3), Color(0xFF78E9DA),
    Color(0xFFFFF3DE), Color(0xFFBFE3D3), Color(0xFFB5EAD7), Color(0xFFF7D1BA),
    Color(0xFFFF9AA2), Color(0xFFFFB7B2), Color(0xFFFDFFB6), Color(0xFFBDB2FF),
    Color(0xFFA0E7E5), Color(0xFFF0DEFD), Color(0xFFE2F0CB), Color(0xFFD5F4E6),
    Color(0xFFEC87FF), Color(0xFFB8F6EA), Color(0xFFFFF1C9), Color(0xFFFFCFDF),
    Color(0xFFE1F8DC), Color(0xFFFFE6E6), Color(0xFFC9F3E4), Color(0xFFF8E1A6),
    Color(0xFFE8D3EF), Color(0xFFE3F2FD), Color(0xFFF0F4E3), Color(0xFFFDE2E4),
    Color(0xFFD4E9DA), Color(0xFFE4F3EA), Color(0xFFA5D6A7), Color(0xFF81D4FA),
    Color(0xFF4DD0E1), Color(0xFFCE93D8), Color(0xFFFFAB91), Color(0xFFE6EE9C),
    Color(0xFFC5E1A5), Color(0xFFD2B48C), Color(0xFFD0E0E3), Color(0xFFBED2E6),
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
