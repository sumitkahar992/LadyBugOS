package com.despicable.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.despicable.core.database.dao.NoteDao
import com.despicable.core.database.dao.TagDao
import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.ChecklistItemDao
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.NoteTagRefEntity
import com.despicable.core.database.model.TagEntity


@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagRefEntity::class, ChecklistEntity::class],
    version = 2
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun checklistDao(): ChecklistItemDao
}