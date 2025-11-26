package com.despicable.core.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: NoteContent = NoteContent.Text(""),
    val creationDate: Instant,
    val updateDate: Instant,
    val lightColor: Int = 0,
    val isPinned: Boolean = false,
    val pinnedDate: Instant? = null,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderDate: Instant? = null,
    val isDone: Boolean = false,
    val noteType: NoteType = NoteType.TEXT
) {
    fun matchesSearch(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
                when (content) {
                    is NoteContent.Text -> content.text.contains(query, ignoreCase = true)
                    is NoteContent.ChecklistItems -> content.items.any {
                        it.content.contains(
                            query,
                            ignoreCase = true
                        )
                    }
                }
}


enum class NoteType {
    TEXT,       // Plain text note
    CHECKLIST,  // Checklist note
}


@Serializable
sealed class NoteContent {
    @Serializable   // Text content
    data class Text(val text: String) : NoteContent()

    @Serializable   // CheckList content
    data class ChecklistItems(val items: List<Checklist>) :
        NoteContent()
}


data class NoteComplete(
    val note: Note,
    val checklistItems: List<Checklist> = emptyList(),
    val habitItems: List<HabitItem> = emptyList(),
    val tags: List<Tag> = emptyList()
)

fun getRelativeTimeAgo(updateInstant: Instant): String {
    val now = Clock.System.now()
    val updateTime = updateInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val duration = now - updateInstant

    // Subtract 1 day to get yesterday
    val yesterday = currentTime.date.minus(1, DateTimeUnit.DAY)

    return when {
        duration.inWholeMinutes < 1 -> "just now"
        duration.inWholeMinutes < 60 -> "${duration.inWholeMinutes} min ago"
        duration.inWholeHours < 4 -> "${duration.inWholeHours} hour ago"
        updateTime.date == yesterday -> {
            val timeStr = updateTime.time.toString().substringBeforeLast(':') // e.g., "13:45"
            "Yesterday, $timeStr"
        }

        duration.inWholeHours < 24 -> updateTime.time.toString().substringBeforeLast(':')
        else -> updateTime.date.toString() // e.g., "2023-10-15"
    }
}


/*
// Java DateTIme
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

enum class NoteType {
    TEXT,       // Plain text note
    CHECKLIST,  // Checklist note
    AUDIO,      // Audio recording note
    IMAGE,      // Image note
    DRAWING     // Drawing/sketch note
}

enum class NoteType {
    TEXT_NOTE,
    CHECKLIST,
    HABIT_TRACKER
}
@Serializable
sealed class NoteContent {
    @Serializable
    data class Text(val text: String) : NoteContent()

    @Serializable
    data class Checklist(val items: List<ChecklistItem>) : NoteContent() {
        @Serializable
        data class ChecklistItem(
            val id: String = java.util.UUID.randomUUID().toString(),
            val text: String,
            val isChecked: Boolean = false,
            val position: Int
        )
    }

    @Serializable
    data class Media(
        val text: String = "",
        val mediaItems: List<MediaItem> = emptyList()
    ) : NoteContent() {
        @Serializable
        sealed class MediaItem {
            abstract val id: String

            @Serializable
            data class Image(
                override val id: String = java.util.UUID.randomUUID().toString(),
                val uri: String,
                val thumbnailUri: String,  // Separate thumbnail for list views
                val position: Int
            ) : MediaItem()

            @Serializable
            data class Audio(
                override val id: String = java.util.UUID.randomUUID().toString(),
                val uri: String,
                val durationMs: Long,
                val position: Int
            ) : MediaItem()

            @Serializable
            data class Drawing(
                override val id: String = java.util.UUID.randomUUID().toString(),
                val uri: String,
                val thumbnailUri: String,
                val position: Int
            ) : MediaItem()
        }
    }
}
*/


