package com.mrhayami.vaultio.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ViewMode { LIST, GRID, POKEDEX }
enum class SortMode { NAME, SET, VALUE, DATE_ADDED }

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
    val showSaveSuccess: Boolean = false
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
        _searchQuery,
        _selectedFolderId,
        _isSearchBarVisible,
        _viewMode,
        _sortMode,
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
        val searchQuery = args[2] as String
        val selectedFolderId = args[3] as Long?
        val isSearchBarVisible = args[4] as Boolean
        val viewMode = args[5] as ViewMode
        val sortMode = args[6] as SortMode
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[7] as Set<Long>
        val listSettings = args[8] as ListSettings
        val gridSettings = args[9] as GridSettings
        val pokedexSettings = args[10] as PokedexSettings
        @Suppress("UNCHECKED_CAST")
        val sets = args[11] as Map<String, SetEntity>
        val showSaveSuccess = args[12] as Boolean

        val filtered = userCards.filter { card ->
            val matchesSearch = if (searchQuery.isBlank()) true 
                else card.card.name.contains(searchQuery, ignoreCase = true) || 
                     card.card.pokemonName?.contains(searchQuery, ignoreCase = true) == true ||
                     card.set.name.contains(searchQuery, ignoreCase = true)
            
            val matchesFolder = if (selectedFolderId == null) true
                else false // Folders need a more complex join or pre-filtered flow if we want this to work here efficiently
            
            matchesSearch
        }

        val pokedexEntries = computePokedexEntries(userCards, pokedexSettings)

        CollectionUiState(
            viewMode = viewMode,
            sortMode = sortMode,
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
            showSaveSuccess = showSaveSuccess
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionUiState()
    )

    private fun computePokedexEntries(userCards: List<CardWithDetails>, settings: PokedexSettings): List<PokedexEntry> {
        val buckets = mutableMapOf<Int, MutableList<CardWithDetails>>()
        
        // Map to store normalized name to Dex ID mapping for fuzzy matching cards missing Dex IDs
        val nameToDexIdMap = mutableMapOf<String, Int>()

        // First pass: group by explicit Dex IDs and build name->ID map
        userCards.forEach { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = try {
                card.dexIds?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(card.dexId?.toIntOrNull())
            } catch (e: Exception) {
                listOfNotNull(card.dexId?.toIntOrNull())
            }

            dexIds.forEach { id ->
                buckets.getOrPut(id) { mutableListOf() }.add(cardWithDetails)
                card.pokemonName?.let { nameToDexIdMap[it.lowercase()] = id }
            }
        }

        // Second pass: handle cards missing Dex IDs but having a pokemonName that matches a known Dex ID
        userCards.forEach { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = try {
                card.dexIds?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(card.dexId?.toIntOrNull())
            } catch (e: Exception) {
                listOfNotNull(card.dexId?.toIntOrNull())
            }

            if (dexIds.isEmpty()) {
                card.pokemonName?.lowercase()?.let { normalizedName ->
                    nameToDexIdMap[normalizedName]?.let { id ->
                        val bucket = buckets.getOrPut(id) { mutableListOf() }
                        if (!bucket.any { it.userCard.id == cardWithDetails.userCard.id }) {
                            bucket.add(cardWithDetails)
                        }
                    }
                }
            }
        }

        val entries = mutableListOf<PokedexEntry>()
        val maxDexNumber = if (settings.showUncollected) 1025 else buckets.keys.maxOrNull() ?: 0

        for (i in 1..maxDexNumber) {
            val cardsInBucket = buckets[i] ?: emptyList()
            if (cardsInBucket.isNotEmpty() || settings.showUncollected) {
                // Prioritize normalized pokemonName for the entry title
                val bestName = cardsInBucket.mapNotNull { it.card.pokemonName }.firstOrNull() 
                    ?: cardsInBucket.firstOrNull()?.card?.name
                
                entries.add(
                    PokedexEntry(
                        dexNumber = i,
                        pokemonName = bestName,
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

    fun addUserCard(card: TcgDexCard, quantity: Int, condition: String, printing: String, finish: String) {
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
