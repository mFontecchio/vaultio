package com.mrhayami.vaultio.ui.card_detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.theme.*
import com.mrhayami.vaultio.ui.components.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    repository: VaultioRepository,
    userCardId: Long,
    onNavigateBack: () -> Unit,
) {
    val viewModel: CardDetailViewModel = viewModel(
        factory = CardDetailViewModelFactory(repository, SavedStateHandle(mapOf("userCardId" to userCardId)))
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var quantity by remember { mutableIntStateOf(0) }
    var condition by remember { mutableStateOf("") }
    var printing by remember { mutableStateOf("") }
    var finish by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }
    var is3dViewEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.cardWithDetails) {
        if (uiState.cardWithDetails != null && !isInitialized) {
            val userCard = uiState.cardWithDetails!!.userCard
            quantity = userCard.quantity
            condition = userCard.condition
            printing = userCard.printing
            finish = userCard.finish
            isInitialized = true
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            snackbarHostState.showSnackbar("Card updated successfully")
            viewModel.consumeSaveSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.cardWithDetails?.card?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveChanges(quantity, condition, printing, finish) }) {
                        Icon(Icons.Rounded.Done, contentDescription = "Save")
                    }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Card") },
                            text = { Text("Are you sure you want to remove this card from your collection?") },
                            shape = RoundedCornerShape(28.dp),
                            confirmButton = {
                                TextButton(onClick = { 
                                    showDeleteDialog = false
                                    viewModel.deleteUserCard() 
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.cardWithDetails != null) {
            val details = uiState.cardWithDetails!!
            val card = details.card
            val set = details.set
            val userCard = details.userCard

            val currentPrice = uiState.prices.find { 
                it.finish == userCard.finish && 
                it.condition == userCard.condition 
            } ?: uiState.prices.firstOrNull()

            val energyColor = remember(card.types) {
                val type = card.types?.let { 
                    if (it.startsWith("[")) {
                        it.substringAfter("\"").substringBefore("\"")
                    } else {
                        it.split(",").firstOrNull()?.trim()
                    }
                }
                when (type?.lowercase()) {
                    "grass" -> EnergyGrass
                    "fire" -> EnergyFire
                    "water" -> EnergyWater
                    "lightning", "electric" -> EnergyLightning
                    "psychic" -> EnergyPsychic
                    "fighting" -> EnergyFighting
                    "darkness", "dark" -> EnergyDarkness
                    "metal", "steel" -> EnergyMetal
                    "fairy" -> EnergyFairy
                    "dragon" -> EnergyDragon
                    else -> Color.Gray
                }
            }

            val isHolo = finish == "Holo" || finish == "Reverse Holo"
            val isGold = finish == "Gold"

            Box(modifier = Modifier.fillMaxSize()) {
                // Background Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    energyColor.copy(alpha = 0.4f),
                                    energyColor.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .sparkleEffect(show = isHolo)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Card Image Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (is3dViewEnabled) {
                            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                                ThreeDCard {
                                    AsyncImage(
                                        model = "${card.image}/high.webp",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .sparkleEffect(show = isHolo)
                                            .shimmerEffect(show = isGold),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                IconButton(
                                    onClick = { is3dViewEnabled = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Close, "Exit 3D View")
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(0.718f)
                                    .clickable { is3dViewEnabled = true },
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 16.dp,
                                color = Color.Transparent
                            ) {
                                AsyncImage(
                                    model = "${card.image}/high.webp",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .sparkleEffect(show = isHolo)
                                        .shimmerEffect(show = isGold),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    // Set and Rarity Row
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (set.symbol != null) {
                                AsyncImage(
                                    model = set.symbol,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(set.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "#${card.localId} / ${set.totalCards}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (card.rarity != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        card.rarity,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val priceText = currentPrice?.marketPrice?.let {
                                    NumberFormat.getCurrencyInstance(Locale.US).format(it)
                                } ?: "$0.00"
                                Text(priceText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.refreshPrice() },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Price", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("Estimated Market Value", style = MaterialTheme.typography.labelLarge)
                            if (currentPrice != null) {
                                Text(
                                    "Source: ${currentPrice.source.uppercase()} • Updated ${getRelativeTime(currentPrice.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Collection Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    Text(
                        "Added on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(userCard.dateAdded))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Quantity Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    IconButton(onClick = { if (quantity > 1) quantity-- }) { 
                                        Icon(Icons.Rounded.Remove, null, tint = MaterialTheme.colorScheme.primary) 
                                    }
                                    Text(
                                        quantity.toString(), 
                                        style = MaterialTheme.typography.titleLarge, 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    IconButton(onClick = { quantity++ }) { 
                                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary) 
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            DetailDropdown(
                                label = "Condition",
                                value = condition,
                                options = listOf("Near Mint", "Lightly Played", "Moderately Played", "Heavily Played", "Damaged"),
                                onSelected = { condition = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            DetailDropdown(
                                label = "Printing",
                                value = printing,
                                options = listOf("Standard", "First Edition", "Unlimited", "Promo"),
                                onSelected = { printing = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            DetailDropdown(
                                label = "Finish",
                                value = finish,
                                options = listOf("Non Holo", "Holo", "Reverse Holo", "Textured", "Gold"),
                                onSelected = { finish = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Folders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        uiState.folders.forEach { folder ->
                            val isInFolder = false // TODO: Real check via CrossRef
                            FilterChip(
                                selected = isInFolder,
                                onClick = { 
                                    if (isInFolder) viewModel.removeCardFromFolder(folder.id)
                                    else viewModel.addCardToFolder(folder.id)
                                },
                                label = { Text(folder.name) },
                                modifier = Modifier.padding(end = 8.dp),
                                shape = CircleShape
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        content = { content() }
    )
}

fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
