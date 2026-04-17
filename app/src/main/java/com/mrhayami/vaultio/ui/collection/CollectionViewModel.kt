package com.mrhayami.vaultio.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.PokemonUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.FolderCardCrossRef
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    // Base Data Flows
    private val _allCards = repository.allUserCards
    private val _allFolders = repository.allFolders
    private val _allCrossRefs = repository.allFolderCardCrossRefs
    private val _allPrices = repository.allPrices
    private val _allVintagePrices = repository.allVintagePrices
    private val _allSets = repository.allSets.map { sets -> sets.associateBy { it.id } }

    init {
        // Observe all data and update state
        combine(
            userPreferencesRepository.viewMode,
            userPreferencesRepository.sortMode,
            _sortDirection,
            _filterSettings,
            _searchQuery,
            _selectedFolderId,
            _isSearchBarVisible,
            _allCards,
            _allFolders,
            _allCrossRefs,
            _allPrices,
            _allVintagePrices,
            userPreferencesRepository.listSettings,
            userPreferencesRepository.gridSettings,
            userPreferencesRepository.pokedexSettings,
            userPreferencesRepository.preferSetLogo,
            _allSets,
            _showSaveSuccess,
            _selectionState,
            _remoteSearchResults,
            _isRemoteSearching
        ) { flows ->
            val viewMode = flows[0] as ViewMode
            val sortMode = flows[1] as SortMode
            val sortDirection = flows[2] as SortDirection
            val filterSettings = flows[3] as FilterSettings
            val searchQuery = flows[4] as String
            val selectedFolderId = flows[5] as Long?
            val isSearchBarVisible = flows[6] as Boolean
            val allCards = flows[7] as List<CardWithDetails>
            val folders = flows[8] as List<FolderEntity>
            val crossRefs = flows[9] as List<FolderCardCrossRef>
            val allPrices = flows[10] as List<PriceEntity>
            val allVintagePrices = flows[11] as List<VintagePriceEntity>
            val listSettings = flows[12] as ListSettings
            val gridSettings = flows[13] as GridSettings
            val pokedexSettings = flows[14] as PokedexSettings
            val preferSetLogo = flows[15] as Boolean
            val setsMap = flows[16] as Map<String, SetEntity>
            val showSaveSuccess = flows[17] as Boolean
            val selectedIds = flows[18] as Set<Long>
            val remoteResults = flows[19] as List<TcgDexCard>
            val isRemoteSearching = flows[20] as Boolean

            // Filter logic
            val filteredCards = filterAndSortCards(
                allCards, crossRefs, searchQuery, selectedFolderId,
                sortMode, sortDirection, filterSettings, allPrices, allVintagePrices
            )

            // Stats logic
            val stats = computeStats(filteredCards, allPrices, allVintagePrices)

            // Available filters logic
            val availableFilters = computeAvailableFilters(allCards)

            // Pokedex logic
            val pokedexUserCards = if (selectedFolderId == null && filterSettings == FilterSettings()) allCards else filteredCards
            val effectivePokedexSettings = if (selectedFolderId != null || filterSettings != FilterSettings()) {
                pokedexSettings.copy(showUncollected = false)
            } else pokedexSettings
            
            var pokedexEntries = computePokedexEntries(pokedexUserCards, effectivePokedexSettings)
            if (searchQuery.isNotBlank()) {
                pokedexEntries = pokedexEntries.filter { entry ->
                    entry.pokemonName?.contains(searchQuery, ignoreCase = true) == true ||
                    entry.dexNumber.toString() == searchQuery
                }
            }

            updateState {
                copy(
                    viewMode = viewMode,
                    sortMode = sortMode,
                    sortDirection = sortDirection,
                    filterSettings = filterSettings,
                    userCards = allCards,
                    filteredUserCards = filteredCards,
                    pokedexEntries = pokedexEntries,
                    folders = folders,
                    selectedFolderId = selectedFolderId,
                    searchQuery = searchQuery,
                    isSearchBarVisible = isSearchBarVisible,
                    isLoading = false,
                    selectedIds = selectedIds,
                    isSelectionMode = selectedIds.isNotEmpty(),
                    listSettings = listSettings,
                    gridSettings = gridSettings,
                    pokedexSettings = pokedexSettings,
                    preferSetLogo = preferSetLogo,
                    sets = setsMap,
                    showSaveSuccess = showSaveSuccess,
                    availableRarities = availableFilters.first,
                    availableCategories = availableFilters.second,
                    availableTypes = availableFilters.third,
                    totalValue = stats.first,
                    totalCount = stats.second,
                    totalQuantity = stats.third,
                    searchResults = remoteResults,
                    isSearching = isRemoteSearching
                )
            }
        }.launchIn(viewModelScope)
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
            is CollectionEvent.OnExportCollection -> exportCollection(event.folderIds)
            is CollectionEvent.OnImportCollection -> importCollection(event.json)
        }
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
        allPrices: List<PriceEntity>,
        allVintagePrices: List<VintagePriceEntity>
    ): List<CardWithDetails> {
        var filtered = userCards.filter { cardWithDetails ->
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

        val comparator = when (sortMode) {
            SortMode.NAME -> compareBy<CardWithDetails> { it.card.name }
            SortMode.SET -> compareBy<CardWithDetails> { it.set.releaseDate }.thenBy { it.card.localId.padStart(5, '0') }
            SortMode.VALUE -> compareBy<CardWithDetails> { item ->
                item.userCard.manualPrice ?: run {
                    val cardId = item.card.id
                    val finish = item.userCard.finish
                    val condition = item.userCard.condition
                    val printing = item.userCard.printing
                    allPrices.find { it.cardId == cardId && it.finish == finish && it.condition == condition }?.marketPrice
                        ?: allVintagePrices.find { it.cardId == cardId && it.finish == finish && it.condition == condition && it.printing == printing }?.marketPrice
                        ?: 0.0
                }
            }
            SortMode.DATE_ADDED -> compareBy<CardWithDetails> { it.userCard.dateAdded }
            SortMode.RARITY -> compareBy<CardWithDetails> { it.card.rarity }
            SortMode.QUANTITY -> compareBy<CardWithDetails> { it.userCard.quantity }
            SortMode.NUMBER -> compareBy<CardWithDetails> { it.card.localId.padStart(5, '0') }
        }

        filtered = if (sortDirection == SortDirection.ASCENDING) filtered.sortedWith(comparator) else filtered.sortedWith(comparator.reversed())
        return filtered
    }

    private fun computeStats(
        filtered: List<CardWithDetails>,
        allPrices: List<PriceEntity>,
        allVintagePrices: List<VintagePriceEntity>
    ): Triple<Double, Int, Int> {
        var totalValue = 0.0
        var totalQuantity = 0
        filtered.forEach { item ->
            val quantity = item.userCard.quantity
            totalQuantity += quantity
            val price = item.userCard.manualPrice ?: run {
                val cardId = item.card.id
                val finish = item.userCard.finish
                val condition = item.userCard.condition
                val printing = item.userCard.printing
                allPrices.find { it.cardId == cardId && it.finish == finish && it.condition == condition }?.marketPrice
                    ?: allVintagePrices.find { it.cardId == cardId && it.finish == finish && it.condition == condition && it.printing == printing }?.marketPrice
                    ?: 0.0
            }
            totalValue += price * quantity
        }
        return Triple(totalValue, filtered.size, totalQuantity)
    }

    private fun computeAvailableFilters(userCards: List<CardWithDetails>): Triple<List<String>, List<String>, List<String>> {
        val rarities = userCards.mapNotNull { it.card.rarity }.distinct().sorted()
        val categories = userCards.mapNotNull { it.card.category }.distinct().sorted()
        val types = userCards.flatMap { it.card.types?.split(",") ?: emptyList() }.map { it.trim() }.distinct().sorted()
        return Triple(rarities, categories, types)
    }

    private fun computePokedexEntries(userCards: List<CardWithDetails>, settings: PokedexSettings): List<PokedexEntry> {
        val buckets = mutableMapOf<Int, MutableList<CardWithDetails>>()
        val idToNameMap = mutableMapOf<Int, String>()
        val nameToDexIdMap = mutableMapOf<String, Int>()

        userCards.forEach { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = PokemonUtils.parseDexIds(card.dexIds, card.dexId)
            val normalizedName = card.pokemonName ?: PokemonUtils.extractPokemonName(card.name)

            dexIds.forEach { id ->
                buckets.getOrPut(id) { mutableListOf() }.add(cardWithDetails)
                val currentName = idToNameMap[id]
                if (currentName == null || normalizedName.length < currentName.length) {
                    idToNameMap[id] = normalizedName
                }
                nameToDexIdMap[normalizedName.lowercase()] = id
            }
        }

        userCards.forEach { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = PokemonUtils.parseDexIds(card.dexIds, card.dexId)

            if (dexIds.isEmpty()) {
                val normalizedName = card.pokemonName ?: PokemonUtils.extractPokemonName(card.name)
                nameToDexIdMap[normalizedName.lowercase()]?.let { id ->
                    val bucket = buckets.getOrPut(id) { mutableListOf() }
                    if (!bucket.any { it.userCard.id == cardWithDetails.userCard.id }) {
                        bucket.add(cardWithDetails)
                    }
                }
            }
        }

        val entries = mutableListOf<PokedexEntry>()
        val maxDexNumber = if (settings.showUncollected) 1025 else buckets.keys.maxOrNull() ?: 0

        for (i in 1..maxDexNumber) {
            val cardsInBucket = buckets[i] ?: emptyList()
            if (cardsInBucket.isNotEmpty() || settings.showUncollected) {
                val displayName = idToNameMap[i] 
                    ?: cardsInBucket.mapNotNull { it.card.pokemonName }.firstOrNull()
                    ?: cardsInBucket.firstOrNull()?.card?.let { PokemonUtils.extractPokemonName(it.name) }
                
                entries.add(
                    PokedexEntry(
                        dexNumber = i,
                        pokemonName = displayName,
                        cardCount = cardsInBucket.size,
                        totalQuantity = cardsInBucket.sumOf { it.userCard.quantity },
                        representativeImage = cardsInBucket.firstOrNull()?.card?.image,
                        isCollected = cardsInBucket.isNotEmpty()
                    )
                )
            }
        }
        return entries
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
            current.copy(rarities = newRarities)
        }
    }

    private fun toggleCategoryFilter(category: String) {
        _filterSettings.update { current ->
            val newCategories = if (current.categories.contains(category)) {
                current.categories - category
            } else {
                current.categories + category
            }
            current.copy(categories = newCategories)
        }
    }

    private fun toggleTypeFilter(type: String) {
        _filterSettings.update { current ->
            val newTypes = if (current.types.contains(type)) {
                current.types - type
            } else {
                current.types + type
            }
            current.copy(types = newTypes)
        }
    }

    private fun toggleConditionFilter(condition: String) {
        _filterSettings.update { current ->
            val newConditions = if (current.conditions.contains(condition)) {
                current.conditions - condition
            } else {
                current.conditions + condition
            }
            current.copy(conditions = newConditions)
        }
    }

    private fun toggleFinishFilter(finish: String) {
        _filterSettings.update { current ->
            val newFinishes = if (current.finishes.contains(finish)) {
                current.finishes - finish
            } else {
                current.finishes + finish
            }
            current.copy(finishes = newFinishes)
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
