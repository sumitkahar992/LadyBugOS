package com.despicable.core.database.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.despicable.core.model.NoteContent
import com.despicable.core.model.NoteType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "notes",
    indices = [
        Index("updateDate"),           // For sorting
        Index("reminderDate"),         // For reminders
        Index(value = ["isPinned", "isArchived", "isTrashed"]) // Composite index For filtering
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: NoteContent = NoteContent.Text(""),
    val creationDate: Instant, // Explicitly updated
    val updateDate: Instant, // Explicitly updated
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Instant? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Instant? = null,
    val isDone: Boolean = false,
    val noteType: NoteType = NoteType.TEXT
)


// Add this combined relationship class for complete note data
data class NoteCompleteEntity(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val checklistItems: List<ChecklistEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val habitItems: List<HabitEntity>,

    @Relation(
        entity = TagEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagRefEntity::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)


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
