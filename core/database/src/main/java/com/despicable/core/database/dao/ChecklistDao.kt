package com.despicable.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.despicable.core.database.model.ChecklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    // Widgets
    @Query("SELECT * FROM checklist_items WHERE noteid = :noteId AND id = :itemId")
    suspend fun getChecklistItem(noteId: Long, itemId: Long): ChecklistEntity?


    // Checklist Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistEntity): Long

    @Update
    suspend fun updateChecklistItem(item: ChecklistEntity)

    @Update
    suspend fun updateChecklistItems(items: List<ChecklistEntity>)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteAllChecklistItemsForNote(noteId: Long)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteChecklistItem(itemId: Long)


//    @Query("UPDATE notes SET isChecklist = :isChecklist WHERE id = :noteId")
//    suspend fun updateNoteChecklist(noteId: Long, isChecklist: Boolean)

    // BACK- UP
    @Query("SELECT * FROM checklist_items")
    fun getAllChecklistItems(): Flow<List<ChecklistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistEntity>)

}