package com.mrhayami.vaultio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.collection.GridSettings
import com.mrhayami.vaultio.ui.collection.ListSettings
import com.mrhayami.vaultio.ui.collection.PokedexSettings
import com.mrhayami.vaultio.ui.collection.SortMode
import com.mrhayami.vaultio.ui.collection.ViewMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeBrand: ThemeBrand = ThemeBrand.DEFAULT,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val apiUsage: Int = 0,
    val offlineSetsCount: Int = 0,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.themeBrand,
        userPreferencesRepository.darkThemeConfig,
        repository.allSets.map { sets -> sets.count { it.isDownloaded } }
    ) { themeBrand, darkThemeConfig, downloadedCount ->
        SettingsUiState(
            themeBrand = themeBrand,
            darkThemeConfig = darkThemeConfig,
            apiUsage = repository.getApiUsage(),
            offlineSetsCount = downloadedCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeBrand(brand: ThemeBrand) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeBrand(brand)
        }
    }

    fun setDarkThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkThemeConfig(config)
        }
    }

    fun clearImageCache() {
        // TODO: Implement using Coil's ImageLoader if needed
    }

    fun resetSettings() {
        viewModelScope.launch {
            userPreferencesRepository.setThemeBrand(ThemeBrand.DEFAULT)
            userPreferencesRepository.setDarkThemeConfig(DarkThemeConfig.FOLLOW_SYSTEM)
            userPreferencesRepository.setViewMode(ViewMode.GRID)
            userPreferencesRepository.setSortMode(SortMode.DATE_ADDED)
            userPreferencesRepository.setListSettings(ListSettings())
            userPreferencesRepository.setGridSettings(GridSettings())
            userPreferencesRepository.setPokedexSettings(PokedexSettings())
        }
    }
}

class SettingsViewModelFactory(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
