package com.mrhayami.vaultio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = "System",
    val apiUsage: Int = 0,
    val offlineSetsCount: Int = 0,
    val isLoading: Boolean = true
)

class SettingsViewModel(private val repository: VaultioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val usage = repository.getApiUsage()
            // In a real app, you'd collect from a flow or DataStore
            _uiState.value = SettingsUiState(
                apiUsage = usage,
                isLoading = false
            )
        }
    }

    fun setTheme(theme: String) {
        _uiState.value = _uiState.value.copy(theme = theme)
    }

    fun clearImageCache() {
        // TODO: Implement
    }

    fun resetSettings() {
        // TODO: Implement
    }
}

class SettingsViewModelFactory(private val repository: VaultioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
