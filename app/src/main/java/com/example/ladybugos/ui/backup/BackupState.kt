package com.example.ladybugos.ui.backup


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