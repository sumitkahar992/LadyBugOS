package com.despicable.core.domain.usecase.backup

import android.net.Uri
import com.despicable.core.data.repository.BackupRepository
import com.despicable.core.database.model.BackupResults
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    operator fun invoke(backupUri: Uri): Flow<BackupResults> {
        return backupRepository.restoreBackup(backupUri)
    }
}