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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeBrand: ThemeBrand = ThemeBrand.DEFAULT,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val showEnergyAnimations: Boolean = true,
    val showFinishAnimations: Boolean = true,
    val preferSetLogo: Boolean = true,
    val justTcgApiKey: String = "",
    val dailyUsed: Int = 0,
    val dailyLimit: Int = 100,
    val dailyRemaining: Int = 100,
    val planUsed: Int = 0,
    val planLimit: Int = 1000,
    val planRemaining: Int = 1000,
    val planName: String = "Free",
    val lastSyncedAt: Long = 0L,
    val offlineSetsCount: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class SettingsViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    init {
        // Silently revalidate quota on screen open if key is set and data is stale (> 5 min)
        viewModelScope.launch {
            val apiKey = userPreferencesRepository.justTcgApiKey.firstOrNull()
            if (!apiKey.isNullOrEmpty()) {
                val lastSync = repository.getApiUsageDetails()?.lastSyncedAt ?: 0L
                if (System.currentTimeMillis() - lastSync > 5 * 60_000L) {
                    _isRefreshing.value = true
                    repository.refreshApiUsageFromApi()
                    _isRefreshing.value = false
                }
            }
        }
    }

    private data class PreferenceValues(
        val themeBrand: ThemeBrand,
        val darkThemeConfig: DarkThemeConfig,
        val showEnergy: Boolean,
        val showFinish: Boolean,
        val preferSetLogo: Boolean
    )

    // Grouped so the outer combine stays within the typed 5-flow overload
    private val prefsFlow: Flow<PreferenceValues> = combine(
        userPreferencesRepository.themeBrand,
        userPreferencesRepository.darkThemeConfig,
        userPreferencesRepository.showEnergyAnimations,
        userPreferencesRepository.showFinishAnimations,
        userPreferencesRepository.preferSetLogo
    ) { themeBrand, darkThemeConfig, showEnergy, showFinish, preferSetLogo ->
        PreferenceValues(themeBrand, darkThemeConfig, showEnergy, showFinish, preferSetLogo)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
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
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    /** Manual refresh — hits GET /health which doesn't consume a request. */
    fun refreshApiUsage() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshApiUsageFromApi()
            } catch (e: Exception) {
                // Potential error handling
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setThemeBrand(brand: ThemeBrand) {
        viewModelScope.launch { userPreferencesRepository.setThemeBrand(brand) }
    }

    fun setDarkThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch { userPreferencesRepository.setDarkThemeConfig(config) }
    }

    fun setShowEnergyAnimations(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowEnergyAnimations(show) }
    }

    fun setShowFinishAnimations(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowFinishAnimations(show) }
    }

    fun setPreferSetLogo(preferLogo: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPreferSetLogo(preferLogo) }
    }

    fun setJustTcgApiKey(apiKey: String) {
        viewModelScope.launch { userPreferencesRepository.setJustTcgApiKey(apiKey) }
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
