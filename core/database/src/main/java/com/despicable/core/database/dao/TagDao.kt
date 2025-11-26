package com.despicable.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.despicable.core.database.model.TagEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Transaction
    suspend fun deleteTagAndCrossRefs(tag: TagEntity) {
        deleteTagCrossRefs(tag.id)
        deleteTag(tag)
    }

    @Query("DELETE FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTagCrossRefs(tagId: Long)

    @Query("SELECT * FROM tags WHERE id IN (SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId)")
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>

}
