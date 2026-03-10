package com.mrhayami.vaultio.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val detectedText: String = "",
    val detectedNumber: String? = null,
    val candidates: List<TcgDexCard> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

class ScannerViewModel(private val repository: VaultioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val numberRegex = Regex("""(\d+)/(\d+)""")

    fun onTextDetected(text: String) {
        val match = numberRegex.find(text)
        val number = match?.value
        
        if (number != null && number != _uiState.value.detectedNumber) {
            _uiState.value = _uiState.value.copy(
                detectedText = text,
                detectedNumber = number,
                isSearching = true
            )
            searchCard(number)
        } else {
            _uiState.value = _uiState.value.copy(detectedText = text)
        }
    }

    private fun searchCard(number: String) {
        viewModelScope.launch {
            // In a real app, you'd check local DB first
            // val localMatches = repository.getCardsByNumber(number)
            
            // For this task, we'll use the API search
            val results = repository.searchTcgDex(number)
            _uiState.value = _uiState.value.copy(
                candidates = results,
                isSearching = false
            )
        }
    }

    fun clearDetectedNumber() {
        _uiState.value = _uiState.value.copy(detectedNumber = null, candidates = emptyList())
    }
}

class ScannerViewModelFactory(private val repository: VaultioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
