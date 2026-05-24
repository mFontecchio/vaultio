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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.tooling.preview.Preview
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
import kotlinx.coroutines.launch

@Composable
fun CollectionScreen(
    repository: VaultioRepository,
    userPreferencesRepository: UserPreferencesRepository,
    onNavigateToScanner: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit,
    onNavigateToWishlist: () -> Unit,
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
        onNavigateToWishlist = onNavigateToWishlist,
        importLauncher = importLauncher
    )
}

@Composable
fun CollectionContent(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCardDetail: (Long) -> Unit,
    onNavigateToWishlist: () -> Unit,
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
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(CollectionEvent.OnClearSelection) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        val allSelected = uiState.selectedIds.size == uiState.userCards.size
                        IconButton(onClick = { if (allSelected) onEvent(CollectionEvent.OnClearSelection) else onEvent(CollectionEvent.OnSelectAll) }) {
                            Icon(
                                if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                contentDescription = if (allSelected) "Select None" else "Select All"
                            )
                        }
                        IconButton(onClick = { showMoveToFolderSheet = true }) {
                            Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = "Move to Folder")
                        }
                        IconButton(onClick = { onEvent(CollectionEvent.OnDeleteSelectedCards) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        if (uiState.isSearchBarVisible) {
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
                                    value = uiState.searchQuery,
                                    onValueChange = { onEvent(CollectionEvent.OnSearchQueryChange(it)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    decorationBox = { innerTextField ->
                                        if (uiState.searchQuery.isEmpty()) {
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
                        } else {
                            Text("Collection")
                        }
                    },
                    navigationIcon = {
                        if (uiState.isSearchBarVisible) {
                            IconButton(onClick = { onEvent(CollectionEvent.OnToggleSearchBar) }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!uiState.isSearchBarVisible) {
                            IconButton(onClick = { onEvent(CollectionEvent.OnToggleSearchBar) }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Search")
                            }

                            IconButton(onClick = onNavigateToWishlist) {
                                Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Wishlist")
                            }

                            var showMenu by remember { mutableStateOf(false) }
                            var showExportDialog by remember { mutableStateOf(false) }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    shape = RoundedCornerShape(28.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 6.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("View Settings", style = MaterialTheme.typography.labelLarge) },
                                        onClick = {
                                            showMenu = false
                                            showViewSettings = true
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(20.dp)) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Manage Folders", style = MaterialTheme.typography.labelLarge) },
                                        onClick = {
                                            showMenu = false
                                            showManageFolders = true
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.FolderCopy, null, modifier = Modifier.size(20.dp)) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
                                    DropdownMenuItem(
                                        text = { Text("Import Collection", style = MaterialTheme.typography.labelLarge) },
                                        onClick = {
                                            showMenu = false
                                            importLauncher.launch(arrayOf("application/json"))
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(20.dp)) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export Collection", style = MaterialTheme.typography.labelLarge) },
                                        onClick = {
                                            showMenu = false
                                            showExportDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(20.dp)) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }

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
                        } else {
                            IconButton(onClick = { onEvent(CollectionEvent.OnSearchQueryChange("")) }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (fabExpanded) 45f else 0f,
                        label = "FabRotation"
                    )

                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    showFolderDialog = FolderEntity(name = "")
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Add Folder")
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    showAddCardModal = true
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add Card Manually")
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    onNavigateToScanner()
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan Card")
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = if (fabExpanded) "Close Menu" else "Open Menu",
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
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
                    Text(
                        if (uiState.searchQuery.isNotEmpty()) "No cards match your search." 
                        else if (uiState.selectedFolderId != null) "This folder is empty."
                        else "Your collection is empty.\nTap + to add cards!",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    AnimatedContent(
                        targetState = uiState.viewMode,
                        transitionSpec = {
                            (fadeIn(tween(200)) + scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(200)
                            ))
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
            }
        }
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
            Column(modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()) {
                Text("Move to Folder", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                uiState.folders.forEach { folder ->
                    val clickableModifier = remember(folder.id) {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onEvent(CollectionEvent.OnMoveSelectedToFolder(folder.id))
                                showMoveToFolderSheet = false
                            }
                    }
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = {
                            Icon(
                                getIconFromName(folder.icon),
                                contentDescription = null,
                                tint = folder.color?.let { Color(it.toLong().toInt()) } ?: MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = clickableModifier
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showManageFolders) {
        ModalBottomSheet(onDismissRequest = { showManageFolders = false }) {
            Column(modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()) {
                Text("Manage Folders", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(uiState.folders, key = { it.id }) { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            leadingContent = {
                                Icon(
                                    getIconFromName(folder.icon),
                                    contentDescription = null,
                                    tint = folder.color?.let { Color(it.toLong().toInt()) } ?: MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        showFolderDialog = folder
                                        showManageFolders = false
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onEvent(CollectionEvent.OnDeleteFolder(folder)) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                    onEvent(CollectionEvent.OnUpdateFolder(showFolderDialog!!.copy(name = name, icon = icon, color = color)))
                }
                showFolderDialog = null
            }
        )
    }

    if (uiState.selectedDexId != null) {
        ModalBottomSheet(onDismissRequest = { onEvent(CollectionEvent.OnDismissDexDetail) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Pokédex #${uiState.selectedDexId} Collected Cards",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (uiState.collectedCardsForDex.isEmpty()) {
                    Text("No cards collected for this Pokemon in the current filter.", modifier = Modifier.padding(vertical = 32.dp))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.collectedCardsForDex) { item ->
                            Card(
                                onClick = {
                                    onNavigateToCardDetail(item.details.userCard.id)
                                    onEvent(CollectionEvent.OnDismissDexDetail)
                                },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = "${item.details.card.image}/high.webp",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.718f),
                                        contentScale = ContentScale.FillBounds
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                    ) {
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

@Preview(showBackground = true, name = "Collection Content Populated")
@Composable
private fun CollectionContentPreview() {
    MaterialTheme {
        CollectionContent(
            uiState = CollectionUiState(
                userCards = List(5) {
                    mockCardWithDetails(
                        "swsh1-$it",
                        "Pokemon Card $it"
                    ).details
                },
                filteredUserCards = List(5) { mockCardWithDetails("swsh1-$it", "Pokemon Card $it") },
                folders = listOf(FolderEntity(id = 1L, name = "Favorites", icon = "star")),
                totalValue = 420.69,
                isLoading = false
            ),
            onEvent = {},
            onNavigateToScanner = {},
            onNavigateToCardDetail = {},
            onNavigateToWishlist = {},
            importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {}
        )
    }
}

@Preview(showBackground = true, name = "Collection Content Empty")
@Composable
private fun CollectionContentEmptyPreview() {
    MaterialTheme {
        CollectionContent(
            uiState = CollectionUiState(
                userCards = emptyList(),
                filteredUserCards = emptyList(),
                isLoading = false
            ),
            onNavigateToScanner = {},
            onNavigateToCardDetail = {},
            onNavigateToWishlist = {},
            importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {},
            onEvent = {},
            modifier = Modifier
        )
    }
}
