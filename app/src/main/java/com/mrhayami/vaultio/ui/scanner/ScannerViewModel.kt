package com.mrhayami.vaultio.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScannerUiState(
    val detectedText: String = "",
    val detectedNumber: String? = null,
    val detectedTotal: String? = null,
    val detectedName: String? = null,
    val candidates: List<TcgDexCard> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val isSearching: Boolean = false,
    val isPaused: Boolean = false,
    val autoSelectedCard: TcgDexCard? = null,
    val showSaveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ScannerViewModel(private val repository: VaultioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = combine(
        _uiState,
        repository.allFolders
    ) { state, folders ->
        state.copy(folders = folders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScannerUiState())

    private var searchJob: Job? = null
    private var lastMatchedNumber: String? = null

    // Pokemon Card Layout Analysis
    private val numberRegex = Regex("""(\d{1,3})\s*/\s*(\d{1,3})""")
    
    private val noiseWords = setOf(
        "HP", "STAGE", "BASIC", "LEVEL", "WEAKNESS", "RESISTANCE", "RETREAT", 
        "POKEMON", "DRAGON", "PULSE", "SPIRAL", "BURST", "RAPID", "STRIKE", 
        "DISCARD", "ENERGY", "DAMAGE", "RULE", "ABILITY", "TRAINER", "ITEM", 
        "SUPPORTER", "STADIUM", "ATTACK", "VRULE", "KNOCKED"
    )

    fun onLinesDetected(lines: List<DetectedLine>) {
        if (_uiState.value.isPaused || _uiState.value.isSearching) return

        // Deep Dive Step 8: Inspect first three lines for name
        val topLines = lines.sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }.take(3)
        var bestName: String? = null
        for (line in topLines) {
            val cleaned = cleanCardName(line.text)
            if (cleaned.length >= 3) {
                bestName = cleaned
                break
            }
        }

        // Deep Dive Step 7: Collector-number extraction (slash pattern only)
        var bestLocalId: String? = null
        var bestTotal: String? = null
        
        // Search bottom 40% of the cropped frame for number
        val numberCandidates = lines.filter { (it.boundingBox?.centerY()?.toFloat() ?: 0f) / it.imageHeight > 0.60f }
        for (line in numberCandidates) {
            val normalizedText = normalizeOcrText(line.text)
            val numMatch = numberRegex.find(normalizedText)
            if (numMatch != null) {
                bestLocalId = numMatch.groupValues[1]
                bestTotal = numMatch.groupValues[2]
                break
            }
        }

        // Deep Dive: Rejects events whose numberText is the same as the last matched number
        if (bestLocalId != null && bestLocalId == lastMatchedNumber) return

        if (bestLocalId != null) {
            _uiState.update { it.copy(
                detectedNumber = bestLocalId,
                detectedTotal = bestTotal,
                detectedName = bestName,
                isSearching = true
            ) }
            
            // Debounce window (100ms) as per deep dive
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(100)
                searchCard(bestLocalId, bestTotal, bestName)
            }
        }
    }

    private fun normalizeOcrText(text: String): String {
        return text.replace("O", "0")
            .replace("o", "0")
            .replace("I", "1")
            .replace("l", "1")
            .replace("S", "5")
            .replace("s", "5")
            .replace("B", "8")
            .replace("G", "6")
            .replace("D", "0")
            .replace("Z", "2")
            .replace("b", "6")
            .replace("q", "9")
    }

    private fun cleanCardName(text: String): String {
        val upper = text.uppercase()
        // Reject lines that look like evolution boilerplate, HP, etc.
        if (noiseWords.any { upper.contains(it) }) {
            // If it contains a noise word, try to strip common prefixes
            val prefixes = listOf("BASIC", "STAGE 1", "STAGE 2", "LEVEL")
            var cleaned = upper
            prefixes.forEach { cleaned = cleaned.replace(it, "") }
            return cleaned.replace(Regex("[^A-Z ]"), "").trim()
        }
        return text.replace(Regex("[^A-Za-z0-9 ]"), "").trim()
    }

    private fun searchCard(localId: String, totalCount: String?, name: String?) {
        viewModelScope.launch {
            // Normalize for DB lookup (e.g., "5" -> "005")
            val numberNorm = localId.padStart(3, '0')
            
            // 1. Search Local DB (priority)
            val localResults = repository.searchLocalCards(numberNorm)
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

            // 2. Supplement with API
            val remoteResults = try {
                repository.searchTcgDexByLocalId(localId)
            } catch (e: Exception) {
                emptyList()
            }
            
            val allCandidates = (localCandidates + remoteResults).distinctBy { it.id }
            
            // 3. Strict Number + Name Filtering (Deep Dive Stage: Strict number plus name search)
            var filtered = allCandidates
            if (name != null) {
                val tokens = name.lowercase().split(" ").filter { it.length >= 3 }
                if (tokens.isNotEmpty()) {
                    filtered = allCandidates.filter { card ->
                        val cardNameLower = card.name.lowercase()
                        tokens.any { cardNameLower.contains(it) }
                    }
                }
            }
            
            // If name was provided and filtered out everything, we stick to that (prevent false positives)
            // unless we have high confidence in the number match and multiple candidates remain.
            
            // 4. Ranking (Similarity heuristic)
            if (filtered.size > 1 && name != null) {
                filtered = filtered.sortedByDescending { calculateSimilarity(it.name, name) }
            }

            // Check if we have a definitive match
            if (filtered.size == 1) {
                lastMatchedNumber = localId
                _uiState.update { it.copy(
                    autoSelectedCard = filtered.first(),
                    isSearching = false,
                    isPaused = true
                ) }
            } else {
                _uiState.update { it.copy(
                    candidates = filtered.take(5), // Limit to top 5
                    isSearching = false
                ) }
            }
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        val name1 = s1.lowercase()
        val name2 = s2.lowercase()
        if (name1.startsWith(name2) || name2.startsWith(name1)) return 0.9f
        
        // Simple word overlap similarity
        val words1 = name1.split(" ").toSet()
        val words2 = name2.split(" ").toSet()
        val intersection = words1.intersect(words2)
        return intersection.size.toFloat() / maxOf(words1.size, words2.size)
    }

    fun resumeScanning() {
        lastMatchedNumber = null
        _uiState.update { it.copy(
            isPaused = false, 
            autoSelectedCard = null, 
            detectedNumber = null,
            detectedTotal = null,
            detectedName = null,
            candidates = emptyList()
        ) }
    }

    fun saveScannedCard(card: TcgDexCard, quantity: Int, condition: String, printing: String, finish: String, folderIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            repository.addUserCard(
                card,
                UserCardEntity(
                    cardId = card.id,
                    quantity = quantity,
                    condition = condition,
                    printing = printing,
                    finish = finish
                ),
                folderIds = folderIds
            )
            _uiState.update { it.copy(showSaveSuccess = true) }
            // Don't go back, stay in scanner but clear state for next card
            resumeScanning()
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(showSaveSuccess = false) }
    }

    fun clearDetectedNumber() {
        lastMatchedNumber = null
        _uiState.update { it.copy(
            detectedNumber = null, 
            detectedTotal = null, 
            detectedName = null, 
            candidates = emptyList()
        ) }
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
