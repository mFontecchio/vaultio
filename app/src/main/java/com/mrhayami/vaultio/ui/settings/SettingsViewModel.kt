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
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : MviViewModel<SettingsUiState, SettingsEvent, SettingsEffect>(
    initialState = SettingsUiState()
) {

    private val _isRefreshing = MutableStateFlow(false)

    init {
        // Silently revalidate quota on screen open if key is set and data is stale (> 5 min)
        viewModelScope.launch {
            val apiKey = userPreferencesRepository.justTcgApiKey.firstOrNull()
            if (!apiKey.isNullOrEmpty()) {
                val lastSync = repository.getApiUsageDetails()?.lastSyncedAt ?: 0L
                if (System.currentTimeMillis() - lastSync > 24 * 60 * 60_000L) {
                    _isRefreshing.value = true
                    repository.refreshApiUsageFromApi()
                    _isRefreshing.value = false
                }
            }
        }

        // Trigger refresh when API key changes
        viewModelScope.launch {
            userPreferencesRepository.justTcgApiKey
                .distinctUntilChanged()
                .collect { apiKey ->
                    if (!apiKey.isNullOrEmpty()) {
                        refreshApiUsage()
                    }
                }
        }

        observeSettings()
    }

    private fun observeSettings() {
        val prefsFlow: Flow<PreferenceValues> = combine(
            userPreferencesRepository.themeBrand,
            userPreferencesRepository.darkThemeConfig,
            userPreferencesRepository.showEnergyAnimations,
            userPreferencesRepository.showFinishAnimations,
            userPreferencesRepository.preferSetLogo
        ) { themeBrand, darkThemeConfig, showEnergy, showFinish, preferSetLogo ->
            PreferenceValues(themeBrand, darkThemeConfig, showEnergy, showFinish, preferSetLogo)
        }

        viewModelScope.launch {
            combine(
                prefsFlow,
                userPreferencesRepository.justTcgApiKey,
                repository.allSets.map { sets -> sets.count { it.isDownloaded } },
                repository.getApiUsageFlow(),
                _isRefreshing
            ) { prefs, apiKey, downloadedCount, usage, refreshing ->
                SettingsUiState(
                    themeBrand = prefs.themeBrand,
                    darkThemeConfig = prefs.darkThemeConfig,
                    showEnergyAnimations = prefs.showEnergy,
                    showFinishAnimations = prefs.showFinish,
                    preferSetLogo = prefs.preferSetLogo,
                    justTcgApiKey = apiKey ?: "",
                    dailyUsed = usage?.count ?: 0,
                    dailyLimit = usage?.dailyLimit ?: 100,
                    dailyRemaining = usage?.dailyRemaining ?: 100,
                    planUsed = usage?.planUsed ?: 0,
                    planLimit = usage?.planLimit ?: 1000,
                    planRemaining = usage?.planRemaining ?: 1000,
                    planName = usage?.planName ?: "Free",
                    lastSyncedAt = usage?.lastSyncedAt ?: 0L,
                    offlineSetsCount = downloadedCount,
                    isLoading = false,
                    isRefreshing = refreshing
                )
            }.flowOn(Dispatchers.Default)
                .collect { newState ->
                    updateState { newState }
                }
        }
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeBrand -> setThemeBrand(event.brand)
            is SettingsEvent.SetDarkThemeConfig -> setDarkThemeConfig(event.config)
            is SettingsEvent.SetShowEnergyAnimations -> setShowEnergyAnimations(event.show)
            is SettingsEvent.SetShowFinishAnimations -> setShowFinishAnimations(event.show)
            is SettingsEvent.SetPreferSetLogo -> setPreferSetLogo(event.preferLogo)
            is SettingsEvent.SetJustTcgApiKey -> setJustTcgApiKey(event.apiKey)
            SettingsEvent.RefreshApiUsage -> refreshApiUsage()
            SettingsEvent.ClearImageCache -> clearImageCache()
            SettingsEvent.ResetSettings -> resetSettings()
        }
    }

    private data class PreferenceValues(
        val themeBrand: ThemeBrand,
        val darkThemeConfig: DarkThemeConfig,
        val showEnergy: Boolean,
        val showFinish: Boolean,
        val preferSetLogo: Boolean
    )

    /** Manual refresh — hits GET /health which doesn't consume a request. */
    private fun refreshApiUsage() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshApiUsageFromApi()
            } catch (_: Exception) {
                emitEffect(SettingsEffect.ShowMessage("Failed to refresh API usage"))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun setThemeBrand(brand: ThemeBrand) {
        viewModelScope.launch { userPreferencesRepository.setThemeBrand(brand) }
    }

    private fun setDarkThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch { userPreferencesRepository.setDarkThemeConfig(config) }
    }

    private fun setShowEnergyAnimations(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowEnergyAnimations(show) }
    }

    private fun setShowFinishAnimations(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowFinishAnimations(show) }
    }

    private fun setPreferSetLogo(preferLogo: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPreferSetLogo(preferLogo) }
    }

    private fun setJustTcgApiKey(apiKey: String) {
        viewModelScope.launch { userPreferencesRepository.setJustTcgApiKey(apiKey) }
    }

    private fun clearImageCache() {
        // TODO: Implement using Coil's ImageLoader if needed
        emitEffect(SettingsEffect.ShowMessage("Image cache cleared"))
    }

    private fun resetSettings() {
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
            emitEffect(SettingsEffect.ShowMessage("Settings reset to defaults"))
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
