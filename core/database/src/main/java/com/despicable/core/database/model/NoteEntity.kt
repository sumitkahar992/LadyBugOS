package com.despicable.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
