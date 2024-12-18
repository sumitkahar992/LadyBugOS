package com.despicable.core.data.repository

import android.net.Uri
import com.despicable.core.database.model.BackupResults
import kotlinx.coroutines.flow.Flow


interface BackupRepository {
    fun createBackup(destinationUri: Uri): Flow<BackupResults>
    fun restoreBackup(backupUri: Uri): Flow<BackupResults>
}