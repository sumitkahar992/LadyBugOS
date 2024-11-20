package com.example.ladybugos.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NativeBackupViewModel(
    private val backupManager: NativeBackupManager
) : ViewModel() {
    private val _backupState = MutableStateFlow<BackupResults?>(null)
    val backupState: StateFlow<BackupResults?> = _backupState.asStateFlow()

    fun performBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.createBackup(uri).collect { result ->
                _backupState.value = result
            }
        }
    }

    fun performRestore(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreBackup(uri).collect { result ->
                _backupState.value = result
            }
        }
    }
}
