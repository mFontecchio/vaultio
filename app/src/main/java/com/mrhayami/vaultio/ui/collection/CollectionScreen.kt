@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.mrhayami.vaultio.ui.collection

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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
    val uiState by viewModel.uiState.collectAsState()
    var showAddCardModal by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf<FolderEntity?>(null) }
    var showManageFolders by remember { mutableStateOf(false) }
    var showViewSettings by remember { mutableStateOf(false) }
    var selectedDexId by remember { mutableStateOf<Int?>(null) }
    var showMoveToFolderSheet by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            snackbarHostState.showSnackbar("Card added to collection")
            viewModel.consumeSaveSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        val allSelected = uiState.selectedIds.size == uiState.userCards.size
                        IconButton(onClick = { if (allSelected) viewModel.clearSelection() else viewModel.selectAll() }) {
                            Icon(
                                if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                contentDescription = if (allSelected) "Select None" else "Select All"
                            )
                        }
                        IconButton(onClick = { showMoveToFolderSheet = true }) {
                            Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = "Move to Folder")
                        }
                        IconButton(onClick = viewModel::deleteSelectedCards) {
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
                                    onValueChange = viewModel::setSearchQuery,
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
                            IconButton(onClick = viewModel::toggleSearchBar) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!uiState.isSearchBarVisible) {
                            IconButton(onClick = viewModel::toggleSearchBar) {
                                Icon(Icons.Rounded.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { showManageFolders = true }) {
                                Icon(Icons.Rounded.FolderCopy, contentDescription = "Manage Folders")
                            }
                            IconButton(onClick = { showViewSettings = true }) {
                                Icon(Icons.Rounded.Tune, contentDescription = "View Settings")
                            }
                        } else {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
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
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { showFolderDialog = FolderEntity(name = "") },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Add Folder")
                    }
                    SmallFloatingActionButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan Card")
                    }
                    FloatingActionButton(
                        onClick = { showAddCardModal = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Card")
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
            StickyControls(
                uiState = uiState,
                onViewModeChange = viewModel::setViewMode,
                onFolderSelect = viewModel::selectFolder
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.filteredUserCards.isEmpty() && uiState.viewMode != ViewMode.POKEDEX) {
                    Text(
                        if (uiState.searchQuery.isNotEmpty()) "No cards match your search." 
                        else "Your collection is empty.\nTap + to add cards!",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    when (uiState.viewMode) {
                        ViewMode.LIST -> ListView(
                            userCards = uiState.filteredUserCards,
                            selectedIds = uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            settings = uiState.listSettings,
                            onCardClick = { if (uiState.isSelectionMode) viewModel.toggleSelection(it) else onNavigateToCardDetail(it) },
                            onCardLongClick = viewModel::toggleSelection
                        )
                        ViewMode.GRID -> GridView(
                            userCards = uiState.filteredUserCards,
                            selectedIds = uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            settings = uiState.gridSettings,
                            onCardClick = { if (uiState.isSelectionMode) viewModel.toggleSelection(it) else onNavigateToCardDetail(it) },
                            onCardLongClick = viewModel::toggleSelection
                        )
                        ViewMode.POKEDEX -> PokedexView(
                            entries = uiState.pokedexEntries,
                            settings = uiState.pokedexSettings,
                            onDexClick = { dexId -> selectedDexId = dexId }
                        )
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
                onUpdateList = viewModel::updateListSettings,
                onUpdateGrid = viewModel::updateGridSettings,
                onUpdatePokedex = viewModel::updatePokedexSettings
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
                viewModel = viewModel,
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
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Move to Folder", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                uiState.folders.forEach { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = {
                            Icon(
                                getIconFromName(folder.icon),
                                contentDescription = null,
                                tint = folder.color?.let { Color(it.toLong().toInt()) } ?: MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.moveSelectedToFolder(folder.id)
                                showMoveToFolderSheet = false
                            }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showManageFolders) {
        ModalBottomSheet(onDismissRequest = { showManageFolders = false }) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Manage Folders", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(uiState.folders) { folder ->
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
                                    IconButton(onClick = { viewModel.deleteFolder(folder) }) {
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
                    viewModel.addFolder(name, icon, color)
                } else {
                    viewModel.updateFolder(showFolderDialog!!.copy(name = name, icon = icon, color = color))
                }
                showFolderDialog = null
            }
        )
    }

    if (selectedDexId != null) {
        val moshi = remember { Moshi.Builder().build() }
        val listIntAdapter = remember { moshi.adapter<List<Int>>(Types.newParameterizedType(List::class.java, Integer::class.java)) }
        
        val collectedForDex = uiState.userCards.filter { cardWithDetails ->
            val card = cardWithDetails.card
            val dexIds = try {
                card.dexIds?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(card.dexId?.toIntOrNull())
            } catch (e: Exception) {
                listOfNotNull(card.dexId?.toIntOrNull())
            }
            dexIds.contains(selectedDexId)
        }
        
        ModalBottomSheet(onDismissRequest = { selectedDexId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Pokédex #$selectedDexId Collected Cards",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (collectedForDex.isEmpty()) {
                    Text("No cards collected for this Pokemon yet.", modifier = Modifier.padding(vertical = 32.dp))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(collectedForDex) { item ->
                            Card(
                                onClick = {
                                    onNavigateToCardDetail(item.userCard.id)
                                    selectedDexId = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                AsyncImage(
                                    model = "${item.card.image}/high.webp",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.718f),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ViewSettingsSheet(
    viewMode: ViewMode,
    listSettings: ListSettings,
    gridSettings: GridSettings,
    pokedexSettings: PokedexSettings,
    onUpdateList: (ListSettings) -> Unit,
    onUpdateGrid: (GridSettings) -> Unit,
    onUpdatePokedex: (PokedexSettings) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
        Text("View Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when (viewMode) {
            ViewMode.LIST -> {
                SettingsToggle("Show Prices", listSettings.showPrices) { onUpdateList(listSettings.copy(showPrices = it)) }
                SettingsToggle("Compact Mode", listSettings.isCompact) { onUpdateList(listSettings.copy(isCompact = it)) }
            }
            ViewMode.GRID -> {
                Text("Columns: ${gridSettings.columns}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = gridSettings.columns.toFloat(),
                    onValueChange = { onUpdateGrid(gridSettings.copy(columns = it.toInt())) },
                    valueRange = 2f..5f,
                    steps = 2
                )
                SettingsToggle("Show Quantity Badges", gridSettings.showBadges) { onUpdateGrid(gridSettings.copy(showBadges = it)) }
            }
            ViewMode.POKEDEX -> {
                SettingsToggle("Show Uncollected Slots", pokedexSettings.showUncollected) { onUpdatePokedex(pokedexSettings.copy(showUncollected = it)) }
                SettingsToggle("Use Shiny Sprites", pokedexSettings.useShinySprites) { onUpdatePokedex(pokedexSettings.copy(useShinySprites = it)) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun FolderDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedIcon by remember { mutableStateOf(folder.icon ?: "folder") }

    val colorOptions = listOf(
        Color(0xFF78C850), // Grass
        Color(0xFFF08030), // Fire
        Color(0xFF6890F0), // Water
        Color(0xFFF8D030), // Electric
        Color(0xFFF85888), // Psychic
        Color(0xFFA8A878), // Normal
        Color(0xFFE0C068), // Ground
        Color(0xFFA040A0), // Poison
        Color(0xFFC03028), // Fighting
        Color(0xFFB8A038), // Rock
        Color(0xFFA8B820), // Bug
        Color(0xFF705898), // Ghost
        Color(0xFFB8B8D0), // Steel
        Color(0xFF98D8D8), // Ice
        Color(0xFF7038F8), // Dragon
        Color(0xFF705848), // Dark
        Color(0xFFEE99AC)  // Fairy
    )

    var selectedColor by remember {
        mutableStateOf(folder.color ?: colorOptions[1].toArgb().toLong().toString())
    }

    val icons = listOf("folder", "star", "favorite", "label", "history", "cloud", "auto_awesome", "bolt")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder.id == 0L) "New Folder" else "Edit Folder") },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icons) { iconName ->
                        IconButton(
                            onClick = { selectedIcon = iconName },
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(getIconFromName(iconName), contentDescription = null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Theme Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(colorOptions) { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColor == color.toArgb().toLong().toString()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color.toArgb().toLong().toString() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, selectedIcon, selectedColor) }) {
                Text(if (folder.id == 0L) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

@Composable
fun StickyControls(
    uiState: CollectionUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onFolderSelect: (Long?) -> Unit
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedFolderId == null,
                    onClick = { onFolderSelect(null) },
                    label = { Text("All") },
                    shape = CircleShape
                )
            }
            items(uiState.folders) { folder ->
                FilterChip(
                    selected = uiState.selectedFolderId == folder.id,
                    onClick = { onFolderSelect(folder.id) },
                    label = { Text(folder.name) },
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            getIconFromName(folder.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = folder.color?.let { Color(it.toLong().toInt()) } ?: MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${uiState.filteredUserCards.size} Cards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(end = 16.dp),
            ) {
                SegmentedButton(
                    selected = uiState.viewMode == ViewMode.LIST,
                    onClick = { onViewModeChange(ViewMode.LIST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "List") }
                SegmentedButton(
                    selected = uiState.viewMode == ViewMode.GRID,
                    onClick = { onViewModeChange(ViewMode.GRID) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Icon(Icons.Rounded.GridView, contentDescription = "Grid") }
                SegmentedButton(
                    selected = uiState.viewMode == ViewMode.POKEDEX,
                    onClick = { onViewModeChange(ViewMode.POKEDEX) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Pokedex") }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ListView(
    userCards: List<CardWithDetails>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    settings: ListSettings,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.navigationBarsPadding()
    ) {
        items(userCards) { item ->
            val isSelected = selectedIds.contains(item.userCard.id)
            ListItem(
                headlineContent = { Text(item.card.name, fontWeight = FontWeight.Bold) },
                supportingContent = {
                    if (!settings.isCompact) {
                        Text("${item.set.name} • ${item.card.localId}")
                    }
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(if (settings.isCompact) 48.dp else 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = "${item.card.image}/low.webp",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter
                        )
                    }
                },
                trailingContent = {
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onCardClick(item.userCard.id) })
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("x${item.userCard.quantity}", fontWeight = FontWeight.Bold)
                            if (settings.showPrices) {
                                Text("$0.00", style = MaterialTheme.typography.bodySmall) // Price placeholder
                            }
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { onCardClick(item.userCard.id) },
                        onLongClick = { onCardLongClick(item.userCard.id) }
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
fun GridView(
    userCards: List<CardWithDetails>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    settings: GridSettings,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.columns),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.navigationBarsPadding()
    ) {
        items(userCards) { item ->
            val isSelected = selectedIds.contains(item.userCard.id)
            Card(
                modifier = Modifier.combinedClickable(
                    onClick = { onCardClick(item.userCard.id) },
                    onLongClick = { onCardLongClick(item.userCard.id) }
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box {
                    AsyncImage(
                        model = "${item.card.image}/high.webp",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.718f),
                        contentScale = ContentScale.FillBounds
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        }
                    } else if (settings.showBadges) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text("${item.userCard.quantity}", modifier = Modifier.padding(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PokedexView(
    entries: List<PokedexEntry>,
    settings: PokedexSettings,
    onDexClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.navigationBarsPadding()
    ) {
        items(entries) { entry ->
            val isCollected = entry.isCollected
            val spriteType = if (settings.useShinySprites) "shiny" else "pokemon"
            val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/$spriteType/${entry.dexNumber}.png"

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(
                        if (isCollected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onDexClick(entry.dexNumber) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("#${entry.dexNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = spriteUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit,
                            alpha = if (isCollected) 1f else 0.3f,
                            colorFilter = if (isCollected) null else androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                            )
                        )
                    }
                    if (isCollected && entry.pokemonName != null) {
                        Text(
                            entry.pokemonName,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddCardModal(viewModel: CollectionViewModel, onDismiss: () -> Unit) {
    val searchState by viewModel.remoteSearchState.collectAsState()
    val collectionUiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCard by remember { mutableStateOf<TcgDexCard?>(null) }

    if (selectedCard == null) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .imePadding()
        ) {
            val focusRequester = remember { FocusRequester() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length > 2) viewModel.searchRemoteCards(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Search, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search Pokemon Cards",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                innerTextField()
                            }
                            if (searchState.second) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(searchState.first) { card ->
                    ListItem(
                        headlineContent = { Text(card.name, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            val setId = card.id.substringBefore("-")
                            val set = collectionUiState.sets[setId]
                            val setName = set?.name ?: setId
                            val officialCount = set?.officialCards ?: 0
                            val cardNumberText = if (officialCount > 0) "${card.localId}/$officialCount" else card.localId
                            val category = card.category ?: ""
                            Text("$setName • $cardNumberText" + if (category.isNotEmpty()) " • $category" else "")
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = "${card.image}/low.webp",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCard = card }
                    )
                }
            }
        }
    } else {
        MetadataModal(
            card = selectedCard!!,
            onConfirm = { q, c, p, f ->
                viewModel.addUserCard(selectedCard!!, q, c, p, f)
                onDismiss()
            },
            onBack = { selectedCard = null }
        )
    }
}
