package com.despicable.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.despicable.core.database.dao.ChecklistDao
import com.despicable.core.database.dao.HabitDao
import com.despicable.core.database.dao.NoteDao
import com.despicable.core.database.dao.TagDao
import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.HabitEntity
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.NoteTagRefEntity
import com.despicable.core.database.model.TagEntity


@Database(
    entities = [NoteEntity::class,
        TagEntity::class,
        NoteTagRefEntity::class,
        ChecklistEntity::class,
        HabitEntity::class],
    version = 2
)
@TypeConverters(RoomConverters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun habitDao(): HabitDao

}