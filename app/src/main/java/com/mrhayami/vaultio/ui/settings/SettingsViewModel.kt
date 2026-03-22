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
    val showEnergyAnimations: Boolean = true,
    val showFinishAnimations: Boolean = true,
    val preferSetLogo: Boolean = true,
    val justTcgApiKey: String = "",
    val apiUsage: Int = 0,
    val offlineSetsCount: Int = 0,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private data class PreferenceValues(
        val themeBrand: ThemeBrand,
        val darkThemeConfig: DarkThemeConfig,
        val showEnergy: Boolean,
        val showFinish: Boolean,
        val preferSetLogo: Boolean
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            userPreferencesRepository.themeBrand,
            userPreferencesRepository.darkThemeConfig,
            userPreferencesRepository.showEnergyAnimations,
            userPreferencesRepository.showFinishAnimations,
            userPreferencesRepository.preferSetLogo
        ) { themeBrand, darkThemeConfig, showEnergy, showFinish, preferSetLogo ->
            PreferenceValues(themeBrand, darkThemeConfig, showEnergy, showFinish, preferSetLogo)
        },
        userPreferencesRepository.justTcgApiKey,
        repository.allSets.map { sets -> sets.count { it.isDownloaded } }
    ) { prefs, apiKey, downloadedCount ->
        SettingsUiState(
            themeBrand = prefs.themeBrand,
            darkThemeConfig = prefs.darkThemeConfig,
            showEnergyAnimations = prefs.showEnergy,
            showFinishAnimations = prefs.showFinish,
            preferSetLogo = prefs.preferSetLogo,
            justTcgApiKey = apiKey ?: "",
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

    fun setShowEnergyAnimations(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowEnergyAnimations(show)
        }
    }

    fun setShowFinishAnimations(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowFinishAnimations(show)
        }
    }

    fun setPreferSetLogo(preferLogo: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferSetLogo(preferLogo)
        }
    }

    fun setJustTcgApiKey(apiKey: String) {
        viewModelScope.launch {
            userPreferencesRepository.setJustTcgApiKey(apiKey)
        }
    }

    fun clearImageCache() {
        // TODO: Implement using Coil's ImageLoader if needed
    }

    fun resetSettings() {
        viewModelScope.launch {
            userPreferencesRepository.setThemeBrand(ThemeBrand.DEFAULT)
            userPreferencesRepository.setDarkThemeConfig(DarkThemeConfig.FOLLOW_SYSTEM)
            userPreferencesRepository.setShowEnergyAnimations(true)
            userPreferencesRepository.setShowFinishAnimations(true)
            userPreferencesRepository.setPreferSetLogo(true)
            userPreferencesRepository.setJustTcgApiKey("")
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
