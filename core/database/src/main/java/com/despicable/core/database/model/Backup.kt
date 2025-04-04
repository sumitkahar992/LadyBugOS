package com.despicable.core.database.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BackupData(
    val notes: List<NoteEntity>,
    val tags: List<TagEntity>,
    val noteTagCrossRefs: List<NoteTagRefEntity>,
    val checklistItems: List<ChecklistEntity>? = null
)

sealed class BackupResults {
    data class Progress(val percentage: Int) : BackupResults()
    data class Success(
        val notesCount: Int = 0,
        val tagsCount: Int = 0,
        val operation: Operation
    ) : BackupResults() {
        enum class Operation { BACKUP, RESTORE }
    }

    data class Error(val message: String) : BackupResults()
}