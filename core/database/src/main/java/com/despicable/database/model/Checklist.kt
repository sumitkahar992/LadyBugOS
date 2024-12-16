package com.despicable.database.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
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
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val text: String,
    val isChecked: Boolean = false,
    val position: Int,
)


@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position")
    fun getChecklistItems(noteId: Long): Flow<List<ChecklistItem>>

    @Insert
    suspend fun insertChecklistItem(item: ChecklistItem): Long

    @Insert
    suspend fun insertChecklistItems(items: List<ChecklistItem>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId AND isChecked = 1")
    suspend fun deleteCheckedItems(noteId: Long)

    @Query("UPDATE checklist_items SET position = position - 1 WHERE noteId = :noteId AND position > :deletedPosition")
    suspend fun reorderAfterDelete(noteId: Long, deletedPosition: Int)

    @Query("SELECT MAX(position) FROM checklist_items WHERE noteId = :noteId")
    suspend fun getMaxPosition(noteId: Long): Int?
}