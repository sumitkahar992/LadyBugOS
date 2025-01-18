package com.despicable.core.database.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("noteId")
    ]
)
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val content: String,
    val isChecked: Boolean = false,
    val position: Int,
)


@Dao
interface ChecklistItemDao {
    /*    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position")
        fun getChecklistItems(noteId: Long): Flow<List<ChecklistEntity>>

        @Insert
        suspend fun insertChecklistItem(item: ChecklistEntity): Long

        @Insert
        suspend fun insertChecklistItems(items: List<ChecklistEntity>)

        @Update
        suspend fun updateChecklistItem(item: ChecklistEntity)

        @Delete
        suspend fun deleteChecklistItem(item: ChecklistEntity)

        @Query("DELETE FROM checklist_items WHERE noteId = :noteId AND isChecked = 1")
        suspend fun deleteCheckedItems(noteId: Long)

        @Query("UPDATE checklist_items SET position = position - 1 WHERE noteId = :noteId AND position > :deletedPosition")
        suspend fun reorderAfterDelete(noteId: Long, deletedPosition: Int)

        @Query("SELECT MAX(position) FROM checklist_items WHERE noteId = :noteId")
        suspend fun getMaxPosition(noteId: Long): Int?*/

    //
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position")
    fun getChecklistItemsFlow(noteId: Long): Flow<List<ChecklistEntity>>

    @Insert
    suspend fun insertChecklistItem(item: ChecklistEntity): Long

    @Update
    suspend fun updateChecklistItem(item: ChecklistEntity)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistEntity)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteAllChecklistItems(noteId: Long)

    @Transaction
    @Query("UPDATE checklist_items SET position = position + 1 WHERE noteId = :noteId AND position >= :startPosition")
    suspend fun shiftItemsDown(noteId: Long, startPosition: Int)

    @Query("SELECT MAX(position) FROM checklist_items WHERE noteId = :noteId")
    suspend fun getMaxPosition(noteId: Long): Int?

    @Query("UPDATE checklist_items SET position = :newPosition WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: Long, newPosition: Int)
}


