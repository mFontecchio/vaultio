package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.PHash
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One frame's worth of extracted OCR data used by the consensus buffer. */
private data class FrameDetection(val number: String?, val total: String?, val name: String?, val pHash: Long? = null)

@Immutable
data class ScannerUiState(
    val autoCaptureTrigger: Long = 0L,
    val detectedText: String = "",
    val detectedNumber: String? = null,
    val detectedTotal: String? = null,
    val detectedName: String? = null,
    val candidates: ImmutableList<TcgDexCard> = persistentListOf(),
    val folders: ImmutableList<FolderEntity> = persistentListOf(),
    val isSearching: Boolean = false,
    val isPaused: Boolean = false,
    val autoSelectedCard: TcgDexCard? = null,
    val selectedCard: TcgDexCard? = null,
    val showSaveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val hasCameraPermission: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val activeMode: ScannerMode = ScannerMode.IDLE,
    val pageScanMode: PageScanMode = PageScanMode.IDLE,
    val pageScanCells: List<PageScanCell> = emptyList(),
    val bulkDefaults: BulkScanDefaults = BulkScanDefaults(),
    val bulkSessionLog: ImmutableList<BulkScanEntry> = persistentListOf(),
    val skippedCards: ImmutableList<TcgDexCard> = persistentListOf(),
    val priceCheckInfo: PriceCheckInfo? = null,
    val isSaving: Boolean = false,
    val targetUserCardId: Long? = null,
    val isGlareDetected: Boolean = false,
    val exposureIndex: Int = 0,
    val exposureRange: IntRange = 0..0,
    val isExposureSupported: Boolean = false,
    val focusPoint: Offset? = null,
    val recognizerUnavailable: Boolean = false,
    val isCatalogEmpty: Boolean = false,
    val showEmptyCatalogHint: Boolean = false
) {
    val isBulkMode: Boolean get() = activeMode == ScannerMode.BULK
    val isGradingMode: Boolean get() = activeMode == ScannerMode.GRADING
    val isPageScanMode: Boolean get() = activeMode == ScannerMode.PAGE
    val isPriceCheckMode: Boolean get() = activeMode == ScannerMode.PRICE_CHECK
}

sealed interface ScannerEffect {
    data class NavigateToGrading(
        val userCardId: Long,
        val capturedImage: Bitmap? = null,
        val pendingCard: TcgDexCard? = null
    ) : ScannerEffect

    data object NavigateToSetDownloads : ScannerEffect
}

sealed interface ScannerEvent {
    data class SetTargetUserCard(val userCardId: Long) : ScannerEvent
    data object ResumeScanning : ScannerEvent
    data object ClearDetectedNumber : ScannerEvent
    data object ConsumeSaveSuccess : ScannerEvent
    data class CardSelected(val card: TcgDexCard?) : ScannerEvent
    data class PermissionResult(val granted: Boolean) : ScannerEvent
    data object ToggleTorch : ScannerEvent
    data class LinesDetected(
        val lines: List<DetectedLine>,
        val pHash: Long?,
        val isGlareDetected: Boolean = false
    ) : ScannerEvent
    data class SelectMode(val mode: ScannerMode) : ScannerEvent
    data class CapturePhoto(val bitmap: Bitmap) : ScannerEvent
    data class ConfirmPageCell(val index: Int, val card: TcgDexCard?) : ScannerEvent
    data class RejectPageCell(val index: Int) : ScannerEvent
    data object SaveAllPageResults : ScannerEvent
    data object RetryPageScan : ScannerEvent
    data class SetBulkDefaults(val defaults: BulkScanDefaults) : ScannerEvent
    data object UndoLastBulkScan : ScannerEvent
    data object ClearBulkSession : ScannerEvent
    data class AdjustExposure(val index: Int) : ScannerEvent
    data class SetExposureState(val range: IntRange, val current: Int, val supported: Boolean) :
        ScannerEvent

