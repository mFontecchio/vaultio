package com.mrhayami.vaultio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mrhayami.vaultio.ui.collection.ViewMode
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val VIEW_MODE = stringPreferencesKey("view_mode")
    }

    val viewMode = dataStore.data.map {
        val viewModeName = it[PreferencesKeys.VIEW_MODE] ?: ViewMode.GRID.name
        ViewMode.valueOf(viewModeName)
    }

    suspend fun setViewMode(viewMode: ViewMode) {
        dataStore.edit {
            it[PreferencesKeys.VIEW_MODE] = viewMode.name
        }
    }
}