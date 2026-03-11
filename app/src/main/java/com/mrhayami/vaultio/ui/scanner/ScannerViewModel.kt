package com.mrhayami.vaultio.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val detectedText: String = "",
    val detectedNumber: String? = null,
    val detectedTotal: String? = null,
    val detectedName: String? = null,
    val candidates: List<TcgDexCard> = emptyList(),
    val isSearching: Boolean = false,
    val isPaused: Boolean = false,
    val autoSelectedCard: TcgDexCard? = null,
    val errorMessage: String? = null
)

class ScannerViewModel(private val repository: VaultioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    // Pokemon Card Layout Analysis (normalized coordinates 0.0 - 1.0)
    // Name: Top (0% - 15% height)
    // Number: Bottom (85% - 100% height)
    
    private val numberRegex = Regex("""(\d+)/(\d+)|([A-Z]{1,2}\d{1,3})""")
    private val nameRegex = Regex("""\b[A-Z][a-z]{2,}\b""")

    private val noiseWords = setOf(
        "HP", "Stage", "Basic", "Level", "Weakness", "Resistance", "Retreat", 
        "Pokemon", "Dragon", "Pulse", "Spiral", "Burst", "Rapid", "Strike", 
        "Discard", "Energy", "Damage", "Rule", "Ability", "Trainer", "Item", 
        "Supporter", "Stadium", "Attack", "Vrule", "Knocked"
    )

    fun onLinesDetected(lines: List<DetectedLine>) {
        if (_uiState.value.isPaused || _uiState.value.isSearching) return

        var bestLocalId: String? = null
        var bestTotal: String? = null
        var bestName: String? = null

        // Group lines by region
        lines.forEach { line ->
            val box = line.boundingBox ?: return@forEach
            val centerY = box.centerY().toFloat() / line.imageHeight
            
            // 1. Region: Top (Card Name)
            if (centerY < 0.20f) {
                val nameMatch = nameRegex.find(line.text)
                if (nameMatch != null && nameMatch.value.uppercase() !in noiseWords.map { it.uppercase() }) {
                    if (bestName == null) bestName = nameMatch.value
                }
            }
            
            // 2. Region: Bottom (Card Number e.g. 194/203)
            if (centerY > 0.80f) {
                val numMatch = numberRegex.find(line.text)
                if (numMatch != null) {
                    val fullMatch = numMatch.value
                    if (fullMatch.contains("/")) {
                        val parts = fullMatch.split("/")
                        bestLocalId = parts.getOrNull(0)
                        bestTotal = parts.getOrNull(1)
                    } else {
                        bestLocalId = fullMatch
                    }
                }
            }
        }

        if (bestLocalId != null && (bestLocalId != _uiState.value.detectedNumber || bestTotal != _uiState.value.detectedTotal || bestName != _uiState.value.detectedName)) {
            _uiState.value = _uiState.value.copy(
                detectedNumber = bestLocalId,
                detectedTotal = bestTotal,
                detectedName = bestName,
                isSearching = true
            )
            searchCard(bestLocalId!!, bestTotal, bestName)
        }
    }

    private fun searchCard(localId: String, totalCount: String?, name: String?) {
        viewModelScope.launch {
            // Priority 1: Search local DB first for instant matches
            val localResults = repository.searchLocalCards(localId)
            
            // Convert local CardEntity to TcgDexCard for consistency in UI
            val localCandidates = localResults.map { entity ->
                TcgDexCard(
                    id = entity.id,
                    localId = entity.localId,
                    name = entity.name,
                    image = entity.image,
                    rarity = entity.rarity,
                    category = entity.category,
                    dexId = entity.dexId?.let { listOf(it.toInt()) }
                )
            }

            // Priority 2: Supplement with API results if needed
            val remoteResults = repository.searchTcgDexByLocalId(localId)
            
            val allCandidates = (localCandidates + remoteResults).distinctBy { it.id }
            
            // Filter by name and total count
            var filtered = allCandidates
            if (name != null) {
                filtered = filtered.filter { it.name.contains(name, ignoreCase = true) }
            }
            
            // Use the "Total" part of the number (e.g., 203) to find the specific set
            if (totalCount != null) {
                val totalInt = totalCount.toIntOrNull()
                // Most modern cards follow 'id = setid-localid'. 
                // We can't easily filter by set count without another API call or DB lookup,
                // but the combination of name + localId is usually unique.
            }

            if (filtered.size == 1 && (name != null || totalCount != null)) {
                _uiState.value = _uiState.value.copy(
                    autoSelectedCard = filtered.first(),
                    isSearching = false,
                    isPaused = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    candidates = filtered,
                    isSearching = false
                )
            }
        }
    }

    fun resumeScanning() {
        _uiState.value = _uiState.value.copy(
            isPaused = false, 
            autoSelectedCard = null, 
            detectedNumber = null,
            detectedTotal = null,
            detectedName = null,
            candidates = emptyList()
        )
    }

    fun saveScannedCard(card: TcgDexCard, quantity: Int, condition: String, printing: String, finish: String) {
        viewModelScope.launch {
            repository.addUserCard(
                card,
                UserCardEntity(
                    cardId = card.id,
                    quantity = quantity,
                    condition = condition,
                    printing = printing,
                    finish = finish
                )
            )
            resumeScanning()
        }
    }

    fun clearDetectedNumber() {
        _uiState.value = _uiState.value.copy(detectedNumber = null, detectedTotal = null, detectedName = null, candidates = emptyList())
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
