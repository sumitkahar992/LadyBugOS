package com.example.ladybugos.claude_note

import androidx.room.Dao
import androidx.room.Database
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
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Entities
@Entity(tableName = "knotes")
data class KNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val updateDate: String = "",
    val lightColor: Int = 0,
    var isPinned: Boolean = false,
    var isArchived: Boolean = false,
    var isTrashed: Boolean = false
) {
    fun matchesSearch(query: String): Boolean =
        title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
}

@Entity(tableName = "ktags")
data class KTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)


@Entity(
    tableName = "knote_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = KNote::class, parentColumns = ["id"], childColumns = ["noteId"]),
        ForeignKey(entity = KTag::class, parentColumns = ["id"], childColumns = ["tagId"])
    ],
    indices = [Index(value = ["tagId"])]
)
data class KNoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)

data class KNoteWithTags(
    @Embedded val note: KNote,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = KNoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<KTag>
)

// DAOs
@Dao
interface KNoteDao {
    @Transaction
    @Query("SELECT * FROM knotes")
    fun getAllNotesWithTags(): Flow<List<KNoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: KNote): Long

    @Update
    suspend fun updateNote(note: KNote)

    @Delete
    suspend fun deleteNote(note: KNote)

    @Query("SELECT * FROM knotes WHERE id = :id")
    fun getNoteById(id: Long): Flow<KNote?>

    @Transaction
    @Query("SELECT * FROM knotes WHERE id = :id")
    fun getNoteWithTagsById(id: Long): Flow<KNoteWithTags?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: KNoteTagCrossRef)

    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: KNoteTagCrossRef)

    @Query("DELETE FROM knote_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)


    @Query("DELETE FROM knote_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTagCrossRefs(noteId: Long)

    @Transaction
    suspend fun deleteNoteAndCrossRefs(note: KNote) {
        deleteNoteTagCrossRefs(note.id)
        deleteNote(note)
    }
}

@Dao
interface KTagDao {
    @Query("SELECT * FROM ktags")
    fun getAllTags(): Flow<List<KTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: KTag): Long

    @Update
    suspend fun updateTag(tag: KTag)

    @Delete
    suspend fun deleteTag(tag: KTag)

    @Query("SELECT * FROM ktags WHERE id = :id")
    fun getTagById(id: Long): Flow<KTag?>


    @Query("DELETE FROM knote_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTagCrossRefs(tagId: Long)

    @Transaction
    suspend fun deleteTagAndCrossRefs(tag: KTag) {
        deleteTagCrossRefs(tag.id)
        deleteTag(tag)
    }
}

// Repository
class KNoteRepository(private val noteDao: KNoteDao, private val tagDao: KTagDao) {
    fun getAllNotesWithTags() = noteDao.getAllNotesWithTags()
    fun getAllTags() = tagDao.getAllTags()
    fun getNoteWithTagsById(id: Long) = noteDao.getNoteWithTagsById(id)

    suspend fun insertNote(note: KNote, tagIds: List<Long>): Long {
        val noteId = noteDao.insertNote(note)
        tagIds.forEach { tagId ->
            noteDao.insertNoteTagCrossRef(KNoteTagCrossRef(noteId, tagId))
        }
        return noteId
    }

    suspend fun updateNote(note: KNote, tagIds: List<Long>) {
        noteDao.updateNote(note)
        noteDao.deleteAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            noteDao.insertNoteTagCrossRef(KNoteTagCrossRef(note.id, tagId))
        }
    }

    suspend fun insertTag(tag: KTag) = tagDao.insertTag(tag)


    suspend fun updateTag(tag: KTag) = tagDao.updateTag(tag)

    suspend fun deleteNote(note: KNote) {
        noteDao.deleteNoteAndCrossRefs(note)
    }

    suspend fun deleteTag(tag: KTag) {
        tagDao.deleteTagAndCrossRefs(tag)
    }
}

// DB
@Database(
    entities = [KNote::class, KTag::class, KNoteTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class KNoteDatabase : RoomDatabase() {
    abstract fun kNoteDao(): KNoteDao
    abstract fun kTagDao(): KTagDao

}
