package com.mrhayami.vaultio.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** One frame's worth of extracted OCR data used by the consensus buffer. */
private data class FrameDetection(val number: String?, val total: String?, val name: String?)

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

    // Ring buffer storing the last 5 frame detections for multi-frame consensus.
    private val detectionHistory = ArrayDeque<FrameDetection>()

    // Pokemon Card Layout Analysis: Regex to capture "Number/Total" format
    private val numberRegex = Regex("""([A-Z0-9]{1,5})\s*/\s*([A-Z0-9]{1,5})""")
    
    private val noiseWords = setOf(
        "HP", "STAGE", "BASIC", "LEVEL", "WEAKNESS", "RESISTANCE", "RETREAT", 
        "POKEMON", "DRAGON", "PULSE", "SPIRAL", "BURST", "RAPID", "STRIKE", 
        "DISCARD", "ENERGY", "DAMAGE", "RULE", "ABILITY", "TRAINER", "ITEM", 
        "SUPPORTER", "STADIUM", "ATTACK", "VRULE", "KNOCKED", "VMAX", "VSTAR",
        "EX", "GX", "BREAK", "MEGA", "TAG TEAM", "PRISM", "RADIANT", "TERA"
    )

    fun onLinesDetected(lines: List<DetectedLine>) {
        if (_uiState.value.isPaused || _uiState.value.isSearching) return

        // --- Name extraction: restrict to top 25% of the frame ---
        val topLines = lines
            .filter { (it.boundingBox?.top?.toFloat() ?: Float.MAX_VALUE) / it.imageHeight < 0.25f }
            .sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
            .take(3)
        var bestName: String? = null
        for (line in topLines) {
            val cleaned = cleanCardName(line.text)
            if (cleaned.length >= 3) {
                bestName = cleaned
                break
            }
        }

        // --- Collector-number extraction: restrict to bottom 25% of the frame ---
        var bestLocalId: String? = null
        var bestTotal: String? = null
        val numberCandidates = lines.filter {
            (it.boundingBox?.centerY()?.toFloat() ?: 0f) / it.imageHeight > 0.75f
        }
        
        val combinedBottomText = numberCandidates.joinToString(" ") { it.text }
        val normalizedBottom = normalizeOcrText(combinedBottomText)
        val numMatch = numberRegex.find(normalizedBottom)
        
        if (numMatch != null) {
            bestLocalId = numMatch.groupValues[1]
            bestTotal = numMatch.groupValues[2]
        }

        // --- Multi-frame consensus: update ring buffer ---
        if (detectionHistory.size >= 5) detectionHistory.removeFirst()
        detectionHistory.addLast(FrameDetection(number = bestLocalId, total = bestTotal, name = bestName))

        if (bestLocalId == null) return
        
        // Consensus for Number & Total
        val consensusCount = detectionHistory.count { it.number == bestLocalId }
        if (consensusCount < 3) return

        val consensusTotal = detectionHistory
            .filter { it.number == bestLocalId }
            .mapNotNull { it.total }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: bestTotal

        val consensusName = detectionHistory
            .filter { it.number == bestLocalId }
            .mapNotNull { it.name }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: bestName

        // Skip re-searching a card that is already the active match.
        if (bestLocalId == lastMatchedNumber && consensusTotal == _uiState.value.detectedTotal) return

        _uiState.update { it.copy(
            detectedNumber = bestLocalId,
            detectedTotal = consensusTotal,
            detectedName = consensusName,
            isSearching = true
        ) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150)
            searchCard(bestLocalId!!, consensusTotal, consensusName)
        }
    }

    private fun searchCard(localId: String, totalCount: String?, name: String?) {
        viewModelScope.launch {
            val numberNorm = if (localId.all { it.isDigit() }) localId.padStart(3, '0') else localId
            
            // 1. HIGH ACCURACY LOCAL SEARCH: Number + Set Total
            var localResults = emptyList<CardEntity>()
            val totalInt = totalCount?.toIntOrNull()
            if (totalInt != null) {
                localResults = repository.searchLocalCardsWithTotal(numberNorm, totalInt)
            }
            
            // 2. FALLBACK LOCAL SEARCH: Just Number
            if (localResults.isEmpty()) {
                localResults = repository.searchLocalCards(numberNorm)
            }

            // 3. Name Filtering on Local Results
            if (name != null && localResults.size > 1) {
                val filtered = localResults.filter { card ->
                    calculateSimilarity(card.name, name) > 0.6f
                }
                if (filtered.isNotEmpty()) localResults = filtered
            }

            // 4. API SEARCH: ONLY if local results are inconclusive or empty
            val finalCandidates = if (localResults.size == 1) {
                // High confidence local match found - STOP HERE to avoid redundant API calls
                localResults.map { mapToTcgDexCard(it) }
            } else {
                // Inconclusive local data, supplement with API
                val remoteResults = try {
                    repository.searchTcgDexByLocalId(localId)
                } catch (e: Exception) {
                    emptyList()
                }
                
                // Combine and de-duplicate
                val all = (localResults.map { mapToTcgDexCard(it) } + remoteResults).distinctBy { it.id }
                
                // Final filtering by Total and Name
                var filtered = all
                if (totalInt != null) {
                    // Note: TCGdex API doesn't always return set total in the search list, 
                    // so we filter by ID prefix if possible or just use name fuzzy matching
                    filtered = all.filter { card ->
                        val cardTotal = card.id.substringAfterLast("-").toIntOrNull()
                        cardTotal == null || cardTotal == totalInt
                    }
                    if (filtered.isEmpty()) filtered = all // fallback if filtering was too aggressive
                }
                
                if (name != null && filtered.size > 1) {
                    filtered = filtered.sortedByDescending { calculateSimilarity(it.name, name) }
                }
                filtered
            }

            // 5. Update UI
            if (finalCandidates.size == 1) {
                lastMatchedNumber = localId
                _uiState.update { it.copy(
                    autoSelectedCard = finalCandidates.first(),
                    isSearching = false,
                    isPaused = true
                ) }
            } else {
                _uiState.update { it.copy(
                    candidates = finalCandidates.take(5),
                    isSearching = false
                ) }
            }
        }
    }

    private fun mapToTcgDexCard(entity: CardEntity) = TcgDexCard(
        id = entity.id,
        localId = entity.localId,
        name = entity.name,
        image = entity.image,
        rarity = entity.rarity,
        category = entity.category,
        dexId = entity.dexId?.let { listOf(it.toInt()) }
    )

    private fun normalizeOcrText(text: String): String {
        return text.split(Regex("\\s+")).joinToString(" ") { token ->
            if (token.any { it.isDigit() || it == '/' || it == 'I' || it == 'l' || it == 'O' || it == 'o' }) {
                token.uppercase()
                    .replace("O", "0")
                    .replace("I", "1")
                    .replace("L", "1")
                    .replace("S", "5")
                    .replace("B", "8")
                    .replace("G", "6")
                    .replace("D", "0")
                    .replace("Z", "2")
                    .replace("b", "6")
                    .replace("q", "9")
            } else {
                token
            }
        }
    }

    private fun cleanCardName(text: String): String {
        val upper = text.uppercase()
        if (noiseWords.contains(upper.trim())) return ""
        val prefixes = listOf("BASIC", "STAGE 1", "STAGE 2", "LEVEL")
        var cleaned = upper
        prefixes.forEach { cleaned = cleaned.replace(it, "") }
        val result = cleaned.replace(Regex("[^A-Z0-9 ]"), "").trim()
        if (result.all { it.isDigit() } || noiseWords.contains(result)) return ""
        return result
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        val name1 = s1.lowercase()
        val name2 = s2.lowercase()
        if (name1 == name2) return 1.0f
        if (name1.startsWith(name2) || name2.startsWith(name1)) return 0.95f
        val maxLen = maxOf(name1.length, name2.length)
        if (maxLen == 0) return 1.0f
        return 1f - levenshteinDistance(name1, name2).toFloat() / maxLen
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                curr[j] = if (s1[i - 1] == s2[j - 1]) {
                    prev[j - 1]
                } else {
                    1 + minOf(prev[j], curr[j - 1], prev[j - 1])
                }
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[n]
    }

    fun resumeScanning() {
        lastMatchedNumber = null
        detectionHistory.clear()
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
            resumeScanning()
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(showSaveSuccess = false) }
    }

    fun clearDetectedNumber() {
        lastMatchedNumber = null
        detectionHistory.clear()
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
