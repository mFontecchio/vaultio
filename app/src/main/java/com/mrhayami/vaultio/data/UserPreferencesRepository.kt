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

enum class ThemeBrand {
    DEFAULT, GRASS, FIRE, WATER, ELECTRIC, PSYCHIC, FIGHTING, DARKNESS, STEEL, FAIRY, DRAGON
}

enum class DarkThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val THEME_BRAND = stringPreferencesKey("theme_brand")
        val DARK_THEME_CONFIG = stringPreferencesKey("dark_theme_config")
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

    val themeBrand = dataStore.data.map {
        val brandName = it[PreferencesKeys.THEME_BRAND] ?: ThemeBrand.DEFAULT.name
        ThemeBrand.valueOf(brandName)
    }

    suspend fun setThemeBrand(brand: ThemeBrand) {
        dataStore.edit {
            it[PreferencesKeys.THEME_BRAND] = brand.name
        }
    }

    val darkThemeConfig = dataStore.data.map {
        val configName = it[PreferencesKeys.DARK_THEME_CONFIG] ?: DarkThemeConfig.FOLLOW_SYSTEM.name
        DarkThemeConfig.valueOf(configName)
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        dataStore.edit {
            it[PreferencesKeys.DARK_THEME_CONFIG] = config.name
        }
    }
}