package com.mrhayami.vaultio.ui.collection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.PokemonUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderCardCrossRef
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CollectionViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : MviViewModel<CollectionUiState, CollectionEvent, CollectionEffect>(
    initialState = CollectionUiState()
) {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    private val _isSearchBarVisible = MutableStateFlow(false)
    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    private val _filterSettings = MutableStateFlow(FilterSettings())
    private val _selectionState = MutableStateFlow(emptySet<Long>())
    private val _showSaveSuccess = MutableStateFlow(false)
    private val _remoteSearchResults = MutableStateFlow<List<TcgDexCard>>(emptyList())
    private val _isRemoteSearching = MutableStateFlow(false)
    private val _newSetsToDownload = MutableStateFlow<List<SetEntity>>(emptyList())
    private val _isDownloadingNewSets = MutableStateFlow(false)
    private val _selectedDexId = MutableStateFlow<Int?>(null)

    // Base Data Flows
    private val _allCards = repository.allUserCards
    private val _allFolders = repository.allFolders
    private val _allCrossRefs = repository.allFolderCardCrossRefs
    private val _allPrices = repository.allPrices
    private val _allVintagePrices = repository.allVintagePrices
    private val _allSets = repository.allSets.map { sets -> sets.associateBy { it.id } }

    // Derived Flows for optimization
    private val _priceMap = _allPrices.map { prices ->
        prices.associateBy { "${it.cardId}_${it.finish}_${it.condition}" }
    }.flowOn(Dispatchers.Default)

    private val _vintagePriceMap = _allVintagePrices.map { prices ->
        prices.associateBy { "${it.cardId}_${it.finish}_${it.condition}_${it.printing}" }
    }.flowOn(Dispatchers.Default)

    private val _filteredCards = combine(
        _searchQuery,
        _selectedFolderId,
        userPreferencesRepository.sortMode,
        _sortDirection,
        _filterSettings,
        _priceMap,
        _vintagePriceMap
    ) { flows ->
        val sq = flows[0] as String
        val sfid = flows[1] as Long?
        val sm = flows[2] as SortMode
        val sd = flows[3] as SortDirection
        val fs = flows[4] as FilterSettings
        val pm = flows[5] as Map<String, PriceEntity>
        val vpm = flows[6] as Map<String, VintagePriceEntity>

        repository.getFilteredUserCards(sq, sfid, sm, sd, fs).map { cards ->
            mapAndSortFinal(cards, pm, vpm, sm, sd)
        }
    }.flatMapLatest { it }
        .flowOn(Dispatchers.Default)

    private val _stats = _filteredCards.map { filtered ->
        computeStats(filtered)
    }.flowOn(Dispatchers.Default)

    private val _pokedexEntries = combine(
        _allCards,
        _filteredCards,
        _searchQuery,
        _selectedFolderId,
        _filterSettings,
        userPreferencesRepository.pokedexSettings
    ) { flows ->
        val allCards = flows[0] as List<CardWithDetails>
        val filteredCards = flows[1] as List<CardUiModel>
        val searchQuery = flows[2] as String
        val selectedFolderId = flows[3] as Long?
        val filterSettings = flows[4] as FilterSettings
        val pokedexSettings = flows[5] as PokedexSettings

        val pokedexUserCards = if (selectedFolderId == null && filterSettings == FilterSettings()) {
            allCards
        } else {
            filteredCards.map { it.details }
        }

        var entries = computePokedexEntries(pokedexUserCards, pokedexSettings)
        if (searchQuery.isNotBlank()) {
            entries = entries.filter { entry ->
                entry.pokemonName?.contains(searchQuery, ignoreCase = true) == true ||
                        entry.dexNumber.toString() == searchQuery
            }
        }
        entries
    }.flowOn(Dispatchers.Default)

    private val _collectedCardsForDex = combine(
        _allCards,
        _selectedDexId,
        _priceMap,
        _vintagePriceMap
    ) { allCards, dexId, priceMap, vintagePriceMap ->
        if (dexId == null) return@combine emptyList<CardUiModel>()

        allCards.filter { cardWithDetails ->
            val dexIds =
                PokemonUtils.parseDexIds(cardWithDetails.card.dexIds, cardWithDetails.card.dexId)
            dexIds.contains(dexId)
        }.map { details ->
            val price = details.userCard.manualPrice ?: run {
                val cardId = details.card.id
                val finish = details.userCard.finish
                val condition = details.userCard.condition
                val printing = details.userCard.printing

                priceMap["${cardId}_${finish}_${condition}"]?.marketPrice
                    ?: vintagePriceMap["${cardId}_${finish}_${condition}_${printing}"]?.marketPrice
                    ?: 0.0
            }
            CardUiModel(details, price)
        }
    }.flowOn(Dispatchers.Default)

    init {
        // 1. UI State Changes
        combine(
            userPreferencesRepository.viewMode,
            _searchQuery,
            _selectedFolderId,
            _isSearchBarVisible,
            _selectionState,
            _showSaveSuccess,
            _selectedDexId
        ) { flows ->
            val viewMode = flows[0] as ViewMode
            val searchQuery = flows[1] as String
            val selectedFolderId = flows[2] as Long?
            val isSearchBarVisible = flows[3] as Boolean
            val selectedIds = flows[4] as Set<Long>
            val showSaveSuccess = flows[5] as Boolean
            val selectedDexId = flows[6] as Int?

            updateState {
                copy(
                    viewMode = viewMode,
                    searchQuery = searchQuery,
                    selectedFolderId = selectedFolderId,
                    isSearchBarVisible = isSearchBarVisible,
                    selectedIds = selectedIds.toImmutableSet(),
                    isSelectionMode = selectedIds.isNotEmpty(),
                    showSaveSuccess = showSaveSuccess,
                    selectedDexId = selectedDexId
                )
            }
        }.launchIn(viewModelScope)

        // 2. Settings Changes
        combine(
            userPreferencesRepository.sortMode,
            _sortDirection,
            _filterSettings,
            userPreferencesRepository.listSettings,
            userPreferencesRepository.gridSettings,
            userPreferencesRepository.pokedexSettings,
            userPreferencesRepository.preferSetLogo
        ) { flows ->
            val sortMode = flows[0] as SortMode
            val sortDirection = flows[1] as SortDirection
            val filterSettings = flows[2] as FilterSettings
            val listSettings = flows[3] as ListSettings
            val gridSettings = flows[4] as GridSettings
            val pokedexSettings = flows[5] as PokedexSettings
            val preferSetLogo = flows[6] as Boolean

            updateState {
                copy(
                    sortMode = sortMode,
                    sortDirection = sortDirection,
                    filterSettings = filterSettings,
                    listSettings = listSettings,
                    gridSettings = gridSettings,
                    pokedexSettings = pokedexSettings,
                    preferSetLogo = preferSetLogo
                )
            }
        }.launchIn(viewModelScope)

        // 3. Data Changes
        combine(
            _allCards,
            _allFolders,
            _filteredCards,
            _pokedexEntries,
            _stats,
            _allSets,
            _collectedCardsForDex
        ) { flows ->
            val allCards = flows[0] as List<CardWithDetails>
            val folders = flows[1] as List<FolderEntity>
            val filteredCards = flows[2] as List<CardUiModel>
            val pokedexEntries = flows[3] as List<PokedexEntry>
            val stats = flows[4] as Triple<Double, Int, Int>
            val setsMap = flows[5] as Map<String, SetEntity>
            val collectedCardsForDex = flows[6] as List<CardUiModel>

            val availableFilters = computeAvailableFilters(allCards)

            updateState {
                copy(
                    userCards = allCards.toImmutableList(),
                    folders = folders.toImmutableList(),
                    filteredUserCards = filteredCards.toImmutableList(),
                    pokedexEntries = pokedexEntries.toImmutableList(),
                    totalValue = stats.first,
                    totalCount = stats.second,
                    totalQuantity = stats.third,
                    sets = setsMap.toImmutableMap(),
                    collectedCardsForDex = collectedCardsForDex.toImmutableList(),
                    availableRarities = availableFilters.first.toImmutableList(),
                    availableCategories = availableFilters.second.toImmutableList(),
                    availableTypes = availableFilters.third.toImmutableList(),
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)

        // 4. Remote Search & Download Changes
        combine(
            _remoteSearchResults,
            _isRemoteSearching,
            _newSetsToDownload,
            _isDownloadingNewSets
        ) { flows ->
            val remoteResults = flows[0] as List<TcgDexCard>
            val isRemoteSearching = flows[1] as Boolean
            val newSets = flows[2] as List<SetEntity>
            val isDownloading = flows[3] as Boolean

            updateState {
                copy(
                    searchResults = remoteResults.toImmutableList(),
                    isSearching = isRemoteSearching,
                    newSetsToDownload = newSets.toImmutableList(),
                    isDownloadingNewSets = isDownloading
                )
            }
        }.launchIn(viewModelScope)

        checkForNewSets()
    }

    private fun checkForNewSets() {
        viewModelScope.launch {
            val lastCheck = userPreferencesRepository.lastSetCheck.first()
            val now = System.currentTimeMillis()
            if (now - lastCheck > 24 * 60 * 60 * 1000) {
                userPreferencesRepository.setLastSetCheck(now)
                val newSets = repository.getNewSets()
                if (newSets.isNotEmpty()) {
                    _newSetsToDownload.value = newSets
                }
            }
        }
    }

    override fun onEvent(event: CollectionEvent) {
        when (event) {
            is CollectionEvent.OnViewModeChange -> setViewMode(event.viewMode)
            is CollectionEvent.OnSortModeChange -> setSortMode(event.sortMode)
            is CollectionEvent.OnSortDirectionChange -> setSortDirection(event.direction)
            CollectionEvent.OnToggleSearchBar -> toggleSearchBar()
            is CollectionEvent.OnSearchQueryChange -> setSearchQuery(event.query)
            is CollectionEvent.OnFolderSelect -> selectFolder(event.folderId)
            is CollectionEvent.OnUpdateListSettings -> updateListSettings(event.settings)
            is CollectionEvent.OnUpdateGridSettings -> updateGridSettings(event.settings)
            is CollectionEvent.OnUpdatePokedexSettings -> updatePokedexSettings(event.settings)
            is CollectionEvent.OnToggleRarityFilter -> toggleRarityFilter(event.rarity)
            is CollectionEvent.OnToggleCategoryFilter -> toggleCategoryFilter(event.category)
            is CollectionEvent.OnToggleTypeFilter -> toggleTypeFilter(event.type)
            is CollectionEvent.OnToggleConditionFilter -> toggleConditionFilter(event.condition)
            is CollectionEvent.OnToggleFinishFilter -> toggleFinishFilter(event.finish)
            CollectionEvent.OnClearFilters -> clearFilters()
            is CollectionEvent.OnToggleSelection -> toggleSelection(event.id)
            CollectionEvent.OnSelectAll -> selectAll()
            CollectionEvent.OnClearSelection -> clearSelection()
            CollectionEvent.OnDeleteSelectedCards -> deleteSelectedCards()
            is CollectionEvent.OnMoveSelectedToFolder -> moveSelectedToFolder(event.folderId)
            is CollectionEvent.OnAddFolder -> addFolder(event.name, event.icon, event.color)
            is CollectionEvent.OnUpdateFolder -> updateFolder(event.folder)
            is CollectionEvent.OnDeleteFolder -> deleteFolder(event.folder)
            is CollectionEvent.OnSearchRemoteCards -> searchRemoteCards(event.query)
            is CollectionEvent.OnAddUserCard -> addUserCard(event.card, event.quantity, event.condition, event.printing, event.finish, event.folderIds)
            CollectionEvent.OnConsumeSaveSuccess -> consumeSaveSuccess()
            CollectionEvent.OnDownloadNewSets -> downloadNewSets()
            CollectionEvent.OnDismissNewSetsPrompt -> dismissNewSetsPrompt()
            is CollectionEvent.OnExportCollection -> exportCollection(event.folderIds)
            is CollectionEvent.OnImportCollection -> importCollection(event.json)
            is CollectionEvent.OnDexClick -> selectDex(event.dexId)
            CollectionEvent.OnDismissDexDetail -> selectDex(null)
        }
    }

    private fun selectDex(dexId: Int?) {
        _selectedDexId.value = dexId
    }

    private fun downloadNewSets() {
        viewModelScope.launch {
            val setsToDownload = _newSetsToDownload.value
            if (setsToDownload.isEmpty()) return@launch

            _isDownloadingNewSets.value = true
            _newSetsToDownload.value = emptyList()

            var successCount = 0
            setsToDownload.forEach { set ->
                try {
                    repository.downloadSet(set.id)
                    successCount++
                } catch (e: Exception) {
                    Log.e("CollectionViewModel", "Failed to download set ${set.id}", e)
                }
            }

            _isDownloadingNewSets.value = false
            emitEffect(CollectionEffect.ShowToast("Downloaded $successCount new sets successfully!"))
        }
    }

    private fun dismissNewSetsPrompt() {
        _newSetsToDownload.value = emptyList()
    }

    private fun exportCollection(folderIds: List<Long>?) {
        viewModelScope.launch {
            val json = repository.exportCollectionJson(folderIds)
            emitEffect(CollectionEffect.ExportCollection(json))
        }
    }

    private fun importCollection(json: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            repository.importCollectionFromJson(json)
                .onSuccess {
                    emitEffect(CollectionEffect.ImportSuccess)
                    emitEffect(CollectionEffect.ShowToast("Collection imported successfully"))
                }
                .onFailure {
                    emitEffect(CollectionEffect.ShowToast("Failed to import collection: ${it.message}"))
                }
            updateState { copy(isLoading = false) }
        }
    }

    private fun filterAndSortCards(
        userCards: List<CardWithDetails>,
        crossRefs: List<FolderCardCrossRef>,
        searchQuery: String,
        selectedFolderId: Long?,
        sortMode: SortMode,
        sortDirection: SortDirection,
        filterSettings: FilterSettings,
        priceMap: Map<String, PriceEntity>,
        vintagePriceMap: Map<String, VintagePriceEntity>
    ): List<CardUiModel> {
        val filtered = userCards.filter { cardWithDetails ->
            val card = cardWithDetails.card
            val userCard = cardWithDetails.userCard

            val matchesSearch = if (searchQuery.isBlank()) true
            else card.name.contains(searchQuery, ignoreCase = true) ||
                    card.pokemonName?.contains(searchQuery, ignoreCase = true) == true ||
                    cardWithDetails.set.name.contains(searchQuery, ignoreCase = true)

            val matchesFolder = if (selectedFolderId == null) true
            else crossRefs.any { it.folderId == selectedFolderId && it.userCardId == userCard.id }

            val matchesRarity = filterSettings.rarities.isEmpty() || filterSettings.rarities.contains(card.rarity)
            val matchesCategory = filterSettings.categories.isEmpty() || filterSettings.categories.contains(card.category)
            val matchesCondition = filterSettings.conditions.isEmpty() || filterSettings.conditions.contains(userCard.condition)
            val matchesFinish = filterSettings.finishes.isEmpty() || filterSettings.finishes.contains(userCard.finish)

            val cardTypes = card.types?.split(",")?.map { it.trim() } ?: emptyList()
            val matchesType = filterSettings.types.isEmpty() || cardTypes.any { filterSettings.types.contains(it) }

            matchesSearch && matchesFolder && matchesRarity && matchesCategory && matchesCondition && matchesFinish && matchesType
        }

        val cardUiModels = filtered.map { details ->
            val price = details.userCard.manualPrice ?: run {
                val cardId = details.card.id
                val finish = details.userCard.finish
                val condition = details.userCard.condition
                val printing = details.userCard.printing

                priceMap["${cardId}_${finish}_${condition}"]?.marketPrice
                    ?: vintagePriceMap["${cardId}_${finish}_${condition}_${printing}"]?.marketPrice
                    ?: 0.0
            }
            CardUiModel(details, price)
        }

        val comparator = when (sortMode) {
            SortMode.NAME -> compareBy<CardUiModel> { it.details.card.name }
            SortMode.SET -> compareBy<CardUiModel> { it.details.set.releaseDate }.thenBy {
                it.details.card.localId.padStart(
                    5,
                    '0'
                )
            }

            SortMode.VALUE -> compareBy<CardUiModel> { it.price }
            SortMode.DATE_ADDED -> compareBy<CardUiModel> { it.details.userCard.dateAdded }
            SortMode.RARITY -> compareBy<CardUiModel> { it.details.card.rarity }
            SortMode.QUANTITY -> compareBy<CardUiModel> { it.details.userCard.quantity }
            SortMode.NUMBER -> compareBy<CardUiModel> { it.details.card.localId.padStart(5, '0') }
        }

        return if (sortDirection == SortDirection.ASCENDING) cardUiModels.sortedWith(comparator) else cardUiModels.sortedWith(
            comparator.reversed()
        )
    }

    private fun mapAndSortFinal(
        userCards: List<CardWithDetails>,
        priceMap: Map<String, PriceEntity>,
        vintagePriceMap: Map<String, VintagePriceEntity>,
        sortMode: SortMode,
        sortDirection: SortDirection
    ): List<CardUiModel> {
        val cardUiModels = userCards.map { details ->
            val price = details.userCard.manualPrice ?: run {
                val cardId = details.card.id
                val finish = details.userCard.finish
                val condition = details.userCard.condition
                val printing = details.userCard.printing

                priceMap["${cardId}_${finish}_${condition}"]?.marketPrice
                    ?: vintagePriceMap["${cardId}_${finish}_${condition}_${printing}"]?.marketPrice
                    ?: 0.0
            }
            CardUiModel(details, price)
        }

        if (sortMode != SortMode.VALUE) return cardUiModels

        val comparator = compareBy<CardUiModel> { it.price }
        return if (sortDirection == SortDirection.ASCENDING) cardUiModels.sortedWith(comparator) else cardUiModels.sortedWith(
            comparator.reversed()
        )
    }

    private fun computeStats(
        filtered: List<CardUiModel>
    ): Triple<Double, Int, Int> {
        var totalValue = 0.0
        var totalQuantity = 0
        filtered.forEach { item ->
            val quantity = item.details.userCard.quantity
            totalQuantity += quantity
            totalValue += item.price * quantity
        }
        return Triple(totalValue, filtered.size, totalQuantity)
    }

    private fun computeAvailableFilters(userCards: List<CardWithDetails>): Triple<List<String>, List<String>, List<String>> {
        val rarities = userCards.mapNotNull { it.card.rarity }.distinct().sorted()
        val categories = userCards.mapNotNull { it.card.category }.distinct().sorted()
        val types = userCards.flatMap { it.card.types?.split(",") ?: emptyList() }.map { it.trim() }.distinct().sorted()
        return Triple(rarities, categories, types)
    }

    private fun setViewMode(viewMode: ViewMode) {
        viewModelScope.launch {
            userPreferencesRepository.setViewMode(viewMode)
        }
    }

    private fun setSortMode(sortMode: SortMode) {
        viewModelScope.launch {
            userPreferencesRepository.setSortMode(sortMode)
        }
    }

    private fun setSortDirection(direction: SortDirection) {
        _sortDirection.value = direction
    }

    private fun toggleSearchBar() {
        _isSearchBarVisible.update { !it }
        if (!_isSearchBarVisible.value) {
            _searchQuery.value = ""
        }
    }

    private fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    private fun updateListSettings(settings: ListSettings) {
        viewModelScope.launch {
            userPreferencesRepository.setListSettings(settings)
        }
    }

    private fun updateGridSettings(settings: GridSettings) {
        viewModelScope.launch {
            userPreferencesRepository.setGridSettings(settings)
        }
    }

    private fun updatePokedexSettings(settings: PokedexSettings) {
        viewModelScope.launch {
            userPreferencesRepository.setPokedexSettings(settings)
        }
    }

    private fun toggleRarityFilter(rarity: String) {
        _filterSettings.update { current ->
            val newRarities = if (current.rarities.contains(rarity)) {
                current.rarities - rarity
            } else {
                current.rarities + rarity
            }
            current.copy(rarities = newRarities.toImmutableSet())
        }
    }

    private fun toggleCategoryFilter(category: String) {
        _filterSettings.update { current ->
            val newCategories = if (current.categories.contains(category)) {
                current.categories - category
            } else {
                current.categories + category
            }
            current.copy(categories = newCategories.toImmutableSet())
        }
    }

    private fun toggleTypeFilter(type: String) {
        _filterSettings.update { current ->
            val newTypes = if (current.types.contains(type)) {
                current.types - type
            } else {
                current.types + type
            }
            current.copy(types = newTypes.toImmutableSet())
        }
    }

    private fun toggleConditionFilter(condition: String) {
        _filterSettings.update { current ->
            val newConditions = if (current.conditions.contains(condition)) {
                current.conditions - condition
            } else {
                current.conditions + condition
            }
            current.copy(conditions = newConditions.toImmutableSet())
        }
    }

    private fun toggleFinishFilter(finish: String) {
        _filterSettings.update { current ->
            val newFinishes = if (current.finishes.contains(finish)) {
                current.finishes - finish
            } else {
                current.finishes + finish
            }
            current.copy(finishes = newFinishes.toImmutableSet())
        }
    }

    private fun clearFilters() {
        _filterSettings.value = FilterSettings()
    }

    private fun addUserCard(card: TcgDexCard, quantity: Int, condition: String, printing: String, finish: String, folderIds: List<Long>) {
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
            _showSaveSuccess.value = true
        }
    }

    private fun consumeSaveSuccess() {
        _showSaveSuccess.value = false
    }

    private fun addFolder(name: String, icon: String?, color: String?) {
        viewModelScope.launch {
            repository.addFolder(name, icon, color)
        }
    }

    private fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    private fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    private fun toggleSelection(id: Long) {
        _selectionState.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    private fun selectAll() {
        val allIds = state.value.userCards.map { it.userCard.id }.toSet()
        _selectionState.value = allIds
    }

    private fun clearSelection() {
        _selectionState.value = emptySet()
    }

    private fun deleteSelectedCards() {
        viewModelScope.launch {
            repository.deleteUserCards(_selectionState.value.toList())
            clearSelection()
        }
    }

    private fun moveSelectedToFolder(folderId: Long) {
        viewModelScope.launch {
            repository.addCardsToFolder(_selectionState.value.toList(), folderId)
            clearSelection()
        }
    }

    private fun searchRemoteCards(query: String) {
        viewModelScope.launch {
            _isRemoteSearching.value = true
            val results = repository.searchTcgDex(query)
            _remoteSearchResults.value = results
            _isRemoteSearching.value = false
        }
    }
}

class CollectionViewModelFactory(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CollectionViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
