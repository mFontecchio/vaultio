package com.mrhayami.vaultio.ui.collection

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

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
    val useShinySprites: Boolean = false,
    val useOfficialArt: Boolean = false
)

@Immutable
data class FilterSettings(
    val rarities: ImmutableSet<String> = persistentSetOf(),
    val categories: ImmutableSet<String> = persistentSetOf(),
    val types: ImmutableSet<String> = persistentSetOf(),
    val conditions: ImmutableSet<String> = persistentSetOf(),
    val finishes: ImmutableSet<String> = persistentSetOf()
)

@Immutable
data class PokedexEntry(
    val dexNumber: Int,
    val pokemonName: String?,
    val cardCount: Int,
    val totalQuantity: Int,
    val representativeImage: String?,
    val spriteUrl: String,
    val isCollected: Boolean
)

@Immutable
data class CardUiModel(
    val details: CardWithDetails,
    val price: Double
)

@Immutable
data class CollectionUiState(
    val viewMode: ViewMode = ViewMode.GRID,
    val sortMode: SortMode = SortMode.DATE_ADDED,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val filterSettings: FilterSettings = FilterSettings(),
    val userCards: ImmutableList<CardWithDetails> = persistentListOf(),
    val filteredUserCards: ImmutableList<CardUiModel> = persistentListOf(),
    val pokedexEntries: ImmutableList<PokedexEntry> = persistentListOf(),
    val folders: ImmutableList<FolderEntity> = persistentListOf(),
    val selectedFolderId: Long? = null,
    val searchQuery: String = "",
    val isSearchBarVisible: Boolean = false,
    val isLoading: Boolean = true,
    val searchResults: ImmutableList<TcgDexCard> = persistentListOf(),
    val isSearching: Boolean = false,
    val selectedIds: ImmutableSet<Long> = persistentSetOf(),
    val isSelectionMode: Boolean = false,
    val selectedDexId: Int? = null,
    val collectedCardsForDex: ImmutableList<CardUiModel> = persistentListOf(),
    val listSettings: ListSettings = ListSettings(),
    val gridSettings: GridSettings = GridSettings(),
    val pokedexSettings: PokedexSettings = PokedexSettings(),
    val preferSetLogo: Boolean = true,
    val sets: ImmutableMap<String, SetEntity> = persistentMapOf(),
    val showSaveSuccess: Boolean = false,
    // Available filter options based on current collection
    val availableRarities: ImmutableList<String> = persistentListOf(),
    val availableCategories: ImmutableList<String> = persistentListOf(),
    val availableTypes: ImmutableList<String> = persistentListOf(),
    val totalValue: Double = 0.0,
    val totalCount: Int = 0,
    val totalQuantity: Int = 0,
    val newSetsToDownload: ImmutableList<SetEntity> = persistentListOf(),
    val isDownloadingNewSets: Boolean = false
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

    // Auto-check for new sets
    data object OnDownloadNewSets : CollectionEvent
    data object OnDismissNewSetsPrompt : CollectionEvent
    
    // Import/Export
    data class OnExportCollection(val folderIds: List<Long>? = null) : CollectionEvent
    data class OnImportCollection(val json: String) : CollectionEvent

    // Pokedex Detail
    data class OnDexClick(val dexId: Int) : CollectionEvent
    data object OnDismissDexDetail : CollectionEvent
}

sealed interface CollectionEffect {
    data class ShowToast(val message: String) : CollectionEffect
    data class ExportCollection(val json: String) : CollectionEffect
    data object ImportSuccess : CollectionEffect
    
    sealed interface Navigation : CollectionEffect {
        data object ToScanner : Navigation
        data class ToCardDetail(val userCardId: Long) : Navigation
    }
}
