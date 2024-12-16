package com.despicable.ladybugos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.core.datastore.SettingsRepo
import com.despicable.model.Settings
import com.despicable.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


class MainViewModel @Inject constructor(
    repo: SettingsRepo
) : ViewModel() {


    private val theme: Flow<Theme> = repo.get { theme }
    private val dynamicTheme: Flow<Boolean> = repo.get { dynamicColor }


    // Convert combined flow to StateFlow
    val themeConfig: StateFlow<Settings> = combine(
        theme,
        dynamicTheme
    ) { theme, dynamicColor ->
        Settings(
            theme = theme,
            dynamicColor = dynamicColor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = Settings(
            theme = Theme.System,
            dynamicColor = true
        )
    )

}