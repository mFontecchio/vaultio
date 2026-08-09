@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.mrhayami.vaultio.ui.collection

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.collection.components.AddCardModal
import com.mrhayami.vaultio.ui.collection.components.CollectionGridView
import com.mrhayami.vaultio.ui.collection.components.CollectionListView
import com.mrhayami.vaultio.ui.collection.components.ExportSelectionDialog
import com.mrhayami.vaultio.ui.collection.components.FolderDialog
import com.mrhayami.vaultio.ui.collection.components.PokedexView
import com.mrhayami.vaultio.ui.collection.components.SortFilterSheet
import com.mrhayami.vaultio.ui.collection.components.StickyControls
import com.mrhayami.vaultio.ui.collection.components.ViewSettingsSheet
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.EmptyState
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews
import kotlinx.coroutines.launch

@Composable
fun CollectionScreen(
    repository: VaultioRepository,
    userPreferencesRepository: UserPreferencesRepository,
    onNavigateToScanner: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit,
    viewModel: CollectionViewModel = viewModel(
        factory = CollectionViewModelFactory(repository, userPreferencesRepository)
    )
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            pendingExportJson?.let { json ->
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                Toast.makeText(context, "Collection exported successfully", Toast.LENGTH_SHORT).show()
                pendingExportJson = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val json = stream.bufferedReader().use { reader -> reader.readText() }
                viewModel.onEvent(CollectionEvent.OnImportCollection(json))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is CollectionEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is CollectionEffect.ExportCollection -> {
                    pendingExportJson = effect.json
                    exportLauncher.launch("vaultio_collection_${System.currentTimeMillis()}.json")
                }
                CollectionEffect.ImportSuccess -> {
                    // Handled via toast in VM effect
                }
                is CollectionEffect.Navigation -> {
                    when (effect) {
                        CollectionEffect.Navigation.ToScanner -> onNavigateToScanner()
                        is CollectionEffect.Navigation.ToCardDetail -> onNavigateToCardDetail(effect.userCardId)
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            Toast.makeText(context, "Card added to collection", Toast.LENGTH_SHORT).show()
            viewModel.onEvent(CollectionEvent.OnConsumeSaveSuccess)
        }
    }

    CollectionContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToScanner = onNavigateToScanner,
        onNavigateToCardDetail = onNavigateToCardDetail,
        importLauncher = importLauncher
    )
}

