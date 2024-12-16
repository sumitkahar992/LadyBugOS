package com.despicable.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.despicable.model.GridLayout
import com.despicable.model.Settings
import com.despicable.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject


class DatastoreRepo @Inject constructor(
    private val context: Context
) : SettingsRepo {

    private companion object Keys {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings_preferences")

        val THEME: Preferences.Key<String> = stringPreferencesKey("theme")
        val GRID_LAYOUT: Preferences.Key<String> = stringPreferencesKey("grid_layout") // New key
        val DYNAMIC_COLOR: Preferences.Key<Boolean> =
            booleanPreferencesKey("dynamic_color") // New key

    }

    override val stream: Flow<Settings>
        get() = context.dataStore.data
            .catch { if (it is IOException) error("Error reading datastore") }
            .map(::mapSettings)

    override fun <T> get(block: Settings.() -> T): Flow<T> {
        return stream.map { it.block() }
    }


    override suspend fun setTheme(theme: Theme) {
        THEME.update(theme.name)
    }

    override suspend fun setGridLayout(layout: GridLayout) {
        GRID_LAYOUT.update(layout.name)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        DYNAMIC_COLOR.update(enabled)
    }

    private suspend inline fun <T> Preferences.Key<T>.update(value: T) {
        context.dataStore.edit { preference ->
            preference[this] = value
        }
    }

    private fun mapSettings(preferences: Preferences): Settings {
        val theme = preferences[THEME] ?: Theme.System.name
        val gridLayout = preferences[GRID_LAYOUT] ?: GridLayout.TwoColumns.name
        val dynamicColor = preferences[DYNAMIC_COLOR] ?: true



        return Settings(
            theme = Theme.valueOf(theme),
            gridLayout = GridLayout.valueOf(gridLayout),
            dynamicColor = dynamicColor
        )
    }


}