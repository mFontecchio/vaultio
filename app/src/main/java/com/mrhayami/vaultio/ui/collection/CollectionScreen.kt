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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import androidx.compose.material.icons.rounded.GridView
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.collection.components.CollectionGridView
import com.mrhayami.vaultio.ui.collection.components.CollectionListView
import com.mrhayami.vaultio.ui.collection.components.PokedexView
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch
import java.util.Locale

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
    val allPrices by repository.allPrices.collectAsStateWithLifecycle(initialValue = emptyList())
    val allVintagePrices by repository.allVintagePrices.collectAsStateWithLifecycle(initialValue = emptyList())

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
                val json = stream.bufferedReader().use { it.readText() }
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
        allPrices = allPrices,
        allVintagePrices = allVintagePrices,
        onEvent = viewModel::onEvent,
        onNavigateToScanner = onNavigateToScanner,
        onNavigateToCardDetail = onNavigateToCardDetail,
        importLauncher = importLauncher
    )
}

@Composable
fun CollectionContent(
    uiState: CollectionUiState,
    allPrices: List<PriceEntity>,
    allVintagePrices: List<VintagePriceEntity>,
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
    var selectedDexId by remember { mutableStateOf<Int?>(null) }
    var showMoveToFolderSheet by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    val onCardClick = remember(uiState.isSelectionMode) {
        { id: Long -> if (uiState.isSelectionMode) onEvent(CollectionEvent.OnToggleSelection(id)) else onNavigateToCardDetail(id) }
    }
    val onCardLongClick = remember {
        { id: Long -> onEvent(CollectionEvent.OnToggleSelection(id)) }
    }
    val onDexClick = remember {
        { dexId: Int -> selectedDexId = dexId }
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
                            modifier = Modifier.rotate(rotation)
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
            StickyControls(
                uiState = uiState,
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
                            (fadeIn() + scaleIn(initialScale = 0.92f))
                                .togetherWith(fadeOut() + scaleOut(targetScale = 0.92f))
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
                                allPrices = allPrices,
                                allVintagePrices = allVintagePrices,
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
                uiState = uiState,
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
                uiState = uiState,
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
                                onEvent(CollectionEvent.OnMoveSelectedToFolder(folder.id))
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

    if (selectedDexId != null) {
        val moshi = remember { Moshi.Builder().build() }
        val listIntAdapter = remember { moshi.adapter<List<Int>>(Types.newParameterizedType(List::class.java, Int::class.javaObjectType)) }
        
        // Use filtered cards so folder filtering applies to the detailed list too
        val collectedForDex = remember(selectedDexId, uiState.filteredUserCards) {
            uiState.filteredUserCards.filter { cardWithDetails ->
                val card = cardWithDetails.card
                val dexIds = try {
                    card.dexIds?.let { listIntAdapter.fromJson(it) } ?: listOfNotNull(card.dexId?.toIntOrNull())
                } catch (_: Exception) {
                    listOfNotNull(card.dexId?.toIntOrNull())
                }
                dexIds.contains(selectedDexId)
            }
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
                    Text("No cards collected for this Pokemon in the current filter.", modifier = Modifier.padding(vertical = 32.dp))
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
                                Box {
                                    AsyncImage(
                                        model = "${item.card.image}/high.webp",
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
                                            finish = item.userCard.finish,
                                            printing = item.userCard.printing
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
}


@Composable
fun SortFilterSheet(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort & Filter", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { onEvent(CollectionEvent.OnClearFilters) }) { Text("Clear All") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Sort By", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.sortMode == mode,
                    onClick = { onEvent(CollectionEvent.OnSortModeChange(mode)) },
                    label = { Text(mode.name.replace("_", " ").lowercase(Locale.US).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Direction", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(16.dp))
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = uiState.sortDirection == SortDirection.ASCENDING,
                    onClick = { onEvent(CollectionEvent.OnSortDirectionChange(SortDirection.ASCENDING)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Asc") }
                SegmentedButton(
                    selected = uiState.sortDirection == SortDirection.DESCENDING,
                    onClick = { onEvent(CollectionEvent.OnSortDirectionChange(SortDirection.DESCENDING)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Desc") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (uiState.availableRarities.isNotEmpty()) {
            Text("Rarity", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableRarities.forEach { rarity ->
                    FilterChip(
                        selected = uiState.filterSettings.rarities.contains(rarity),
                        onClick = { onEvent(CollectionEvent.OnToggleRarityFilter(rarity)) },
                        label = { Text(rarity) },
                    )
                }
            }
        }

        if (uiState.availableCategories.isNotEmpty()) {
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableCategories.forEach { category ->
                    FilterChip(
                        selected = uiState.filterSettings.categories.contains(category),
                        onClick = { onEvent(CollectionEvent.OnToggleCategoryFilter(category)) },
                        label = { Text(category) },
                    )
                }
            }
        }

        if (uiState.availableTypes.isNotEmpty()) {
            Text("Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableTypes.forEach { type ->
                    FilterChip(
                        selected = uiState.filterSettings.types.contains(type),
                        onClick = { onEvent(CollectionEvent.OnToggleTypeFilter(type)) },
                        label = { Text(type) },
                    )
                }
            }
        }

        Text("Condition", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Mint", "Near Mint", "Lightly Played", "Moderately Played", "Heavily Played", "Damaged").forEach { cond ->
                FilterChip(
                    selected = uiState.filterSettings.conditions.contains(cond),
                    onClick = { onEvent(CollectionEvent.OnToggleConditionFilter(cond)) },
                    label = { Text(cond) },
                )
            }
        }

        Text("Finish", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Non Holo", "Holo", "Reverse Holo", "Textured", "Gold").forEach { finish ->
                FilterChip(
                    selected = uiState.filterSettings.finishes.contains(finish),
                    onClick = { onEvent(CollectionEvent.OnToggleFinishFilter(finish)) },
                    label = { Text(finish) },
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ViewSettingsSheet(
    viewMode: ViewMode,
    listSettings: ListSettings,
    gridSettings: GridSettings,
    pokedexSettings: PokedexSettings,
    onEvent: (CollectionEvent) -> Unit
) {
    Column(modifier = Modifier
        .padding(16.dp)
        .navigationBarsPadding()) {
        Text("View Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when (viewMode) {
            ViewMode.LIST -> {
                SettingsToggle("Show Prices", listSettings.showPrices) { onEvent(CollectionEvent.OnUpdateListSettings(listSettings.copy(showPrices = it))) }
                SettingsToggle("Compact Mode", listSettings.isCompact) { onEvent(CollectionEvent.OnUpdateListSettings(listSettings.copy(isCompact = it))) }
            }
            ViewMode.GRID -> {
                Text("Columns: ${gridSettings.columns}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = gridSettings.columns.toFloat(),
                    onValueChange = { onEvent(CollectionEvent.OnUpdateGridSettings(gridSettings.copy(columns = it.toInt()))) },
                    valueRange = 2f..5f,
                    steps = 2
                )
                SettingsToggle("Show Quantity Badges", gridSettings.showBadges) { onEvent(CollectionEvent.OnUpdateGridSettings(gridSettings.copy(showBadges = it))) }
            }
            ViewMode.POKEDEX -> {
                SettingsToggle("Show Uncollected Slots", pokedexSettings.showUncollected) { onEvent(CollectionEvent.OnUpdatePokedexSettings(pokedexSettings.copy(showUncollected = it))) }
                SettingsToggle("Use Official Art", pokedexSettings.useOfficialArt) { onEvent(CollectionEvent.OnUpdatePokedexSettings(pokedexSettings.copy(useOfficialArt = it))) }
                SettingsToggle("Use Shiny Sprites", pokedexSettings.useShinySprites) { onEvent(CollectionEvent.OnUpdatePokedexSettings(pokedexSettings.copy(useShinySprites = it))) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                                    color = if (selectedColor == color.toArgb().toLong()
                                            .toString()
                                    ) MaterialTheme.colorScheme.primary else Color.Transparent,
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

@Composable
fun ExportSelectionDialog(
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>?) -> Unit
) {
    var selectedFolderIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var exportAll by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Collection") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select what you want to export:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAll = true }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = exportAll,
                        onClick = { exportAll = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Entire Collection")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAll = false }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = !exportAll,
                        onClick = { exportAll = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Selected Folders Only")
                }

                AnimatedVisibility(visible = !exportAll) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(start = 32.dp)
                    ) {
                        LazyColumn {
                            items(folders) { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFolderIds =
                                                if (selectedFolderIds.contains(folder.id)) {
                                                    selectedFolderIds - folder.id
                                                } else {
                                                    selectedFolderIds + folder.id
                                                }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedFolderIds.contains(folder.id),
                                        onCheckedChange = { checked ->
                                            selectedFolderIds = if (checked) {
                                                selectedFolderIds + folder.id
                                            } else {
                                                selectedFolderIds - folder.id
                                            }
                                        }
                                    )
                                    Icon(
                                        getIconFromName(folder.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = folder.color?.let { Color(it.toLong().toInt()) } ?: MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (exportAll) {
                        onConfirm(null)
                    } else if (selectedFolderIds.isNotEmpty()) {
                        onConfirm(selectedFolderIds.toList())
                    }
                },
                enabled = exportAll || selectedFolderIds.isNotEmpty()
            ) {
                Text("Export")
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

@Preview(showBackground = true)
@Composable
private fun StickyControlsPreview() {
    MaterialTheme {
        StickyControls(
            uiState = CollectionUiState(
                totalValue = 1250.50,
                filteredUserCards = List(5) { mockCardWithDetails("swsh1-$it", "Card $it") }
            ),
            onEvent = {},
            onSortClick = {}
        )
    }
}







private fun mockCardWithDetails(id: String, name: String) = CardWithDetails(
    userCard = UserCardEntity(id = id.hashCode().toLong(), cardId = id, quantity = 1),
    card = CardEntity(id = id, localId = "1", name = name, image = "url", setId = "swsh1", rarity = "Common", category = "Pokemon", types = "Fire", dexId = "1"),
    set = SetEntity(id = "swsh1", name = "Sword & Shield", series = "Sword & Shield", logo = "url", symbol = "url", totalCards = 200, officialCards = 202, releaseDate = "2020-02-07")
)

@Preview(showBackground = true, name = "Collection Content Populated")
@Composable
private fun CollectionContentPreview() {
    MaterialTheme {
        CollectionContent(
            uiState = CollectionUiState(
                userCards = List(5) { mockCardWithDetails("swsh1-$it", "Pokemon Card $it") },
                filteredUserCards = List(5) { mockCardWithDetails("swsh1-$it", "Pokemon Card $it") },
                folders = listOf(FolderEntity(id = 1L, name = "Favorites", icon = "star")),
                totalValue = 420.69,
                isLoading = false
            ),
            allPrices = emptyList(),
            allVintagePrices = emptyList(),
            onEvent = {},
            onNavigateToScanner = {},
            onNavigateToCardDetail = {},
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
            allPrices = emptyList(),
            allVintagePrices = emptyList(),
            onNavigateToScanner = {},
            onNavigateToCardDetail = {},
            importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {},
            onEvent = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true, name = "Add Card Modal")
@Composable
private fun AddCardModalPreview() {
    VaultioTheme {
        AddCardModal(
            uiState = CollectionUiState(
                searchResults = listOf(
                    TcgDexCard(
                        id = "swsh1-1",
                        localId = "1",
                        name = "Bulbasaur",
                        image = "https://images.tcgdex.net/en/swsh/swsh1/1",
                        rarity = "Common",
                        category = "Pokemon"
                    ),
                    TcgDexCard(
                        id = "swsh1-2",
                        localId = "2",
                        name = "Ivysaur",
                        image = "https://images.tcgdex.net/en/swsh/swsh1/2",
                        rarity = "Uncommon",
                        category = "Pokemon"
                    )
                ),
                isSearching = false,
                folders = listOf(
                    FolderEntity(id = 1L, name = "Favorites", icon = "star", color = "0xFF78C850")
                )
            ),
            onEvent = {},
            onDismiss = {}
        )
    }
}

@Composable
fun StickyControls(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = uiState.filterSettings != FilterSettings() || uiState.sortMode != SortMode.DATE_ADDED,
                    onClick = onSortClick,
                    label = { Text("Sort & Filter") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null, Modifier.size(18.dp)) },
                    shape = CircleShape
                )
            }
            
            item {
                VerticalDivider(modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp))
            }

            item {
                FilterChip(
                    selected = uiState.selectedFolderId == null,
                    onClick = { onEvent(CollectionEvent.OnFolderSelect(null)) },
                    label = { Text("All") },
                    shape = CircleShape
                )
            }
            items(uiState.folders, key = { it.id }) { folder ->
                FilterChip(
                    selected = uiState.selectedFolderId == folder.id,
                    onClick = { onEvent(CollectionEvent.OnFolderSelect(folder.id)) },
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
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    if (uiState.viewMode == ViewMode.POKEDEX) "${uiState.pokedexEntries.count { it.isCollected }} / ${uiState.pokedexEntries.size} Collected"
                    else "${uiState.totalQuantity} Cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.viewMode != ViewMode.POKEDEX) {
                    Text(
                        "$${String.format(Locale.US, "%.2f", uiState.totalValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(end = 16.dp),
            ) {
                SegmentedButton(
                    selected = uiState.viewMode == ViewMode.LIST,
                    onClick = { onEvent(CollectionEvent.OnViewModeChange(ViewMode.LIST)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "List") }
                SegmentedButton(
                    selected = uiState.viewMode == ViewMode.GRID,
                    onClick = { onEvent(CollectionEvent.OnViewModeChange(ViewMode.GRID)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Icon(Icons.Rounded.GridView, contentDescription = "Grid") }
                SegmentedButton(
                    selected = uiState.viewMode == ViewMode.POKEDEX,
                    onClick = { onEvent(CollectionEvent.OnViewModeChange(ViewMode.POKEDEX)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Pokedex") }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCardModal(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    onDismiss: () -> Unit
) {
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
                        if (it.length > 2) onEvent(CollectionEvent.OnSearchRemoteCards(it))
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
                            if (uiState.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.searchResults, key = { it.id }) { card ->
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(card.name, fontWeight = FontWeight.Bold)
                                if (card.rarity?.contains("Promo", ignoreCase = true) == true) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CardAttributeBadges(
                                        finish = PricingUtils.FINISH_NORMAL,
                                        printing = PricingUtils.PRINTING_PROMO
                                    )
                                }
                            }
                        },
                        supportingContent = {
                            val setId = card.id.substringBefore("-")
                            val set = uiState.sets[setId]
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
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter
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
            folders = uiState.folders,
            onConfirm = { q, c, p, f, folderIds ->
                onEvent(CollectionEvent.OnAddUserCard(selectedCard!!, q, c, p, f, folderIds))
                onDismiss()
            },
            onBack = { selectedCard = null }
        )
    }
}
