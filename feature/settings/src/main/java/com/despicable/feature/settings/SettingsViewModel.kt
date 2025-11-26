package com.despicable.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.datastore.SettingsRepo
import com.despicable.core.designsystem.theme.Theme
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepo
) : ViewModel() {

    fun updateTheme(theme: Theme) {
        viewModelScope.launch { repo.setTheme(theme) }
    }

    fun updateDynamicColor(dynamicColor: Boolean) {
        viewModelScope.launch { repo.setDynamicColor(dynamicColor) }
    }


}