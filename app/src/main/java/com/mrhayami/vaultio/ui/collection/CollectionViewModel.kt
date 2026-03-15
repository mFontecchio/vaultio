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
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ViewMode { LIST, GRID, POKEDEX }
enum class SortMode { NAME, SET, VALUE, DATE_ADDED, RARITY, QUANTITY, NUMBER }
enum class SortDirection { ASCENDING, DESCENDING }

data class ListSettings(
    val showPrices: Boolean = true,
    val isCompact: Boolean = false
)

data class GridSettings(
    val columns: Int = 3,
    val showBadges: Boolean = true
)

data class PokedexSettings(
    val showUncollected: Boolean = true,
    val useShinySprites: Boolean = false
)

data class FilterSettings(
    val rarities: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val conditions: Set<String> = emptySet(),
    val finishes: Set<String> = emptySet()
)

data class PokedexEntry(
    val dexNumber: Int,
    val pokemonName: String?,
    val cardCount: Int,
    val totalQuantity: Int,
    val representativeImage: String?,
    val isCollected: Boolean
)

data class CollectionUiState(
    val viewMode: ViewMode = ViewMode.GRID,
    val sortMode: SortMode = SortMode.DATE_ADDED,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterSettings: FilterSettings = FilterSettings(),
    val userCards: List<CardWithDetails> = emptyList(),
    val filteredUserCards: List<CardWithDetails> = emptyList(),
    val pokedexEntries: List<PokedexEntry> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val selectedFolderId: Long? = null,
    val searchQuery: String = "",
    val isSearchBarVisible: Boolean = false,
    val isLoading: Boolean = true,
    val searchResults: List<TcgDexCard> = emptyList(),
    val isSearching: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val listSettings: ListSettings = ListSettings(),
    val gridSettings: GridSettings = GridSettings(),
    val pokedexSettings: PokedexSettings = PokedexSettings(),
    val sets: Map<String, SetEntity> = emptyMap(),
    val showSaveSuccess: Boolean = false,
    // Available filter options based on current collection
    val availableRarities: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val availableTypes: List<String> = emptyList()
)

