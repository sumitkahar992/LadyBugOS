package com.despicable.core.database.model

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
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
    indices = [Index("noteId", "position")]  // Composite index
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


    // Insert a single checklist item
    @Insert
    suspend fun insertChecklistItem(item: ChecklistEntity): Long

    // Insert multiple checklist items at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistEntity>)


    // Retrieve checklist items for a specific note, ordered by position
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY position ASC")
    fun getChecklistItemsByNoteId(noteId: Long): Flow<List<ChecklistEntity>>

    // Update a single checklist item
    @Update
    suspend fun updateChecklistItem(item: ChecklistEntity)

    // Update multiple checklist items
    @Update
    suspend fun updateChecklistItems(items: List<ChecklistEntity>)


    // Delete all checklist items associated with a specific note
    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteChecklistItemsByNoteId(noteId: Long)

    // Delete a single checklist item by its ID
    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteChecklistItem(itemId: Long)


    @Transaction
    suspend fun updateChecklistItems2(items: List<ChecklistEntity>) {
        items.forEach { item ->
            updateChecklistItem(item)
        }

    }

    // Replace all checklist items for a specific note (delete old and insert new)
    @Transaction
    suspend fun replaceChecklistItemsForNote(
        noteId: Long,
        items: List<ChecklistEntity>
    ) {
        deleteChecklistItemsByNoteId(noteId)
        insertChecklistItems(items.map { it.copy(noteId = noteId) })
    }

}