@Composable
fun CollectionContent(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit,
    importLauncher: ActivityResultLauncher<Array<String>>,
    modifier: Modifier = Modifier
) {
    var showAddCardModal by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf<FolderEntity?>(null) }
    var showManageFolders by remember { mutableStateOf(false) }
    var showViewSettings by remember { mutableStateOf(false) }
    var showSortFilterSheet by remember { mutableStateOf(false) }
    var showMoveToFolderSheet by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val onCardClick = remember(uiState.isSelectionMode, onEvent, onNavigateToCardDetail) {
        { id: Long -> if (uiState.isSelectionMode) onEvent(CollectionEvent.OnToggleSelection(id)) else onNavigateToCardDetail(id) }
    }
    val onCardLongClick = remember(onEvent) {
        { id: Long -> onEvent(CollectionEvent.OnToggleSelection(id)) }
    }
    val onDexClick = remember(onEvent) {
        { dexId: Int -> onEvent(CollectionEvent.OnDexClick(dexId)) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CollectionTopBar(
                uiState = uiState,
                onEvent = onEvent,
                onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                onExportClick = { showExportDialog = true },
                onShowViewSettings = { showViewSettings = true },
                onShowManageFolders = { showManageFolders = true },
                onShowMoveToFolder = { showMoveToFolderSheet = true }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                CollectionFab(
                    expanded = fabExpanded,
                    onToggle = { fabExpanded = !fabExpanded },
                    onAddFolder = {
                        showFolderDialog = FolderEntity(name = "")
                        fabExpanded = false
                    },
                    onAddManual = {
                        showAddCardModal = true
                        fabExpanded = false
                    },
                    onScan = {
                        onNavigateToScanner()
                        fabExpanded = false
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            AnimatedVisibility(visible = uiState.isDownloadingNewSets) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            StickyControls(
                viewMode = uiState.viewMode,
                sortMode = uiState.sortMode,
                filterSettings = uiState.filterSettings,
                folders = uiState.folders,
                selectedFolderId = uiState.selectedFolderId,
                totalQuantity = uiState.totalQuantity,
                totalValue = uiState.totalValue,
                pokedexCollectedCount = uiState.pokedexEntries.count { it.isCollected },
                pokedexTotalCount = uiState.pokedexEntries.size,
                onEvent = onEvent,
                onSortClick = { showSortFilterSheet = true }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.filteredUserCards.isEmpty() && uiState.viewMode != ViewMode.POKEDEX) {
                    when {
                        uiState.searchQuery.isNotEmpty() -> {
                            Text(
                                "No cards match your search.",
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        uiState.selectedFolderId != null -> {
                            Text(
                                "This folder is empty.",
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        else -> {
                            EmptyState(
                                title = "Your collection is empty",
                                message = "Scan cards or add them manually to start building your collection.",
                                icon = Icons.Rounded.QrCodeScanner,
                                primaryLabel = "Scan cards",
                                onPrimaryClick = onNavigateToScanner,
                                secondaryLabel = "Add manually",
                                onSecondaryClick = { showAddCardModal = true },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                } else {
                    AnimatedViewModeContent(
                        viewMode = uiState.viewMode,
                        uiState = uiState,
                        onCardClick = onCardClick,
                        onCardLongClick = onCardLongClick,
                        onDexClick = onDexClick
                    )
                }
            }
        }
    }

    // Dialogs and Bottom Sheets
    if (showExportDialog) {
        ExportSelectionDialog(
            folders = uiState.folders,
            onDismiss = { showExportDialog = false },
            onConfirm = { folderIds ->
                onEvent(CollectionEvent.OnExportCollection(folderIds))
                showExportDialog = false
            }
        )
    }

    if (showViewSettings) {
        ModalBottomSheet(onDismissRequest = { showViewSettings = false }) {
            ViewSettingsSheet(
                viewMode = uiState.viewMode,
                listSettings = uiState.listSettings,
                gridSettings = uiState.gridSettings,
                pokedexSettings = uiState.pokedexSettings,
                onEvent = onEvent
            )
        }
    }

    if (showSortFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showSortFilterSheet = false }) {
            SortFilterSheet(
                sortMode = uiState.sortMode,
                sortDirection = uiState.sortDirection,
                filterSettings = uiState.filterSettings,
                availableRarities = uiState.availableRarities,
                availableCategories = uiState.availableCategories,
                availableTypes = uiState.availableTypes,
                onEvent = onEvent
            )
        }
    }

    if (showAddCardModal) {
        ModalBottomSheet(
            onDismissRequest = { showAddCardModal = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            AddCardModal(
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                folders = uiState.folders,
                setsMap = uiState.sets,
                onEvent = onEvent,
                onWishlistConfirm = { card, quantity, condition, printing, finish ->
                    onEvent(
                        CollectionEvent.OnAddToWishlist(
                            card = card,
                            quantity = quantity,
                            condition = condition,
                            printing = printing,
                            finish = finish
                        )
                    )
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) showAddCardModal = false
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) showAddCardModal = false
                    }
                }
            )
        }
    }

    if (showMoveToFolderSheet) {
        ModalBottomSheet(onDismissRequest = { showMoveToFolderSheet = false }) {
            FolderList(
                title = "Move to Folder",
                folders = uiState.folders,
                onFolderClick = { folderId ->
                    onEvent(CollectionEvent.OnMoveSelectedToFolder(folderId))
                    showMoveToFolderSheet = false
                }
            )
        }
    }

    if (showManageFolders) {
        ModalBottomSheet(onDismissRequest = { showManageFolders = false }) {
            ManageFoldersContent(
                folders = uiState.folders,
                onEditFolder = { folder ->
                    showFolderDialog = folder
                    showManageFolders = false
                },
                onDeleteFolder = { onEvent(CollectionEvent.OnDeleteFolder(it)) }
            )
        }
    }

    if (showFolderDialog != null) {
        FolderDialog(
            folder = showFolderDialog!!,
            onDismiss = { showFolderDialog = null },
            onConfirm = { name, icon, color ->
                if (showFolderDialog!!.id == 0L) {
                    onEvent(CollectionEvent.OnAddFolder(name, icon, color))
                } else {
                    onEvent(
                        CollectionEvent.OnUpdateFolder(
                            showFolderDialog!!.copy(
                                name = name,
                                icon = icon,
                                color = color
                            )
                        )
                    )
                }
                showFolderDialog = null
            }
        )
    }

    if (uiState.selectedDexId != null) {
        ModalBottomSheet(onDismissRequest = { onEvent(CollectionEvent.OnDismissDexDetail) }) {
            DexDetailContent(
                dexId = uiState.selectedDexId,
                cards = uiState.collectedCardsForDex,
                onCardClick = { userCardId ->
                    onNavigateToCardDetail(userCardId)
                    onEvent(CollectionEvent.OnDismissDexDetail)
                }
            )
        }
    }

    if (uiState.newSetsToDownload.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onEvent(CollectionEvent.OnDismissNewSetsPrompt) },
            title = { Text("New Sets Available") },
            text = {
                Text("${uiState.newSetsToDownload.size} new TCG sets have been released. Would you like to download them for offline use now?")
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CollectionEvent.OnDownloadNewSets) }) {
                    Text("Download Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(CollectionEvent.OnDismissNewSetsPrompt) }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun CollectionTopBar(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onShowViewSettings: () -> Unit,
    onShowManageFolders: () -> Unit,
    onShowMoveToFolder: () -> Unit
) {
    if (uiState.isSelectionMode) {
        val filteredIds = uiState.filteredUserCards.map { it.details.userCard.id }.toSet()
        SelectionTopBar(
            selectedCount = uiState.selectedIds.size,
            isAllSelected = filteredIds.isNotEmpty() && uiState.selectedIds.containsAll(filteredIds),
            onClearSelection = { onEvent(CollectionEvent.OnClearSelection) },
            onToggleSelectAll = {
                if (filteredIds.isNotEmpty() && uiState.selectedIds.containsAll(filteredIds)) {
                    onEvent(CollectionEvent.OnClearSelection)
                } else {
                    onEvent(CollectionEvent.OnSelectAll)
                }
            },
            onMoveToFolder = onShowMoveToFolder,
            onDeleteSelected = { onEvent(CollectionEvent.OnDeleteSelectedCards) }
        )
    } else {
        SearchTopBar(
            isSearchBarVisible = uiState.isSearchBarVisible,
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { onEvent(CollectionEvent.OnSearchQueryChange(it)) },
            onToggleSearchBar = { onEvent(CollectionEvent.OnToggleSearchBar) },
            onImportClick = onImportClick,
            onExportClick = onExportClick,
            onShowViewSettings = onShowViewSettings,
            onShowManageFolders = onShowManageFolders
        )
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount Selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = onToggleSelectAll) {
                Icon(
                    if (isAllSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                    contentDescription = if (isAllSelected) "Select None" else "Select All"
                )
            }
            IconButton(onClick = onMoveToFolder) {
                Icon(
                    Icons.AutoMirrored.Rounded.DriveFileMove,
                    contentDescription = "Move to Folder"
                )
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete Selected")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun SearchTopBar(
    isSearchBarVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearchBar: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onShowViewSettings: () -> Unit,
    onShowManageFolders: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchBarVisible) {
                SearchBar(searchQuery, onSearchQueryChange)
            } else {
                Text("Collection")
            }
        },
        navigationIcon = {
            if (isSearchBarVisible) {
                IconButton(onClick = onToggleSearchBar) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (!isSearchBarVisible) {
                IconButton(onClick = onToggleSearchBar) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
                MoreOptionsMenu(
                    onShowViewSettings = onShowViewSettings,
                    onShowManageFolders = onShowManageFolders,
                    onImportClick = onImportClick,
                    onExportClick = onExportClick
                )
            } else {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Clear Search")
                }
            }
        },
        windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)
    )
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Search your cards...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                innerTextField()
            }
        )
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun MoreOptionsMenu(
    onShowViewSettings: () -> Unit,
    onShowManageFolders: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            DropdownMenuItem(
                text = { Text("View Settings") },
                onClick = { showMenu = false; onShowViewSettings() },
                leadingIcon = { Icon(Icons.Rounded.Tune, null) }
            )
            DropdownMenuItem(
                text = { Text("Manage Folders") },
                onClick = { showMenu = false; onShowManageFolders() },
                leadingIcon = { Icon(Icons.Rounded.FolderCopy, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Import Collection") },
                onClick = { showMenu = false; onImportClick() },
                leadingIcon = { Icon(Icons.Rounded.FileDownload, null) }
            )
            DropdownMenuItem(
                text = { Text("Export Collection") },
                onClick = { showMenu = false; onExportClick() },
                leadingIcon = { Icon(Icons.Rounded.FileUpload, null) }
            )
        }
    }
}

@Composable
fun CollectionFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddFolder: () -> Unit,
    onAddManual: () -> Unit,
    onScan: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            label = "FabRotation"
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onAddFolder,
                    icon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                    text = { Text("Folder") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                )
                ExtendedFloatingActionButton(
                    onClick = onAddManual,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                )
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scan") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (expanded) "Close Menu" else "Open Menu",
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun AnimatedViewModeContent(
    viewMode: ViewMode,
    uiState: CollectionUiState,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit,
    onDexClick: (Int) -> Unit
) {
    AnimatedContent(
        targetState = viewMode,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200)))
                .togetherWith(
                    fadeOut(tween(200)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(200)
                    )
                )
                .using(SizeTransform(clip = false))
        },
        label = "ViewModeTransition"
    ) { targetViewMode ->
        when (targetViewMode) {
            ViewMode.LIST -> CollectionListView(
                userCards = uiState.filteredUserCards,
                selectedIds = uiState.selectedIds,
                isSelectionMode = uiState.isSelectionMode,
                settings = uiState.listSettings,
                preferSetLogo = uiState.preferSetLogo,
                onCardClick = onCardClick,
                onCardLongClick = onCardLongClick
            )

            ViewMode.GRID -> CollectionGridView(
                userCards = uiState.filteredUserCards,
                selectedIds = uiState.selectedIds,
                isSelectionMode = uiState.isSelectionMode,
                settings = uiState.gridSettings,
                onCardClick = onCardClick,
                onCardLongClick = onCardLongClick
            )

            ViewMode.POKEDEX -> PokedexView(
                entries = uiState.pokedexEntries,
                settings = uiState.pokedexSettings,
                onDexClick = onDexClick
            )
        }
    }
}

