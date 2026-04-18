package com.mrhayami.vaultio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mrhayami.vaultio.ui.collection.GridSettings
import com.mrhayami.vaultio.ui.collection.ListSettings
import com.mrhayami.vaultio.ui.collection.PokedexSettings
import com.mrhayami.vaultio.ui.collection.SortMode
import com.mrhayami.vaultio.ui.collection.ViewMode
import kotlinx.coroutines.flow.Flow
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
        val SORT_MODE = stringPreferencesKey("sort_mode")
        
        val LIST_SHOW_PRICES = booleanPreferencesKey("list_show_prices")
        val LIST_IS_COMPACT = booleanPreferencesKey("list_is_compact")
        
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val GRID_SHOW_BADGES = booleanPreferencesKey("grid_show_badges")
        
        val POKEDEX_SHOW_UNCOLLECTED = booleanPreferencesKey("pokedex_show_uncollected")
        val POKEDEX_USE_SHINY_SPRITES = booleanPreferencesKey("pokedex_use_shiny_sprites")
        val POKEDEX_USE_OFFICIAL_ART = booleanPreferencesKey("pokedex_use_official_art")

        val THEME_BRAND = stringPreferencesKey("theme_brand")
        val DARK_THEME_CONFIG = stringPreferencesKey("dark_theme_config")
        
        val SHOW_ENERGY_ANIMATIONS = booleanPreferencesKey("show_energy_animations")
        val SHOW_FINISH_ANIMATIONS = booleanPreferencesKey("show_finish_animations")
        
        val JUST_TCG_API_KEY = stringPreferencesKey("just_tcg_api_key")
        val PREFER_SET_LOGO = booleanPreferencesKey("prefer_set_logo")
        
        val SHOULD_SHOW_WALKTHROUGH = booleanPreferencesKey("should_show_walkthrough")

        val BULK_SCAN_CONDITION = stringPreferencesKey("bulk_scan_condition")
        val BULK_SCAN_PRINTING = stringPreferencesKey("bulk_scan_printing")
        val BULK_SCAN_FINISH = stringPreferencesKey("bulk_scan_finish")
        val BULK_SCAN_FOLDER_IDS = stringPreferencesKey("bulk_scan_folder_ids")
    }

    val viewMode: Flow<ViewMode> = dataStore.data.map {
        val viewModeName = it[PreferencesKeys.VIEW_MODE] ?: ViewMode.GRID.name
        try { ViewMode.valueOf(viewModeName) } catch (e: Exception) { ViewMode.GRID }
    }

    suspend fun setViewMode(viewMode: ViewMode) {
        dataStore.edit {
            it[PreferencesKeys.VIEW_MODE] = viewMode.name
        }
    }

    val sortMode: Flow<SortMode> = dataStore.data.map {
        val sortModeName = it[PreferencesKeys.SORT_MODE] ?: SortMode.DATE_ADDED.name
        try { SortMode.valueOf(sortModeName) } catch (e: Exception) { SortMode.DATE_ADDED }
    }

    suspend fun setSortMode(sortMode: SortMode) {
        dataStore.edit {
            it[PreferencesKeys.SORT_MODE] = sortMode.name
        }
    }

    val listSettings: Flow<ListSettings> = dataStore.data.map {
        ListSettings(
            showPrices = it[PreferencesKeys.LIST_SHOW_PRICES] ?: true,
            isCompact = it[PreferencesKeys.LIST_IS_COMPACT] ?: false
        )
    }

    suspend fun setListSettings(settings: ListSettings) {
        dataStore.edit {
            it[PreferencesKeys.LIST_SHOW_PRICES] = settings.showPrices
            it[PreferencesKeys.LIST_IS_COMPACT] = settings.isCompact
        }
    }

    val gridSettings: Flow<GridSettings> = dataStore.data.map {
        GridSettings(
            columns = it[PreferencesKeys.GRID_COLUMNS] ?: 3,
            showBadges = it[PreferencesKeys.GRID_SHOW_BADGES] ?: true
        )
    }

    suspend fun setGridSettings(settings: GridSettings) {
        dataStore.edit {
            it[PreferencesKeys.GRID_COLUMNS] = settings.columns
            it[PreferencesKeys.GRID_SHOW_BADGES] = settings.showBadges
        }
    }

    val pokedexSettings: Flow<PokedexSettings> = dataStore.data.map {
        PokedexSettings(
            showUncollected = it[PreferencesKeys.POKEDEX_SHOW_UNCOLLECTED] ?: true,
            useShinySprites = it[PreferencesKeys.POKEDEX_USE_SHINY_SPRITES] ?: false,
            useOfficialArt = it[PreferencesKeys.POKEDEX_USE_OFFICIAL_ART] ?: false
        )
    }

    suspend fun setPokedexSettings(settings: PokedexSettings) {
        dataStore.edit {
            it[PreferencesKeys.POKEDEX_SHOW_UNCOLLECTED] = settings.showUncollected
            it[PreferencesKeys.POKEDEX_USE_SHINY_SPRITES] = settings.useShinySprites
            it[PreferencesKeys.POKEDEX_USE_OFFICIAL_ART] = settings.useOfficialArt
        }
    }

    val themeBrand = dataStore.data.map {
        val brandName = it[PreferencesKeys.THEME_BRAND] ?: ThemeBrand.DEFAULT.name
        try { ThemeBrand.valueOf(brandName) } catch (e: Exception) { ThemeBrand.DEFAULT }
    }

    suspend fun setThemeBrand(brand: ThemeBrand) {
        dataStore.edit {
            it[PreferencesKeys.THEME_BRAND] = brand.name
        }
    }

    val darkThemeConfig = dataStore.data.map {
        val configName = it[PreferencesKeys.DARK_THEME_CONFIG] ?: DarkThemeConfig.FOLLOW_SYSTEM.name
        try { DarkThemeConfig.valueOf(configName) } catch (e: Exception) { DarkThemeConfig.FOLLOW_SYSTEM }
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        dataStore.edit {
            it[PreferencesKeys.DARK_THEME_CONFIG] = config.name
        }
    }

    val showEnergyAnimations: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.SHOW_ENERGY_ANIMATIONS] ?: true
    }

    suspend fun setShowEnergyAnimations(show: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SHOW_ENERGY_ANIMATIONS] = show
        }
    }

    val showFinishAnimations: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.SHOW_FINISH_ANIMATIONS] ?: true
    }

    suspend fun setShowFinishAnimations(show: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SHOW_FINISH_ANIMATIONS] = show
        }
    }

    val justTcgApiKey: Flow<String?> = dataStore.data.map {
        it[PreferencesKeys.JUST_TCG_API_KEY]
    }

    suspend fun setJustTcgApiKey(apiKey: String) {
        dataStore.edit {
            it[PreferencesKeys.JUST_TCG_API_KEY] = apiKey
        }
    }

    val preferSetLogo: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.PREFER_SET_LOGO] ?: true
    }

    suspend fun setPreferSetLogo(preferLogo: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.PREFER_SET_LOGO] = preferLogo
        }
    }

    val shouldShowWalkthrough: Flow<Boolean> = dataStore.data.map {
        it[PreferencesKeys.SHOULD_SHOW_WALKTHROUGH] ?: true
    }

    suspend fun setShouldShowWalkthrough(show: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SHOULD_SHOW_WALKTHROUGH] = show
        }
    }

    val bulkScanCondition: Flow<String> = dataStore.data.map {
        it[PreferencesKeys.BULK_SCAN_CONDITION] ?: PricingUtils.CONDITION_NM
    }

    suspend fun setBulkScanCondition(condition: String) {
        dataStore.edit {
            it[PreferencesKeys.BULK_SCAN_CONDITION] = condition
        }
    }

    val bulkScanPrinting: Flow<String> = dataStore.data.map {
        it[PreferencesKeys.BULK_SCAN_PRINTING] ?: PricingUtils.PRINTING_UNLIMITED
    }

    suspend fun setBulkScanPrinting(printing: String) {
        dataStore.edit {
            it[PreferencesKeys.BULK_SCAN_PRINTING] = printing
        }
    }

    val bulkScanFinish: Flow<String> = dataStore.data.map {
        it[PreferencesKeys.BULK_SCAN_FINISH] ?: PricingUtils.FINISH_NORMAL
    }

    suspend fun setBulkScanFinish(finish: String) {
        dataStore.edit {
            it[PreferencesKeys.BULK_SCAN_FINISH] = finish
        }
    }

    val bulkScanFolderIds: Flow<List<Long>> = dataStore.data.map {
        it[PreferencesKeys.BULK_SCAN_FOLDER_IDS]?.split(",")?.mapNotNull { id -> id.toLongOrNull() } ?: emptyList()
    }

    suspend fun setBulkScanFolderIds(ids: List<Long>) {
        dataStore.edit {
            it[PreferencesKeys.BULK_SCAN_FOLDER_IDS] = ids.joinToString(",")
        }
    }
}
