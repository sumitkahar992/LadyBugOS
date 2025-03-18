package com.despicable.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.despicable.core.database.model.NoteTagRefEntity
import kotlinx.coroutines.flow.Flow


// First, let's implement the missing NoteTagCrossRefDao

@Dao
interface NoteTagCrossRefDao {
    @Query("SELECT * FROM note_tag_cross_ref")
    fun getAllCrossRefs(): Flow<List<NoteTagRefEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: NoteTagRefEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<NoteTagRefEntity>)

    @Delete
    suspend fun deleteCrossRef(crossRef: NoteTagRefEntity)

    @Query("DELETE FROM note_tag_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("SELECT * FROM note_tag_cross_ref WHERE noteId = :noteId")
    fun getCrossRefsByNoteId(noteId: Long): Flow<List<NoteTagRefEntity>>

    @Query("SELECT * FROM note_tag_cross_ref WHERE tagId = :tagId")
    fun getCrossRefsByTagId(tagId: Long): Flow<List<NoteTagRefEntity>>
}
