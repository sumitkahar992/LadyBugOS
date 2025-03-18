package com.despicable.core.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val updateDate: Long = 0,
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Long? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Long? = null,
    val isDone: Boolean = false,
    val isChecklist: Boolean = false,
) {
    fun matchesSearch(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
                content.contains(query, ignoreCase = true)
}

data class NoteWithTags(
    val note: Note,
    val tags: List<Tag>
)


data class NoteComplete(
    val note: Note,
    val checklistItems: List<Checklist>,
    val tags: List<Tag>
)


fun getRelativeTimeAgo(updateTimestamp: Long): String {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    return try {
        val updateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(updateTimestamp),
            java.time.ZoneId.systemDefault()
        )
        val now = LocalDateTime.now()
        val duration = Duration.between(updateTime, now)

        when {
            duration.toMinutes() < 1 -> "just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
            duration.toHours() < 4 -> "${duration.toHours()} hour ago" // Show hours if updated within 3 hours
            updateTime.toLocalDate().isEqual(
                now.toLocalDate().minusDays(1)
            ) -> "Yesterday, ${updateTime.format(timeFormatter)}" // Show date if updated yesterday
            duration.toHours() < 24 -> updateTime.format(timeFormatter) // Show time if updated within 24 hours
            else -> updateTime.format(dateFormatter) // Show date for older updates
        }
    } catch (e: Exception) {
        "Unknown time" // Fallback in case of parsing error
    }
}


/*
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
*/