    data class TapToFocus(val point: Offset) : ScannerEvent
    data object ClearFocus : ScannerEvent
    data class ConfirmSkippedCard(
        val card: TcgDexCard,
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String,
        val folderIds: List<Long>
    ) : ScannerEvent
    data class SaveScannedCard(
        val card: TcgDexCard,
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String,
        val folderIds: List<Long>
    ) : ScannerEvent
    data class SaveAndGrade(
        val card: TcgDexCard,
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String,
        val folderIds: List<Long>
    ) : ScannerEvent

    data object RecognizerUnavailable : ScannerEvent
    data object DismissEmptyCatalogHint : ScannerEvent
    data object OpenSetDownloads : ScannerEvent
    data object ClearErrorMessage : ScannerEvent
}

class ScannerViewModel(
    private val repository: VaultioRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = combine(
        _uiState,
        repository.allFolders,
        repository.userPreferencesRepository.bulkScanCondition,
        repository.userPreferencesRepository.bulkScanPrinting,
        repository.userPreferencesRepository.bulkScanFinish,
        repository.userPreferencesRepository.bulkScanFolderIds
    ) { args ->
        val state = args[0] as ScannerUiState
        val folders = args[1] as List<FolderEntity>
        val condition = args[2] as String
        val printing = args[3] as String
        val finish = args[4] as String
        val folderIds = args[5] as List<Long>

        state.copy(
            folders = folders.toImmutableList(),
            bulkDefaults = state.bulkDefaults.copy(
                condition = condition,
                printing = printing,
                finish = finish,
                folderIds = folderIds.toImmutableList()
            )
        )
    }.flowOn(defaultDispatcher)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScannerUiState())

    private val _effect = Channel<ScannerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var searchJob: Job? = null
    private var lastMatchedNumber: String? = null
    private var activeCaptureForGrading: Bitmap? = null
    private var emptyCatalogHintDismissed = false

    // Ring buffer storing the last 5 frame detections for multi-frame consensus.
    private val detectionHistory = ArrayDeque<FrameDetection>()

    init {
        viewModelScope.launch(defaultDispatcher) {
            repository.observeCatalogCardCount().collect { count ->
                val empty = count == 0
                _uiState.update {
                    it.copy(
                        isCatalogEmpty = empty,
                        showEmptyCatalogHint = empty && !emptyCatalogHintDismissed
                    )
                }
            }
        }
    }

    // Pokemon Card Layout Analysis: Regex to capture varied collector number formats:
    // 1. "Number / Total" (e.g. 123 / 191) - Primary format
    private val numberTotalRegex = Regex("""([A-Z0-9]{1,5})\s*/\s*([A-Z0-9]{1,5})""")

    // 2. Set Prefix + Number (e.g. SWSH123 or XY 123) - Secondary format
    private val setNumberRegex = Regex("""([A-Z]{1,4})\s*(\d{1,4})""")

    // 3. Fallback: Standalone number (3-5 digits) - Tertiary format
    private val standaloneNumberRegex = Regex("""(\d{3,5})""")
    
    private val noiseWords = setOf(
        "HP", "STAGE", "BASIC", "LEVEL", "WEAKNESS", "RESISTANCE", "RETREAT", 
        "POKEMON", "DRAGON", "PULSE", "SPIRAL", "BURST", "RAPID", "STRIKE", 
        "DISCARD", "ENERGY", "DAMAGE", "RULE", "ABILITY", "TRAINER", "ITEM", 
        "SUPPORTER", "STADIUM", "ATTACK", "VRULE", "KNOCKED", "VMAX", "VSTAR",
        "EX", "GX", "BREAK", "MEGA", "TAG TEAM", "PRISM", "RADIANT", "TERA"
    )

    fun onEvent(event: ScannerEvent) {
        when (event) {
            is ScannerEvent.SetTargetUserCard -> {
                _uiState.update { it.copy(targetUserCardId = event.userCardId) }
                if (event.userCardId != -1L) {
                    viewModelScope.launch(defaultDispatcher) {
                        val userCard = repository.getUserCardByIdSync(event.userCardId)
                        if (userCard != null) {
                            val card = userCard.card
                            val tcgDexCard = mapToTcgDexCard(card)
                            _uiState.update { state ->
                                state.copy(
                                    activeMode = ScannerMode.GRADING,
                                    autoSelectedCard = tcgDexCard,
                                    isPaused = true,
                                    isSearching = false,
                                    autoCaptureTrigger = System.currentTimeMillis()
                                )
                            }
                        }
                    }
                }
            }
            is ScannerEvent.LinesDetected -> onLinesDetected(
                event.lines,
                event.pHash,
                event.isGlareDetected
            )
            ScannerEvent.ResumeScanning -> resumeScanning()
            is ScannerEvent.CardSelected -> {
                if (_uiState.value.isPriceCheckMode && event.card != null) {
                    handlePriceCheck(event.card)
                } else {
                    _uiState.update { it.copy(selectedCard = event.card) }
                }
            }
            is ScannerEvent.SaveScannedCard -> saveScannedCard(
                event.card, event.quantity, event.condition,
                event.printing, event.finish, event.folderIds,
                navigateToGrading = false
            )

            is ScannerEvent.SaveAndGrade -> {
                val bmp = activeCaptureForGrading
                if (bmp == null) {
                    _uiState.update {
                        it.copy(errorMessage = "Capture a photo first to grade this card.")
                    }
                } else {
                    viewModelScope.launch(defaultDispatcher) {
                        _effect.send(
                            ScannerEffect.NavigateToGrading(-1L, bmp, event.card)
                        )
                        activeCaptureForGrading = null
                    }
                }
            }
            ScannerEvent.ConsumeSaveSuccess -> consumeSaveSuccess()
            ScannerEvent.ClearDetectedNumber -> clearDetectedNumber()
            ScannerEvent.ClearErrorMessage -> _uiState.update { it.copy(errorMessage = null) }
            is ScannerEvent.PermissionResult -> _uiState.update { it.copy(hasCameraPermission = event.granted) }
            ScannerEvent.ToggleTorch -> _uiState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
            is ScannerEvent.SelectMode -> {
                val currentMode = _uiState.value.activeMode
                val nextMode = if (currentMode == event.mode) ScannerMode.IDLE else event.mode

                searchJob?.cancel()
                _uiState.update { it.copy(
                    activeMode = nextMode,
                    detectedNumber = null,
                    detectedTotal = null,
                    detectedName = null,
                    candidates = persistentListOf(),
                    isSearching = false,
                    isPaused = false,
                    autoSelectedCard = null,
                    selectedCard = null,
                    priceCheckInfo = null,
                    pageScanMode = if (nextMode == ScannerMode.PAGE) PageScanMode.IDLE else it.pageScanMode
                ) }
                lastMatchedNumber = null
                detectionHistory.clear()
            }
            is ScannerEvent.CapturePhoto -> capturePhoto(event.bitmap)
            is ScannerEvent.ConfirmPageCell -> confirmPageCell(event.index, event.card)
            is ScannerEvent.RejectPageCell -> rejectPageCell(event.index)
            ScannerEvent.SaveAllPageResults -> saveAllPageResults()
            ScannerEvent.RetryPageScan -> _uiState.update { it.copy(pageScanMode = PageScanMode.IDLE, pageScanCells = emptyList()) }
            is ScannerEvent.SetBulkDefaults -> {
                viewModelScope.launch {
                    repository.userPreferencesRepository.setBulkScanCondition(event.defaults.condition)
                    repository.userPreferencesRepository.setBulkScanPrinting(event.defaults.printing)
                    repository.userPreferencesRepository.setBulkScanFinish(event.defaults.finish)
                    repository.userPreferencesRepository.setBulkScanFolderIds(event.defaults.folderIds)
                }
                _uiState.update { it.copy(bulkDefaults = event.defaults) }
            }
            ScannerEvent.UndoLastBulkScan -> undoLastBulkScan()
            ScannerEvent.ClearBulkSession -> _uiState.update {
                it.copy(bulkSessionLog = persistentListOf(), skippedCards = persistentListOf())
            }
            is ScannerEvent.ConfirmSkippedCard -> {
                saveScannedCard(
                    event.card,
                    event.quantity,
                    event.condition,
                    event.printing,
                    event.finish,
                    event.folderIds,
                    navigateToGrading = false
                )
                _uiState.update {
                    it.copy(
                        skippedCards = it.skippedCards.filter { c -> c.id != event.card.id }
                            .toImmutableList()
                    )
                }
            }
            is ScannerEvent.AdjustExposure -> _uiState.update { it.copy(exposureIndex = event.index) }
            is ScannerEvent.SetExposureState -> _uiState.update {
                it.copy(
                    exposureRange = event.range,
                    exposureIndex = event.current,
                    isExposureSupported = event.supported
                )
            }

            is ScannerEvent.TapToFocus -> {
                _uiState.update { it.copy(focusPoint = event.point) }
                viewModelScope.launch {
                    delay(2000)
                    if (_uiState.value.focusPoint == event.point) {
                        _uiState.update { it.copy(focusPoint = null) }
                    }
                }
            }

            ScannerEvent.ClearFocus -> _uiState.update { it.copy(focusPoint = null) }
            ScannerEvent.RecognizerUnavailable ->
                _uiState.update { it.copy(recognizerUnavailable = true) }
            ScannerEvent.DismissEmptyCatalogHint -> {
                emptyCatalogHintDismissed = true
                _uiState.update { it.copy(showEmptyCatalogHint = false) }
            }
            ScannerEvent.OpenSetDownloads -> {
                viewModelScope.launch {
                    _effect.send(ScannerEffect.NavigateToSetDownloads)
                }
            }
        }
    }

    private fun capturePhoto(bitmap: Bitmap) {
        if (_uiState.value.isPageScanMode) {
            _uiState.update {
                it.copy(
                    pageScanMode = PageScanMode.PROCESSING,
                    pageScanCells = emptyList()
                )
            }
            viewModelScope.launch(defaultDispatcher) {
                val cells = PageScanProcessor.processPage(bitmap)
                _uiState.update {
                    it.copy(
                        pageScanCells = cells,
                        pageScanMode = PageScanMode.REVIEWING
                    )
                }

                cells.forEach { cell ->
                    searchCardForCell(cell)
                }
            }
        } else if (_uiState.value.isGradingMode) {
            val card = _uiState.value.autoSelectedCard
            if (card != null) {
                activeCaptureForGrading = bitmap
                viewModelScope.launch {
                    _effect.send(
                        ScannerEffect.NavigateToGrading(
                            -1L,
                            activeCaptureForGrading,
                            card
                        )
                    )
                    activeCaptureForGrading = null
                    resumeScanning()
                }
            }
        }
    }

    private fun onLinesDetected(lines: List<DetectedLine>, pHash: Long?, isGlareDetected: Boolean) {
        if (_uiState.value.activeMode == ScannerMode.IDLE || _uiState.value.isPaused || _uiState.value.isSearching) return

        _uiState.update { it.copy(isGlareDetected = isGlareDetected) }

        viewModelScope.launch(defaultDispatcher) {
            val nameCandidates = lines
                .filter {
                    (it.boundingBox?.top?.toFloat() ?: Float.MAX_VALUE) / it.imageHeight < 0.30f
                }
                .sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
                .take(3)
            var bestName: String? = null
            for (line in nameCandidates) {
                val cleaned = cleanCardName(line.text)
                if (cleaned.length >= 3) {
                    bestName = cleaned
                    break
                }
            }

            var bestLocalId: String? = null
            var bestTotal: String? = null
            val numberCandidates = lines.filter {
                (it.boundingBox?.centerY()?.toFloat() ?: 0f) / it.imageHeight > 0.70f
            }
            
            val combinedBottomText = numberCandidates.joinToString(" ") { it.text }
            val normalizedBottom = normalizeOcrTextForNumbers(combinedBottomText)

            // Prioritized matching: Number/Total > Set Prefix + Number > Standalone Number
            val totalMatch = numberTotalRegex.find(normalizedBottom)
            if (totalMatch != null) {
                bestLocalId = totalMatch.groupValues[1]
                bestTotal = totalMatch.groupValues[2]
            } else {
                val setMatch = setNumberRegex.find(normalizedBottom)
                if (setMatch != null) {
                    bestLocalId = "${setMatch.groupValues[1]}${setMatch.groupValues[2]}"
                } else {
                    val standaloneMatch = standaloneNumberRegex.find(normalizedBottom)
                    if (standaloneMatch != null) {
                        bestLocalId = standaloneMatch.groupValues[1]
                    }
                }
            }

            // Validation: Ensure bestLocalId doesn't match known noise words
            if (bestLocalId != null && noiseWords.contains(bestLocalId.uppercase())) {
                bestLocalId = null
            }

            synchronized(detectionHistory) {
                if (detectionHistory.size >= 5) detectionHistory.removeFirst()
                detectionHistory.addLast(FrameDetection(number = bestLocalId, total = bestTotal, name = bestName, pHash = pHash))
            }

            if (bestLocalId == null) return@launch
            
            val (consensusTotal, consensusName) = synchronized(detectionHistory) {
                val count = detectionHistory.count { it.number == bestLocalId }

                // Restore stability: Require 3 frames for a solid lock.
                val requiredFrames = 3
                if (count < requiredFrames) {
                    return@launch
                }

                val total = detectionHistory
                    .filter { it.number == bestLocalId }
                    .mapNotNull { it.total }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key ?: bestTotal

                val name = detectionHistory
                    .filter { it.number == bestLocalId }
                    .mapNotNull { it.name }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key ?: bestName
                total to name
            }

            if (bestLocalId == lastMatchedNumber && consensusTotal == _uiState.value.detectedTotal) return@launch

            _uiState.update { it.copy(
                detectedNumber = bestLocalId,
                detectedTotal = consensusTotal,
                detectedName = consensusName,
                isSearching = true
            ) }

            searchJob?.cancel()
            searchJob = launch {
                delay(120) // Slightly longer debounce for stability
                searchCard(bestLocalId, consensusTotal, consensusName, pHash)
            }
        }
    }

    private fun searchCard(localId: String, totalCount: String?, name: String?, pHash: Long?) {
        viewModelScope.launch(defaultDispatcher) {
            try {
                val numberNorm = if (localId.all { it.isDigit() }) localId.padStart(3, '0') else localId
                var localResults = emptyList<CardEntity>()
                val totalInt = totalCount?.toIntOrNull()
                if (totalInt != null) {
                    localResults = repository.searchLocalCardsWithTotal(numberNorm, totalInt)
                }
                
                if (localResults.isEmpty()) {
                    localResults = repository.searchLocalCards(numberNorm)
                }

                // If we have local candidates but multiple, try to disambiguate with pHash
                if (localResults.size > 1) {
                    val hashed = localResults.filter { it.pHash != null }
                    if (hashed.isNotEmpty() && pHash != null) {
                        val bestMatch = hashed.minByOrNull { PHash.hammingDistance(it.pHash!!, pHash) }
                        if (bestMatch != null && PHash.hammingDistance(bestMatch.pHash!!, pHash) < 12) {
                            localResults = listOf(bestMatch)
                        }
                    } else if (pHash != null) {
                        // None have pHash locally, try to fetch and update on-the-fly for the top 2
                        localResults.take(2).forEach { card ->
                            if (card.image != null) {
                                val bitmap = repository.fetchBitmapFromUrl(card.image)
                                if (bitmap != null) {
                                    val computed = PHash.computeHash(bitmap)
                                    repository.updateCardPHash(card.id, computed)
                                    // If this is a match, we can use it immediately for the next frame
                                }
                            }
                        }
                    }
                }

                if (name != null && localResults.size > 1) {
                    val filtered = localResults.filter { card ->
                        calculateSimilarity(card.name, name) > 0.65f
                    }
                    if (filtered.isNotEmpty()) localResults = filtered
                }

                val finalCandidates = if (localResults.size == 1) {
                    localResults.map { mapToTcgDexCard(it) }
                } else {
                    val remoteResults = try {
                        repository.searchTcgDexByLocalId(localId)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                    
                    val all = (localResults.map { mapToTcgDexCard(it) } + remoteResults).distinctBy { it.id }
                    
                    var filtered = all
                    if (totalInt != null) {
                        filtered = all.filter { card ->
                            val cardTotal = card.id.substringAfterLast("-").toIntOrNull()
                            cardTotal == null || cardTotal == totalInt
                        }
                        if (filtered.isEmpty()) filtered = all 
                    }

                    // On-the-fly pHash for remote candidates if still ambiguous
                    if (filtered.size > 1 && pHash != null) {
                        for (card in filtered.take(3)) {
                            if (card.image != null) {
                                val bitmap = repository.fetchBitmapFromUrl(card.image)
                                if (bitmap != null) {
                                    val computed = PHash.computeHash(bitmap)
                                    if (PHash.hammingDistance(computed, pHash) < 12) {
                                        filtered = listOf(card)
                                        repository.updateCardPHash(card.id, computed)
                                        break
                                    }
                                }
                            }
                        }
                    }

                    if (name != null && filtered.size > 1) {
                        filtered = filtered.sortedByDescending { calculateSimilarity(it.name, name) }
                    }
                    filtered
                }

                if (finalCandidates.size == 1) {
                    val card = finalCandidates.first()
                    if (_uiState.value.isBulkMode) {
                        saveBulkCard(card)
                    } else if (_uiState.value.isPriceCheckMode) {
                        handlePriceCheck(card)
                    } else {
                        lastMatchedNumber = localId
                        _uiState.update {
                            it.copy(
                                autoSelectedCard = card,
                                isSearching = false,
                                isPaused = true,
                                autoCaptureTrigger = if (it.isGradingMode) System.currentTimeMillis() else it.autoCaptureTrigger
                            )
                        }
                    }
                } else {
                    if (_uiState.value.isBulkMode && finalCandidates.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                bulkSessionLog = (state.bulkSessionLog + BulkScanEntry(
                                    card = finalCandidates.first(),
                                    status = BulkScanStatus.SKIPPED_AMBIGUOUS
                                )).toImmutableList(),
                                skippedCards = (state.skippedCards + finalCandidates).toImmutableList(),
                                isSearching = false
                            )
                        }
                        resumeScanning()
                    } else {
                        _uiState.update {
                            it.copy(
                                candidates = finalCandidates.take(5).toImmutableList(),
                                isSearching = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(isSearching = false, errorMessage = "Search failed: ${e.message}") }
            }
        }
    }

    private fun handlePriceCheck(card: TcgDexCard) {
        lastMatchedNumber = card.localId
        _uiState.update {
            it.copy(
                priceCheckInfo = PriceCheckInfo(card = card, isFetching = true),
                isSearching = false,
                isPaused = true,
                candidates = persistentListOf(),
                detectedNumber = null,
                detectedTotal = null,
                detectedName = null
            )
        }

        viewModelScope.launch(defaultDispatcher) {
            repository.updateCardPrice(card.id)
            val prices = repository.getPricesForCard(card.id).first()
            val vintagePrices = repository.getVintagePricesForCard(card.id).first()
            _uiState.update {
                it.copy(
                    priceCheckInfo = it.priceCheckInfo?.copy(
                        prices = prices.toImmutableList(),
                        vintagePrices = vintagePrices.toImmutableList(),
                        isFetching = false
                    )
                )
            }
        }
    }

    private fun searchCardForCell(cell: PageScanCell) {
        viewModelScope.launch(defaultDispatcher) {
            _uiState.update { state ->
                state.copy(pageScanCells = state.pageScanCells.map {
                    if (it.id == cell.id) it.copy(status = PageScanCellStatus.SCANNING) else it
                })
            }

            val ocr = cell.ocrResult ?: ""
            val normalizedOcr = normalizeOcrTextForNumbers(ocr)

            var localId: String? = null
            var totalCount: String? = null

            val totalMatch = numberTotalRegex.find(normalizedOcr)
            if (totalMatch != null) {
                localId = totalMatch.groupValues[1]
                totalCount = totalMatch.groupValues[2]
            } else {
                val setMatch = setNumberRegex.find(normalizedOcr)
                if (setMatch != null) {
                    localId = "${setMatch.groupValues[1]}${setMatch.groupValues[2]}"
                } else {
                    val standaloneMatch = standaloneNumberRegex.find(normalizedOcr)
                    if (standaloneMatch != null) {
                        localId = standaloneMatch.groupValues[1]
                    }
                }
            }

            val name = cleanCardName(ocr).takeIf { it.isNotEmpty() }

            if (localId == null) {
                _uiState.update { state ->
                    state.copy(pageScanCells = state.pageScanCells.map {
                        if (it.id == cell.id) it.copy(status = PageScanCellStatus.NOT_FOUND) else it
                    })
                }
                return@launch
            }

            try {
                val numberNorm =
                    if (localId.all { it.isDigit() }) localId.padStart(3, '0') else localId
                var localResults = repository.searchLocalCards(numberNorm)
                val totalInt = totalCount?.toIntOrNull()

                if (totalInt != null) {
                    val withTotal = repository.searchLocalCardsWithTotal(numberNorm, totalInt)
                    if (withTotal.isNotEmpty()) localResults = withTotal
                }

                val finalCandidates = if (localResults.size == 1) {
                    localResults.map { mapToTcgDexCard(it) }
                } else {
                    val remoteResults = try {
                        repository.searchTcgDexByLocalId(localId)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                    (localResults.map { mapToTcgDexCard(it) } + remoteResults).distinctBy { it.id }
                }

                val bestMatch = if (name != null) {
                    finalCandidates.maxByOrNull { calculateSimilarity(it.name, name) }
                } else {
                    finalCandidates.firstOrNull()
                }
                val status = when {
                    bestMatch == null -> PageScanCellStatus.NOT_FOUND
                    finalCandidates.size > 1 -> PageScanCellStatus.AMBIGUOUS
                    else -> PageScanCellStatus.MATCHED
                }

                _uiState.update { state ->
                    state.copy(pageScanCells = state.pageScanCells.map {
                        if (it.id == cell.id) {
                            it.copy(
                                matchedCard = bestMatch,
                                candidates = finalCandidates,
                                status = status,
                                isConfirmed = status == PageScanCellStatus.MATCHED
                            )
                        } else it
                    })
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { state ->
                    state.copy(pageScanCells = state.pageScanCells.map {
                        if (it.id == cell.id) it.copy(status = PageScanCellStatus.ERROR) else it
                    })
                }
            }
        }
    }

    private fun confirmPageCell(index: Int, card: TcgDexCard?) {
        _uiState.update { state ->
            state.copy(pageScanCells = state.pageScanCells.map {
                if (it.id == index) it.copy(
                    matchedCard = card,
                    isConfirmed = true,
                    status = PageScanCellStatus.MATCHED
                ) else it
            })
        }
    }

    private fun rejectPageCell(index: Int) {
        _uiState.update { state ->
            state.copy(pageScanCells = state.pageScanCells.map {
                if (it.id == index) it.copy(isConfirmed = false) else it
            })
        }
    }

    private fun saveAllPageResults() {
        val confirmedCells =
            _uiState.value.pageScanCells.filter { it.isConfirmed && it.matchedCard != null }
        if (confirmedCells.isEmpty()) {
            _uiState.update { it.copy(pageScanMode = PageScanMode.IDLE) }
            return
        }

        viewModelScope.launch(defaultDispatcher) {
            val defaults = _uiState.value.bulkDefaults
            confirmedCells.forEach { cell ->
                cell.matchedCard?.let { card ->
                    repository.addUserCard(
                        card,
                        UserCardEntity(
                            cardId = card.id,
                            quantity = 1,
                            condition = defaults.condition,
                            printing = defaults.printing,
                            finish = defaults.finish
                        ),
                        folderIds = defaults.folderIds
                    )
                }
            }
            _uiState.update { it.copy(pageScanMode = PageScanMode.IDLE, showSaveSuccess = true) }
        }
    }

    private fun undoLastBulkScan() {
        val lastEntry =
            _uiState.value.bulkSessionLog.lastOrNull { it.status != BulkScanStatus.SKIPPED_AMBIGUOUS }
                ?: return
        viewModelScope.launch(defaultDispatcher) {
            try {
                repository.deleteLastUserCardInstance(lastEntry.card.id)
                _uiState.update {
                    it.copy(
                        bulkSessionLog = it.bulkSessionLog.dropLast(1).toImmutableList()
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(errorMessage = "Undo failed: ${e.message}") }
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

    private fun normalizeOcrTextForNumbers(text: String): String {
        return text.split(Regex("\\s+")).joinToString(" ") { token ->
            if (token.any { it.isDigit() || it == '/' } || token.length <= 5) {
                token.uppercase()
                    .replace("O", "0")
                    .replace("I", "1")
                    .replace("L", "1")
                    .replace("S", "5")
                    .replace("B", "8")
                    .replace("G", "6")
                    .replace("D", "0")
                    .replace("Z", "2")
                    .replace("Q", "9")
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

    private fun saveBulkCard(card: TcgDexCard) {
        viewModelScope.launch(defaultDispatcher) {
            try {
                val defaults = _uiState.value.bulkDefaults
                repository.addUserCard(
                    card,
                    UserCardEntity(
                        cardId = card.id,
                        quantity = 1,
                        condition = defaults.condition,
                        printing = defaults.printing,
                        finish = defaults.finish
                    ),
                    folderIds = defaults.folderIds
                )
                
                _uiState.update { state ->
                    val lastEntry = state.bulkSessionLog.lastOrNull()
                    val isSameAsLast = lastEntry != null && 
                                     lastEntry.card.id == card.id && 
                                     lastEntry.status != BulkScanStatus.SKIPPED_AMBIGUOUS

                    val newLog = if (isSameAsLast && lastEntry != null) {
                        (state.bulkSessionLog.dropLast(1) + lastEntry.copy(
                            quantity = lastEntry.quantity + 1,
                            status = BulkScanStatus.DUPLICATE_INCREMENTED
                        )).toImmutableList()
                    } else {
                        (state.bulkSessionLog + BulkScanEntry(
                            card = card,
                            status = BulkScanStatus.SAVED
                        )).toImmutableList()
                    }

                    state.copy(
                        bulkSessionLog = newLog,
                        isSearching = false
                    )
                }
                resumeScanning()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(isSearching = false, errorMessage = "Bulk save failed: ${e.message}") }
            }
        }
    }

    private fun resumeScanning() {
        lastMatchedNumber = null
        detectionHistory.clear()
        _uiState.update { it.copy(
            isPaused = false,
            autoSelectedCard = null,
            selectedCard = null,
            priceCheckInfo = null,
            detectedNumber = null,
            detectedTotal = null,
            detectedName = null,
            candidates = persistentListOf()
        ) }
    }

    private fun saveScannedCard(
        card: TcgDexCard,
        quantity: Int,
        condition: String,
        printing: String,
        finish: String,
        folderIds: List<Long> = emptyList(),
        navigateToGrading: Boolean = false
    ) {
        viewModelScope.launch(defaultDispatcher) {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val userCardId = repository.addUserCard(
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

                if (navigateToGrading) {
                    _effect.send(
                        ScannerEffect.NavigateToGrading(
                            userCardId,
                            activeCaptureForGrading
                        )
                    )
                    activeCaptureForGrading = null
                } else {
                    _uiState.update { it.copy(showSaveSuccess = true) }
                }
                resumeScanning()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(errorMessage = "Failed to save card: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun consumeSaveSuccess() {
        _uiState.update { it.copy(showSaveSuccess = false) }
    }

    private fun clearDetectedNumber() {
        lastMatchedNumber = null
        detectionHistory.clear()
        _uiState.update { it.copy(
            detectedNumber = null,
            detectedTotal = null,
            detectedName = null,
            candidates = persistentListOf()
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
