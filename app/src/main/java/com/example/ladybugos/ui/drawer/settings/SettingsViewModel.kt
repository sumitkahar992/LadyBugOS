package com.example.ladybugos.ui.drawer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepo,
) : ViewModel() {


    val theme: StateFlow<Theme> = repo.get { theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), Theme.System)

    val dynamicColor: StateFlow<Boolean> = repo.get { dynamicColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)


    fun updateTheme(theme: Theme) {
        viewModelScope.launch { repo.setTheme(theme) }
    }

    fun updateDynamicColor(dynamicColor: Boolean) {
        viewModelScope.launch { repo.setDynamicColor(dynamicColor) }
    }

}