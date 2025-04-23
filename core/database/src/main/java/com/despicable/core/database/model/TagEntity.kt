package com.despicable.core.database.model

import android.annotation.SuppressLint
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)] // Ensure tag names are unique
)
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
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE   // Add cascade delete
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE   // Add cascade delete
        )
    ],
    indices = [Index(value = ["tagId"]), Index(value = ["noteId"])] // Index for tagName and noteId

)
data class NoteTagRefEntity(
    val noteId: Long,
    val tagId: Long
)


@Serializable
data class NoteWithTagsEntity(
    @Embedded val note: NoteEntity,
    @Relation(
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




