package com.example.ladybugos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


class MainViewModel @Inject constructor(
    repo: SettingsRepo
) : ViewModel() {


    val theme: StateFlow<Theme> = repo.get { theme }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = Theme.System
    )

}