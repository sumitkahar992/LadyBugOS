package com.example.ladybugos.datastore

import com.example.ladybugos.ui.theme.GridLayout
import com.example.ladybugos.ui.theme.Settings
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.Flow


interface SettingsRepo {

    val stream: Flow<Settings>
    fun <T> get(block: Settings.() -> T): Flow<T>

    suspend fun setTheme(theme: Theme)
    suspend fun setGridLayout(layout: GridLayout)
    suspend fun setDynamicColor(enabled: Boolean)

}