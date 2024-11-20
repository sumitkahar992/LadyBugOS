package com.example.ladybugos.ui.drawer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepo,
) : ViewModel() {

    fun updateTheme(theme: Theme) {
        viewModelScope.launch { repo.setTheme(theme) }
    }

    fun updateDynamicColor(dynamicColor: Boolean) {
        viewModelScope.launch { repo.setDynamicColor(dynamicColor) }
    }

}