@Composable
fun FolderList(title: String, folders: List<FolderEntity>, onFolderClick: (Long) -> Unit) {
    Column(modifier = Modifier
        .padding(16.dp)
        .navigationBarsPadding()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        folders.forEach { folder ->
            ListItem(
                headlineContent = { Text(folder.name) },
                leadingContent = {
                    Icon(
                        getIconFromName(folder.icon),
                        contentDescription = null,
                        tint = folder.color?.let { Color(it.toLong().toInt()) }
                            ?: MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFolderClick(folder.id) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ManageFoldersContent(
    folders: List<FolderEntity>,
    onEditFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit
) {
    Column(modifier = Modifier
        .padding(16.dp)
        .navigationBarsPadding()) {
        Text("Manage Folders", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(folders, key = { it.id }) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = {
                        Icon(
                            getIconFromName(folder.icon),
                            contentDescription = null,
                            tint = folder.color?.let { Color(it.toLong().toInt()) }
                                ?: MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onEditFolder(folder) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDeleteFolder(folder) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DexDetailContent(dexId: Int, cards: List<CardUiModel>, onCardClick: (Long) -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .navigationBarsPadding()) {
        Text(
            "Pokédex #$dexId Collected Cards",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (cards.isEmpty()) {
            Text(
                "No cards collected for this Pokemon in the current filter.",
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cards, key = { it.details.userCard.id }) { item ->
                    Card(
                        onClick = { onCardClick(item.details.userCard.id) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = "${item.details.card.image}/low.webp",
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.718f),
                                contentScale = ContentScale.FillBounds
                            )
                            Box(modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)) {
                                CardAttributeBadges(
                                    finish = item.details.userCard.finish,
                                    printing = item.details.userCard.printing
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun getIconFromName(name: String?): ImageVector {
    return when (name) {
        "star" -> Icons.Rounded.Star
        "favorite" -> Icons.Rounded.Favorite
        "label" -> Icons.AutoMirrored.Rounded.Label
        "history" -> Icons.Rounded.History
        "cloud" -> Icons.Rounded.Cloud
        "auto_awesome" -> Icons.Rounded.AutoAwesome
        "bolt" -> Icons.Rounded.Bolt
        else -> Icons.Rounded.Folder
    }
}

private fun mockCardWithDetails(id: String, name: String) = CardUiModel(
    details = CardWithDetails(
    userCard = UserCardEntity(id = id.hashCode().toLong(), cardId = id, quantity = 1),
    card = CardEntity(id = id, localId = "1", name = name, image = "url", setId = "swsh1", rarity = "Common", category = "Pokemon", types = "Fire", dexId = "1"),
    set = SetEntity(id = "swsh1", name = "Sword & Shield", series = "Sword & Shield", logo = "url", symbol = "url", totalCards = 200, officialCards = 202, releaseDate = "2020-02-07")
    ),
    price = 123.00
)

@VaultioPreviews
@Composable
private fun CollectionContentPreview() {
    VaultioPreview {
        CollectionContent(
            uiState = CollectionUiState(
                userCards = kotlinx.collections.immutable.persistentListOf(
                    mockCardWithDetails("swsh1-1", "Pokemon Card 1").details
                ),
                filteredUserCards = kotlinx.collections.immutable.persistentListOf(
                    mockCardWithDetails("swsh1-1", "Pokemon Card 1")
                ),
                folders = kotlinx.collections.immutable.persistentListOf(
                    FolderEntity(
                        id = 1L,
                        name = "Favorites",
                        icon = "star"
                    )
                ),
                totalValue = 420.69,
                isLoading = false
            ),
            onEvent = {},
            onNavigateToScanner = {},
            onNavigateToCardDetail = {},
            importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {}
        )
    }
}

@VaultioPreviews
@Composable
private fun CollectionContentEmptyPreview() {
    VaultioPreview {
        CollectionContent(
            uiState = CollectionUiState(
                isLoading = false
            ),
            onEvent = {},
            onNavigateToScanner = {},
            onNavigateToCardDetail = {},
            importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {}
        )
    }
}
