package com.mrhayami.vaultio.ui.settings

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.data.update.AvailableUpdate
import com.mrhayami.vaultio.data.update.UpdateErrorKind

@Immutable
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
    val isRefreshing: Boolean = false,
    val autoUpdateEnabled: Boolean = false,
    val updaterSupported: Boolean = false,
    val isPlayInstall: Boolean = false,
    val updateCheckState: UpdateCheckUiState = UpdateCheckUiState.Idle,
    val pendingUpdate: AvailableUpdate? = null,
    val downloadProgress: Float? = null
)

sealed interface UpdateCheckUiState {
    data object Idle : UpdateCheckUiState
    data object Checking : UpdateCheckUiState
    data object UpToDate : UpdateCheckUiState
    data class Available(val tagName: String) : UpdateCheckUiState
    data class Downloading(val progress: Float?) : UpdateCheckUiState
    data class ReadyToInstall(val tagName: String) : UpdateCheckUiState
    data class Error(val kind: UpdateErrorKind, val message: String) : UpdateCheckUiState
}

sealed interface SettingsEvent {
    data class SetThemeBrand(val brand: ThemeBrand) : SettingsEvent
    data class SetDarkThemeConfig(val config: DarkThemeConfig) : SettingsEvent
    data class SetShowEnergyAnimations(val show: Boolean) : SettingsEvent
    data class SetShowFinishAnimations(val show: Boolean) : SettingsEvent
    data class SetPreferSetLogo(val preferLogo: Boolean) : SettingsEvent
    data class SetJustTcgApiKey(val apiKey: String) : SettingsEvent
    data class SetAutoUpdateEnabled(val enabled: Boolean) : SettingsEvent
    data object RefreshApiUsage : SettingsEvent
    data object ClearImageCache : SettingsEvent
    data object ResetSettings : SettingsEvent
    data object CheckForUpdates : SettingsEvent
    data object InstallUpdate : SettingsEvent
    data object ResumeInstallAfterUnknownSources : SettingsEvent
}

sealed interface SettingsEffect {
    data class ShowMessage(val message: String) : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data object OpenUnknownSourcesSettings : SettingsEffect
    data class LaunchInstall(val intent: android.content.Intent) : SettingsEffect
}
