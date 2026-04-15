package com.mrhayami.vaultio.ui.collection

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard

enum class ViewMode { LIST, GRID, POKEDEX }
enum class SortMode { NAME, SET, VALUE, DATE_ADDED, RARITY, QUANTITY, NUMBER }
enum class SortDirection { ASCENDING, DESCENDING }

@Immutable
data class ListSettings(
    val showPrices: Boolean = true,
    val isCompact: Boolean = false
)

@Immutable
data class GridSettings(
    val columns: Int = 3,
    val showBadges: Boolean = true
)

@Immutable
data class PokedexSettings(
    val showUncollected: Boolean = true,
    val useShinySprites: Boolean = false
)

@Immutable
data class FilterSettings(
    val rarities: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val conditions: Set<String> = emptySet(),
    val finishes: Set<String> = emptySet()
)

@Immutable
data class PokedexEntry(
    val dexNumber: Int,
    val pokemonName: String?,
    val cardCount: Int,
    val totalQuantity: Int,
    val representativeImage: String?,
    val isCollected: Boolean
)

@Immutable
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
    val preferSetLogo: Boolean = true,
    val sets: Map<String, SetEntity> = emptyMap(),
    val showSaveSuccess: Boolean = false,
    // Available filter options based on current collection
    val availableRarities: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val availableTypes: List<String> = emptyList(),
    val totalValue: Double = 0.0,
    val totalCount: Int = 0,
    val totalQuantity: Int = 0
)

sealed interface CollectionEvent {
    // UI Interactions
    data class OnViewModeChange(val viewMode: ViewMode) : CollectionEvent
    data class OnSortModeChange(val sortMode: SortMode) : CollectionEvent
    data class OnSortDirectionChange(val direction: SortDirection) : CollectionEvent
    data object OnToggleSearchBar : CollectionEvent
    data class OnSearchQueryChange(val query: String) : CollectionEvent
    data class OnFolderSelect(val folderId: Long?) : CollectionEvent
    
    // Settings Updates
    data class OnUpdateListSettings(val settings: ListSettings) : CollectionEvent
    data class OnUpdateGridSettings(val settings: GridSettings) : CollectionEvent
    data class OnUpdatePokedexSettings(val settings: PokedexSettings) : CollectionEvent
    
    // Filters
    data class OnToggleRarityFilter(val rarity: String) : CollectionEvent
    data class OnToggleCategoryFilter(val category: String) : CollectionEvent
    data class OnToggleTypeFilter(val type: String) : CollectionEvent
    data class OnToggleConditionFilter(val condition: String) : CollectionEvent
    data class OnToggleFinishFilter(val finish: String) : CollectionEvent
    data object OnClearFilters : CollectionEvent
    
    // Selection
    data class OnToggleSelection(val id: Long) : CollectionEvent
    data object OnSelectAll : CollectionEvent
    data object OnClearSelection : CollectionEvent
    data object OnDeleteSelectedCards : CollectionEvent
    data class OnMoveSelectedToFolder(val folderId: Long) : CollectionEvent
    
    // Folder Management
    data class OnAddFolder(val name: String, val icon: String?, val color: String?) : CollectionEvent
    data class OnUpdateFolder(val folder: FolderEntity) : CollectionEvent
    data class OnDeleteFolder(val folder: FolderEntity) : CollectionEvent
    
    // Remote Search
    data class OnSearchRemoteCards(val query: String) : CollectionEvent
    data class OnAddUserCard(
        val card: TcgDexCard,
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String,
        val folderIds: List<Long>
    ) : CollectionEvent
    
    data object OnConsumeSaveSuccess : CollectionEvent
}

sealed interface CollectionEffect {
    data class ShowToast(val message: String) : CollectionEffect
    
    sealed interface Navigation : CollectionEffect {
        data object ToScanner : Navigation
        data class ToCardDetail(val userCardId: Long) : Navigation
    }
}