class CollectionViewModel(
    private val repository: VaultioRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val moshi = Moshi.Builder().build()
    private val listIntAdapter = moshi.adapter<List<Int>>(Types.newParameterizedType(List::class.java, Integer::class.java))

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    private val _isSearchBarVisible = MutableStateFlow(false)
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    private val _sortMode = MutableStateFlow(SortMode.DATE_ADDED)
    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    private val _filterSettings = MutableStateFlow(FilterSettings())
    
    private val _listSettings = MutableStateFlow(ListSettings())
    private val _gridSettings = MutableStateFlow(GridSettings())
    private val _pokedexSettings = MutableStateFlow(PokedexSettings())

    private val _selectionState = MutableStateFlow(emptySet<Long>())
    private val _showSaveSuccess = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            userPreferencesRepository.viewMode.collect { _viewMode.value = it }
        }
        viewModelScope.launch {
            userPreferencesRepository.sortMode.collect { _sortMode.value = it }
        }
        viewModelScope.launch {
            userPreferencesRepository.listSettings.collect { _listSettings.value = it }
        }
        viewModelScope.launch {
            userPreferencesRepository.gridSettings.collect { _gridSettings.value = it }
        }
        viewModelScope.launch {
            userPreferencesRepository.pokedexSettings.collect { _pokedexSettings.value = it }
        }
    }

    val uiState: StateFlow<CollectionUiState> = combine(
        repository.allUserCards,
        repository.allFolders,
        repository.allFolderCardCrossRefs,
        _searchQuery,
        _selectedFolderId,
        _isSearchBarVisible,
        _viewMode,
        _sortMode,
        _sortDirection,
        _filterSettings,
        _selectionState,
        _listSettings,
        _gridSettings,
        _pokedexSettings,
        repository.allSets.map { sets -> sets.associateBy { it.id } },
        _showSaveSuccess
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val userCards = args[0] as List<CardWithDetails>
        @Suppress("UNCHECKED_CAST")
        val folders = args[1] as List<FolderEntity>
        @Suppress("UNCHECKED_CAST")
        val crossRefs = args[2] as List<FolderCardCrossRef>
        val searchQuery = args[3] as String
        val selectedFolderId = args[4] as Long?
        val isSearchBarVisible = args[5] as Boolean
        val viewMode = args[6] as ViewMode
        val sortMode = args[7] as SortMode
        val sortDirection = args[8] as SortDirection
        val filterSettings = args[9] as FilterSettings
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[10] as Set<Long>
        val listSettings = args[11] as ListSettings
        val gridSettings = args[12] as GridSettings
        val pokedexSettings = args[13] as PokedexSettings
        @Suppress("UNCHECKED_CAST")
        val sets = args[14] as Map<String, SetEntity>
        val showSaveSuccess = args[15] as Boolean

        // Calculate available filter options from the base userCards
        val availableRarities = userCards.mapNotNull { it.card.rarity }.distinct().sorted()
        val availableCategories = userCards.mapNotNull { it.card.category }.distinct().sorted()
        val availableTypes = userCards.flatMap { it.card.types?.split(",") ?: emptyList() }
            .map { it.trim() }.distinct().sorted()

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

        // Sorting
        val comparator = when (sortMode) {
            SortMode.NAME -> compareBy<CardWithDetails> { it.card.name }
            SortMode.SET -> compareBy<CardWithDetails> { it.set.releaseDate }.thenBy { it.card.localId.padStart(5, '0') }
            SortMode.VALUE -> compareBy<CardWithDetails> { it.userCard.manualPrice ?: 0.0 }
            SortMode.DATE_ADDED -> compareBy<CardWithDetails> { it.userCard.dateAdded }
            SortMode.RARITY -> compareBy<CardWithDetails> { it.card.rarity }
            SortMode.QUANTITY -> compareBy<CardWithDetails> { it.userCard.quantity }
            SortMode.NUMBER -> compareBy<CardWithDetails> { it.card.localId.padStart(5, '0') }
        }

        filtered = if (sortDirection == SortDirection.ASCENDING) {
            filtered.sortedWith(comparator)
        } else {
            filtered.sortedWith(comparator.reversed())
        }

        // Pokedex entries should respect the folder filter and other filters if we want consistency
        val pokedexUserCards = if (selectedFolderId == null && filterSettings == FilterSettings()) userCards else filtered
        val effectivePokedexSettings = if (selectedFolderId != null || filterSettings != FilterSettings()) pokedexSettings.copy(showUncollected = false) else pokedexSettings
        var pokedexEntries = computePokedexEntries(pokedexUserCards, effectivePokedexSettings)
        
        if (searchQuery.isNotBlank()) {
            pokedexEntries = pokedexEntries.filter { entry ->
                entry.pokemonName?.contains(searchQuery, ignoreCase = true) == true ||
                entry.dexNumber.toString() == searchQuery
            }
        }

        CollectionUiState(
            viewMode = viewMode,
            sortMode = sortMode,
            sortDirection = sortDirection,
            filterSettings = filterSettings,
            userCards = userCards,
            filteredUserCards = filtered,
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
            sets = sets,
            showSaveSuccess = showSaveSuccess,
            availableRarities = availableRarities,
            availableCategories = availableCategories,
            availableTypes = availableTypes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionUiState()
    )

    private fun computePokedexEntries(userCards: List<CardWithDetails>, settings: PokedexSettings): List<PokedexEntry> {
        val buckets = mutableMapOf<Int, MutableList<CardWithDetails>>()
        
        // Map to store normalized name for each Dex ID
        val idToNameMap = mutableMapOf<Int, String>()
        val nameToDexIdMap = mutableMapOf<String, Int>()

        // First pass: group by explicit Dex IDs and build name mappings
        userCards.forEach { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = try {
                card.dexIds?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(card.dexId?.toIntOrNull())
            } catch (e: Exception) {
                listOfNotNull(card.dexId?.toIntOrNull())
            }

            val normalizedName = card.pokemonName ?: PokemonUtils.extractPokemonName(card.name)

            dexIds.forEach { id ->
                buckets.getOrPut(id) { mutableListOf() }.add(cardWithDetails)
                // Always keep the best (shortest or most standard) normalized name for the ID
                val currentName = idToNameMap[id]
                if (currentName == null || normalizedName.length < currentName.length) {
                    idToNameMap[id] = normalizedName
                }
                nameToDexIdMap[normalizedName.lowercase()] = id
            }
        }

        // Second pass: handle cards missing Dex IDs but having a name that matches a known Dex ID
        userCards.forEach { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = try {
                card.dexIds?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(card.dexId?.toIntOrNull())
            } catch (e: Exception) {
                listOfNotNull(card.dexId?.toIntOrNull())
            }

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
                // Prioritize the name we mapped for this ID
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

    fun setViewMode(viewMode: ViewMode) {
        viewModelScope.launch {
            userPreferencesRepository.setViewMode(viewMode)
        }
    }

    fun setSortMode(sortMode: SortMode) {
        viewModelScope.launch {
            userPreferencesRepository.setSortMode(sortMode)
        }
    }

    fun setSortDirection(direction: SortDirection) {
        _sortDirection.value = direction
    }

    fun toggleSearchBar() {
        _isSearchBarVisible.value = !_isSearchBarVisible.value
        if (!_isSearchBarVisible.value) {
            _searchQuery.value = ""
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun updateListSettings(settings: ListSettings) {
        viewModelScope.launch {
            userPreferencesRepository.setListSettings(settings)
        }
    }

    fun updateGridSettings(settings: GridSettings) {
        viewModelScope.launch {
            userPreferencesRepository.setGridSettings(settings)
        }
    }

    fun updatePokedexSettings(settings: PokedexSettings) {
        viewModelScope.launch {
            userPreferencesRepository.setPokedexSettings(settings)
        }
    }

    fun toggleRarityFilter(rarity: String) {
        _filterSettings.update { current ->
            val newRarities = if (current.rarities.contains(rarity)) {
                current.rarities - rarity
            } else {
                current.rarities + rarity
            }
            current.copy(rarities = newRarities)
        }
    }

    fun toggleCategoryFilter(category: String) {
        _filterSettings.update { current ->
            val newCategories = if (current.categories.contains(category)) {
                current.categories - category
            } else {
                current.categories + category
            }
            current.copy(categories = newCategories)
        }
    }

    fun toggleTypeFilter(type: String) {
        _filterSettings.update { current ->
            val newTypes = if (current.types.contains(type)) {
                current.types - type
            } else {
                current.types + type
            }
            current.copy(types = newTypes)
        }
    }

    fun toggleConditionFilter(condition: String) {
        _filterSettings.update { current ->
            val newConditions = if (current.conditions.contains(condition)) {
                current.conditions - condition
            } else {
                current.conditions + condition
            }
            current.copy(conditions = newConditions)
        }
    }

    fun toggleFinishFilter(finish: String) {
        _filterSettings.update { current ->
            val newFinishes = if (current.finishes.contains(finish)) {
                current.finishes - finish
            } else {
                current.finishes + finish
            }
            current.copy(finishes = newFinishes)
        }
    }

    fun clearFilters() {
        _filterSettings.value = FilterSettings()
    }

    fun addUserCard(card: TcgDexCard, quantity: Int, condition: String, printing: String, finish: String, folderIds: List<Long> = emptyList()) {
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

    fun consumeSaveSuccess() {
        _showSaveSuccess.value = false
    }

    fun addFolder(name: String, icon: String?, color: String?) {
        viewModelScope.launch {
            repository.addFolder(name, icon, color)
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectionState.value
        _selectionState.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAll() {
        val allIds = uiState.value.userCards.map { it.userCard.id }.toSet()
        _selectionState.value = allIds
    }

    fun clearSelection() {
        _selectionState.value = emptySet()
    }

    fun deleteSelectedCards() {
        viewModelScope.launch {
            repository.deleteUserCards(_selectionState.value.toList())
            clearSelection()
        }
    }

    fun moveSelectedToFolder(folderId: Long) {
        viewModelScope.launch {
            repository.addCardsToFolder(_selectionState.value.toList(), folderId)
            clearSelection()
        }
    }

    // This is for the remote search in the "Add Card" modal
    private val _remoteSearchResults = MutableStateFlow<List<TcgDexCard>>(emptyList())
    private val _isRemoteSearching = MutableStateFlow(false)
    
    val remoteSearchState = combine(_remoteSearchResults, _isRemoteSearching) { results, loading ->
        Pair(results, loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), false))

    fun searchRemoteCards(query: String) {
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
