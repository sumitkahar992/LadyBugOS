package com.example.ladybugos.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ladybugos.ui.theme.Settings
import com.example.ladybugos.ui.theme.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject


interface SettingsRepo {

    val stream: Flow<Settings>
    fun <T> get(block: Settings.() -> T): Flow<T>

    suspend fun setTheme(theme: Theme)
}


class DatastoreSettingsRepo @Inject constructor(
    private val context: Context
) : SettingsRepo {

    private companion object Keys {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings_preferences")

        val THEME: Preferences.Key<String> = stringPreferencesKey("theme")
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

    private suspend inline fun <T> Preferences.Key<T>.update(value: T) {
        context.dataStore.edit { preference ->
            preference[this] = value
        }
    }

    private fun mapSettings(preferences: Preferences): Settings {
        val theme = preferences[THEME] ?: Theme.System.name

        return Settings(
            theme = Theme.valueOf(theme)
        )
    }


}