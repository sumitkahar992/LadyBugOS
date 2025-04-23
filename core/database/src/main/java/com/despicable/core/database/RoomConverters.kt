package com.despicable.core.database

import androidx.room.TypeConverter
import com.despicable.core.database.model.HabitCompletionEntity
import com.despicable.core.model.NoteContent
import com.despicable.core.model.NoteType
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json


/**
 * Room type converters for complex objects
 */
object RoomConverters {


    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(timestamp: Long?): Instant? =
        timestamp?.let { Instant.fromEpochMilliseconds(it) }


    // Converter for NoteType: Enum to String and back
    @TypeConverter
    fun noteTypeToString(value: NoteType): String = value.name

    @TypeConverter
    fun stringToNoteType(value: String): NoteType? =
        try {
            NoteType.valueOf(value)
        } catch (e: Exception) {
            NoteType.TEXT
        }

    // Converter for NoteContent: Sealed class to JSON String and back
    @TypeConverter
    fun fromNoteContent(content: NoteContent): String = json.encodeToString(content)

    @TypeConverter
    fun toNoteContent(jsonString: String): NoteContent = json.decodeFromString(jsonString)


    @TypeConverter
    fun fromHabitCompletionList(completions: List<HabitCompletionEntity>): String {
        return json.encodeToString(completions)
    }

    @TypeConverter
    fun toHabitCompletionList(completionsString: String): List<HabitCompletionEntity> {
        return if (completionsString.isBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(completionsString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }


    /*    @TypeConverter
        fun repeatTypeToString(value: RepeatType?): String? = value?.name

        @TypeConverter
        fun stringToRepeatType(value: String?): RepeatType? =
            value?.let {
                try {
                    RepeatType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }*/
}