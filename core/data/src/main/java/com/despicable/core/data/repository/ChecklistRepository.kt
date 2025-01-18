package com.despicable.core.data.repository

import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.ChecklistItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// ChecklistRepository.kt
class ChecklistRepository @Inject constructor(
    private val checklistDao: ChecklistItemDao
) {
    fun getChecklistItemsFlow(noteId: Long): Flow<List<ChecklistEntity>> =
        checklistDao.getChecklistItemsFlow(noteId)

    suspend fun addChecklistItem(noteId: Long, content: String) {
        val maxPosition = checklistDao.getMaxPosition(noteId) ?: -1
        val newItem = ChecklistEntity(
            noteId = noteId,
            content = content,
            position = maxPosition + 1
        )
        checklistDao.insertChecklistItem(newItem)
    }

    suspend fun updateChecklistItem(item: ChecklistEntity, newContent: String) {
        checklistDao.updateChecklistItem(item.copy(content = newContent))
    }

    suspend fun toggleChecklistItem(item: ChecklistEntity) {
        checklistDao.updateChecklistItem(item.copy(isChecked = !item.isChecked))
    }

    suspend fun deleteChecklistItem(item: ChecklistEntity) {
        checklistDao.deleteChecklistItem(item)
    }

    suspend fun reorderChecklistItems(noteId: Long, fromPosition: Int, toPosition: Int) {
        // Implementation for drag-and-drop reordering
        val items = checklistDao.getChecklistItemsFlow(noteId).first()
        val itemToMove = items.find { it.position == fromPosition } ?: return

        if (fromPosition < toPosition) {
            items.filter { it.position in (fromPosition + 1)..toPosition }
                .forEach { item ->
                    checklistDao.updateItemPosition(item.id, item.position - 1)
                }
        } else {
            items.filter { it.position in toPosition until fromPosition }
                .forEach { item ->
                    checklistDao.updateItemPosition(item.id, item.position + 1)
                }
        }
        checklistDao.updateItemPosition(itemToMove.id, toPosition)
    }
}