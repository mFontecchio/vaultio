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

/** One frame's worth of extracted OCR data used by the consensus buffer. */
private data class FrameDetection(val number: String?, val name: String?)

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

    // Ring buffer storing the last 3 frame detections for multi-frame consensus.
    // A collector number must appear in ≥ 2 of the last 3 frames before triggering
    // a search — eliminates false positives from noisy single-frame reads.
    private val detectionHistory = ArrayDeque<FrameDetection>()

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

        // --- Name extraction: restrict to top 25% of the (uncropped) frame ---
        // Sorting by top position then capping the search zone prevents mid-card
        // noise lines (HP, attack text) from polluting the card name slot.
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
        // Tightened from 0.60 → 0.75 to exclude mid-card numbers (HP, attack damage)
        // that can generate phantom collector-number matches.
        var bestLocalId: String? = null
        var bestTotal: String? = null
        val numberCandidates = lines.filter {
            (it.boundingBox?.centerY()?.toFloat() ?: 0f) / it.imageHeight > 0.75f
        }
        for (line in numberCandidates) {
            val normalizedText = normalizeOcrText(line.text)
            val numMatch = numberRegex.find(normalizedText)
            if (numMatch != null) {
                bestLocalId = numMatch.groupValues[1]
                bestTotal = numMatch.groupValues[2]
                break
            }
        }

        // --- Multi-frame consensus: update ring buffer (max 3 entries) ---
        if (detectionHistory.size >= 3) detectionHistory.removeFirst()
        detectionHistory.addLast(FrameDetection(number = bestLocalId, name = bestName))

        // Require the same collector number in ≥ 2 of the last 3 frames before
        // committing to a search. Single noisy frames are silently discarded.
        if (bestLocalId == null) return
        val consensusCount = detectionHistory.count { it.number == bestLocalId }
        if (consensusCount < 2) return

        // Use the name seen most frequently across the consensus frames for the
        // best signal-to-noise ratio before feeding it into the similarity scorer.
        val consensusName = detectionHistory
            .filter { it.number == bestLocalId }
            .mapNotNull { it.name }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: bestName

        // Skip re-searching a card that is already the active match.
        if (bestLocalId == lastMatchedNumber) return

        _uiState.update { it.copy(
            detectedNumber = bestLocalId,
            detectedTotal = bestTotal,
            detectedName = consensusName,
            isSearching = true
        ) }

        // Debounce: cancel any in-flight job and wait 100ms for the frame stream
        // to settle before hitting the DB / network.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(100)
            searchCard(bestLocalId, bestTotal, consensusName)
        }
    }

    private fun normalizeOcrText(text: String): String {
        // Apply digit-substitution only on tokens that already contain a real digit or
        // slash. This prevents pure-letter tokens (e.g., "BASIC" → "8A51C") from
        // accidentally constructing a slash-pattern that matches the number regex.
        return text.split(Regex("\\s+")).joinToString(" ") { token ->
            if (token.any { c -> c.isDigit() || c == '/' }) {
                token
                    .replace("O", "0").replace("o", "0")
                    .replace("I", "1").replace("l", "1")
                    .replace("S", "5").replace("s", "5")
                    .replace("B", "8").replace("G", "6")
                    .replace("D", "0").replace("Z", "2")
                    .replace("b", "6").replace("q", "9")
            } else {
                token
            }
        }
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

    /**
     * Normalized Levenshtein similarity in [0, 1].
     * Handles single-character OCR misreads (e.g., "Pikachu" → "Plkachu") far better
     * than word-set intersection, which would score such a pair at 0.
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val name1 = s1.lowercase()
        val name2 = s2.lowercase()
        // Fast path: exact or prefix match
        if (name1 == name2) return 1.0f
        if (name1.startsWith(name2) || name2.startsWith(name1)) return 0.95f
        val maxLen = maxOf(name1.length, name2.length)
        if (maxLen == 0) return 1.0f
        return 1f - levenshteinDistance(name1, name2).toFloat() / maxLen
    }

    /** Standard dynamic-programming Levenshtein edit distance. */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        // Use two rolling rows to keep memory O(n) instead of O(m*n).
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
            // Don't go back, stay in scanner but clear state for next card
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
