package com.mrhayami.vaultio.ui.settings

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand

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
    val isRefreshing: Boolean = false
)

sealed interface SettingsEvent {
    data class SetThemeBrand(val brand: ThemeBrand) : SettingsEvent
    data class SetDarkThemeConfig(val config: DarkThemeConfig) : SettingsEvent
    data class SetShowEnergyAnimations(val show: Boolean) : SettingsEvent
    data class SetShowFinishAnimations(val show: Boolean) : SettingsEvent
    data class SetPreferSetLogo(val preferLogo: Boolean) : SettingsEvent
    data class SetJustTcgApiKey(val apiKey: String) : SettingsEvent
    data object RefreshApiUsage : SettingsEvent
    data object ClearImageCache : SettingsEvent
    data object ResetSettings : SettingsEvent
}

sealed interface SettingsEffect {
    data class ShowMessage(val message: String) : SettingsEffect
}
