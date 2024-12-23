package com.despicable.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.data.repository.BackupRepository
import com.despicable.core.database.model.BackupResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val repo: BackupRepository,
) : ViewModel() {
    private val _backupState = MutableStateFlow<BackupResults?>(null)
    val backupState: StateFlow<BackupResults?> = _backupState.asStateFlow()

    fun performBackup(uri: Uri) {
        viewModelScope.launch {
            repo.createBackup(uri).collect { result ->
                _backupState.value = result
            }
        }
    }

    fun performRestore(uri: Uri) {
        viewModelScope.launch {
            repo.restoreBackup(uri).collect { result ->
                _backupState.value = result
            }
        }
    }
}
