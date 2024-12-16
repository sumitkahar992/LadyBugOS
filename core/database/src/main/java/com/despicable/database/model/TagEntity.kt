package com.despicable.database.model

import androidx.room.Dao
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
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
)

@Serializable
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"]),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"])
    ],
    indices = [Index(value = ["tagId"]), Index(value = ["noteId"])] // Index for tagName and noteId

)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)

data class NoteWithTagsEntity(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)


@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE id = :id")
    fun getTagById(id: Long): Flow<TagEntity?>


    @Query("DELETE FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTagCrossRefs(tagId: Long)

    @Transaction
    suspend fun deleteTagAndCrossRefs(tag: TagEntity) {
        deleteTagCrossRefs(tag.id)
        deleteTag(tag)
    }

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)
}


// First, let's implement the missing NoteTagCrossRefDao
@Dao
interface NoteTagCrossRefDao {
    @Query("SELECT * FROM note_tag_cross_ref")
    fun getAllCrossRefs(): Flow<List<NoteTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<NoteTagCrossRef>)

    @Delete
    suspend fun deleteCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("SELECT * FROM note_tag_cross_ref WHERE noteId = :noteId")
    fun getCrossRefsByNoteId(noteId: Long): Flow<List<NoteTagCrossRef>>

    @Query("SELECT * FROM note_tag_cross_ref WHERE tagId = :tagId")
    fun getCrossRefsByTagId(tagId: Long): Flow<List<NoteTagCrossRef>>
}

