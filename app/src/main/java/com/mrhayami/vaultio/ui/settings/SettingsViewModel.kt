package com.mrhayami.vaultio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.ApiUsageEntity
import com.mrhayami.vaultio.data.repository.AppUpdateRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.data.update.AvailableUpdate
import com.mrhayami.vaultio.data.update.UpdateCheckResult
import com.mrhayami.vaultio.data.update.UpdateErrorKind
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val application: VaultioApplication
) : MviViewModel<SettingsUiState, SettingsEvent, SettingsEffect>(
    initialState = SettingsUiState(
        updaterSupported = appUpdateRepository.isUpdaterSupported(),
        isPlayInstall = appUpdateRepository.isPlayInstall()
    )
) {

    private val _isRefreshing = MutableStateFlow(false)
    private val _updateUi = MutableStateFlow(UpdateUiExtras())

    init {
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

        viewModelScope.launch {
            userPreferencesRepository.justTcgApiKey
                .distinctUntilChanged()
                .collect { apiKey ->
                    if (!apiKey.isNullOrEmpty()) {
                        refreshApiUsage()
                    }
                }
        }

        if (appUpdateRepository.hasVerifiedPendingApk()) {
            _updateUi.update {
                it.copy(
                    updateCheckState = UpdateCheckUiState.ReadyToInstall("pending"),
                )
            }
        }

        observeSettings()

        if (appUpdateRepository.isUpdaterSupported()) {
            viewModelScope.launch {
                maybeAutoCheck()
            }
        }
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
            val baseFlow = combine(
                prefsFlow,
                userPreferencesRepository.justTcgApiKey,
                repository.allSets.map { sets -> sets.count { it.isDownloaded } },
                repository.getApiUsageFlow(),
                _isRefreshing
            ) { prefs, apiKey, downloadedCount, usage, refreshing ->
                BaseSettingsSlice(prefs, apiKey, downloadedCount, usage, refreshing)
            }

            combine(
                baseFlow,
                userPreferencesRepository.autoUpdateEnabled,
                _updateUi
            ) { base, autoUpdate, updateUi ->
                SettingsUiState(
                    themeBrand = base.prefs.themeBrand,
                    darkThemeConfig = base.prefs.darkThemeConfig,
                    showEnergyAnimations = base.prefs.showEnergy,
                    showFinishAnimations = base.prefs.showFinish,
                    preferSetLogo = base.prefs.preferSetLogo,
                    justTcgApiKey = base.apiKey ?: "",
                    dailyUsed = base.usage?.count ?: 0,
                    dailyLimit = base.usage?.dailyLimit ?: 100,
                    dailyRemaining = base.usage?.dailyRemaining ?: 100,
                    planUsed = base.usage?.planUsed ?: 0,
                    planLimit = base.usage?.planLimit ?: 1000,
                    planRemaining = base.usage?.planRemaining ?: 1000,
                    planName = base.usage?.planName ?: "Free",
                    lastSyncedAt = base.usage?.lastSyncedAt ?: 0L,
                    offlineSetsCount = base.downloadedCount,
                    isLoading = false,
                    isRefreshing = base.refreshing,
                    autoUpdateEnabled = autoUpdate,
                    updaterSupported = appUpdateRepository.isUpdaterSupported(),
                    isPlayInstall = appUpdateRepository.isPlayInstall(),
                    updateCheckState = updateUi.updateCheckState,
                    pendingUpdate = updateUi.pendingUpdate,
                    downloadProgress = updateUi.downloadProgress
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
            is SettingsEvent.SetAutoUpdateEnabled -> setAutoUpdateEnabled(event.enabled)
            SettingsEvent.RefreshApiUsage -> refreshApiUsage()
            SettingsEvent.ClearImageCache -> clearImageCache()
            SettingsEvent.ResetSettings -> resetSettings()
            SettingsEvent.CheckForUpdates -> checkForUpdates(force = true)
            SettingsEvent.InstallUpdate -> installUpdate()
            SettingsEvent.ResumeInstallAfterUnknownSources -> installUpdate()
        }
    }

    private data class PreferenceValues(
        val themeBrand: ThemeBrand,
        val darkThemeConfig: DarkThemeConfig,
        val showEnergy: Boolean,
        val showFinish: Boolean,
        val preferSetLogo: Boolean
    )

    private data class BaseSettingsSlice(
        val prefs: PreferenceValues,
        val apiKey: String?,
        val downloadedCount: Int,
        val usage: ApiUsageEntity?,
        val refreshing: Boolean
    )

    private data class UpdateUiExtras(
        val updateCheckState: UpdateCheckUiState = UpdateCheckUiState.Idle,
        val pendingUpdate: AvailableUpdate? = null,
        val downloadProgress: Float? = null
    )

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

    private fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoUpdateEnabled(enabled)
            if (enabled) {
                application.scheduleAppUpdateChecks()
                emitEffect(SettingsEffect.RequestNotificationPermission)
            } else {
                application.cancelAppUpdateChecks()
            }
        }
    }

    private suspend fun maybeAutoCheck() {
        val autoEnabled = userPreferencesRepository.autoUpdateEnabled.firstOrNull() == true
        if (!autoEnabled) return
        checkForUpdates(force = false)
    }

    private fun checkForUpdates(force: Boolean) {
        if (!appUpdateRepository.isUpdaterSupported()) return
        viewModelScope.launch {
            _updateUi.update {
                it.copy(updateCheckState = UpdateCheckUiState.Checking, downloadProgress = null)
            }
            when (val result = appUpdateRepository.checkForUpdate(force = force)) {
                UpdateCheckResult.Unsupported -> {
                    _updateUi.update { it.copy(updateCheckState = UpdateCheckUiState.Idle) }
                }
                UpdateCheckResult.UpToDate -> {
                    _updateUi.update {
                        it.copy(
                            updateCheckState = UpdateCheckUiState.UpToDate,
                            pendingUpdate = null
                        )
                    }
                }
                UpdateCheckResult.NotModified -> {
                    if (appUpdateRepository.hasVerifiedPendingApk()) {
                        val tag = _updateUi.value.pendingUpdate?.tagName ?: "update"
                        _updateUi.update {
                            it.copy(updateCheckState = UpdateCheckUiState.ReadyToInstall(tag))
                        }
                    } else if (_updateUi.value.updateCheckState is UpdateCheckUiState.Idle) {
                        // keep idle on throttled open
                        _updateUi.update { it.copy(updateCheckState = UpdateCheckUiState.Idle) }
                    }
                }
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateUi.update {
                        it.copy(
                            updateCheckState = UpdateCheckUiState.Available(result.update.tagName),
                            pendingUpdate = result.update
                        )
                    }
                    downloadUpdate(result.update)
                }
                is UpdateCheckResult.Error -> {
                    _updateUi.update {
                        it.copy(
                            updateCheckState = UpdateCheckUiState.Error(result.kind, result.message)
                        )
                    }
                }
            }
        }
    }

    private fun downloadUpdate(update: AvailableUpdate) {
        viewModelScope.launch {
            _updateUi.update {
                it.copy(
                    updateCheckState = UpdateCheckUiState.Downloading(null),
                    pendingUpdate = update,
                    downloadProgress = null
                )
            }
            val result = appUpdateRepository.downloadUpdate(update) { bytesRead, contentLength ->
                val progress = if (contentLength > 0L) {
                    (bytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
                _updateUi.update {
                    it.copy(
                        updateCheckState = UpdateCheckUiState.Downloading(progress),
                        downloadProgress = progress
                    )
                }
            }
            if (result.isSuccess) {
                _updateUi.update {
                    it.copy(
                        updateCheckState = UpdateCheckUiState.ReadyToInstall(update.tagName),
                        downloadProgress = 1f
                    )
                }
            } else {
                val message = result.exceptionOrNull()?.message ?: "Download failed"
                val kind = if (
                    "certificate" in message.lowercase() ||
                    "package" in message.lowercase() ||
                    "downgrade" in message.lowercase()
                ) {
                    UpdateErrorKind.VerificationFailed
                } else {
                    UpdateErrorKind.Network
                }
                _updateUi.update {
                    it.copy(updateCheckState = UpdateCheckUiState.Error(kind, message))
                }
            }
        }
    }

    private fun installUpdate() {
        if (!appUpdateRepository.canInstallPackages()) {
            emitEffect(SettingsEffect.OpenUnknownSourcesSettings)
            return
        }
        val intent = appUpdateRepository.createInstallIntent()
        if (intent == null) {
            emitEffect(SettingsEffect.ShowMessage("Update file missing. Check for updates again."))
            _updateUi.update {
                it.copy(updateCheckState = UpdateCheckUiState.Idle, pendingUpdate = null)
            }
            return
        }
        emitEffect(SettingsEffect.LaunchInstall(intent))
    }

    private fun clearImageCache() {
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
            userPreferencesRepository.setAutoUpdateEnabled(false)
            userPreferencesRepository.setLastUpdateCheck(0L)
            userPreferencesRepository.setLastAcceptedReleaseIdentity(0L, 0L)
            userPreferencesRepository.setLastNotifiedReleaseIdentity(0L, 0L)
            userPreferencesRepository.setUpdateEtag(null)
            application.cancelAppUpdateChecks()
            _updateUi.value = UpdateUiExtras()
            emitEffect(SettingsEffect.ShowMessage("Settings reset to defaults"))
        }
    }
}

class SettingsViewModelFactory(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val application: VaultioApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                repository,
                userPreferencesRepository,
                appUpdateRepository,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
