package com.despicable.core.data.repository

import android.net.Uri
import com.despicable.core.database.model.BackupResults
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    /**
     * Creates a backup of all notes, tags, and their relationships
     * @param destinationUri The URI where the backup will be saved
     * @return Flow of BackupResults indicating progress and final result
     */
    fun createBackup(destinationUri: Uri): Flow<BackupResults>

    /**
     * Restores notes, tags, and their relationships from a backup file
     * @param backupUri The URI of the backup file to restore from
     * @return Flow of BackupResults indicating progress and final result
     */
    fun restoreBackup(backupUri: Uri): Flow<BackupResults>

    /**
     * Validates if a file is a valid backup
     * @param backupUri The URI of the file to validate
     * @return True if the file is a valid backup, false otherwise
     */
    suspend fun isValidBackup(backupUri: Uri): Boolean
}