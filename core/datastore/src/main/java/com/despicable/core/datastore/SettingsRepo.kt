package com.despicable.core.datastore

import com.despicable.model.GridLayout
import com.despicable.model.Settings
import com.despicable.model.Theme
import kotlinx.coroutines.flow.Flow


interface SettingsRepo {

    val stream: Flow<Settings>
    fun <T> get(block: Settings.() -> T): Flow<T>

    suspend fun setTheme(theme: Theme)
    suspend fun setGridLayout(layout: GridLayout)
    suspend fun setDynamicColor(enabled: Boolean)

}