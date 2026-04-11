package com.mrhayami.vaultio.ui.card_detail

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.VintageSets
import androidx.compose.ui.tooling.preview.Preview
import com.mrhayami.vaultio.data.local.*
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.theme.*
import com.mrhayami.vaultio.ui.components.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailContent(
    uiState: CardDetailUiState,
    onNavigateBack: () -> Unit,
    onEvent: (CardDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var quantity by remember { mutableIntStateOf(0) }
    var condition by remember { mutableStateOf("") }
    var printing by remember { mutableStateOf("") }
    var finish by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.cardWithDetails) {
        if (uiState.cardWithDetails != null && !isInitialized) {
            val userCard = uiState.cardWithDetails.userCard
            quantity = userCard.quantity
            condition = userCard.condition
            printing = userCard.printing
            finish = userCard.finish
            isInitialized = true
        }
    }

    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            Toast.makeText(context, "Card updated successfully", Toast.LENGTH_SHORT).show()
            onEvent(CardDetailEvent.ConsumeSaveSuccess)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.cardWithDetails?.card?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        onEvent(CardDetailEvent.SaveChanges(quantity, condition, printing, finish)) 
                    }) {
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
                                    onEvent(CardDetailEvent.DeleteCard) 
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
            val details = uiState.cardWithDetails
            val card = details.card
            val set = details.set
            val userCard = details.userCard

            val isVintage = VintageSets.isVintageSet(card.setId)
            
            val isFullArt = card.rarity?.contains("Illustrated Rare", ignoreCase = true) == true || 
                            card.rarity?.contains("Star", ignoreCase = true) == true ||
                            card.rarity?.contains("2 Star", ignoreCase = true) == true ||
                            card.rarity?.contains("3 Star", ignoreCase = true) == true
            
            val currentPrice = if (isVintage) {
                uiState.vintagePrices.find { 
                    it.finish == userCard.finish && 
                    it.condition == userCard.condition &&
                    it.printing == userCard.printing
                } ?: uiState.vintagePrices.firstOrNull()
            } else {
                uiState.prices.find { 
                    it.finish == userCard.finish && 
                    it.condition == userCard.condition 
                } ?: uiState.prices.firstOrNull()
            }

            val marketPriceValue = when (currentPrice) {
                is PriceEntity -> currentPrice.marketPrice
                is VintagePriceEntity -> currentPrice.marketPrice
                else -> null
            } ?: 0.0

            val sourceStr = when (currentPrice) {
                is PriceEntity -> currentPrice.source
                is VintagePriceEntity -> currentPrice.source
                else -> "Unknown"
            }

            val timestampValue = when (currentPrice) {
                is PriceEntity -> currentPrice.timestamp
                is VintagePriceEntity -> currentPrice.timestamp
                else -> 0L
            }

            val primaryType = remember(card.types) {
                if (card.types?.startsWith("[") == true) {
                    card.types.substringAfter("\"").substringBefore("\"")
                } else {
                    card.types?.split(",")?.firstOrNull()?.trim()
                }
            }

            val energyColor = remember(primaryType) {
                when (primaryType?.lowercase()) {
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

            val isGold = finish == PricingUtils.FINISH_GOLD
            val showFinishAnims = uiState.showFinishAnimations

            Box(modifier = Modifier.fillMaxSize()) {
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
                        .energyEffect(type = primaryType, show = uiState.showEnergyAnimations)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Card Image Display - Container with extra padding to allow 3D rotation without clipping
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 12.dp), // Increased padding for 3D tilt
                        contentAlignment = Alignment.Center
                    ) {
                        // The card container applies effects and rotation.
                        // We use a Box instead of a Surface to avoid restrictive clipping.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // Slightly smaller width for more rotation room
                                .aspectRatio(0.718f)
                                .holoEffect(
                                    finish = finish,
                                    show = showFinishAnims,
                                    useGyro = true, // Enabled gyro for better tilting experience
                                    isFullArt = isFullArt,
                                    cornerRadius = 12.dp
                                )
                                .shimmerEffect(
                                    show = isGold && showFinishAnims,
                                    cornerRadius = 12.dp
                                )
                        ) {
                            AsyncImage(
                                model = "${card.image}/high.webp",
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val setIcon = remember(set.logo, set.symbol, set.id, uiState.preferSetLogo) {
                                if (uiState.preferSetLogo) {
                                    set.logo ?: set.symbol ?: "https://assets.tcgdex.net/en/sets/${set.id}/logo.png"
                                } else {
                                    set.symbol ?: set.logo ?: "https://assets.tcgdex.net/en/sets/${set.id}/symbol.png"
                                }
                            }
                            
                            AsyncImage(
                                model = setIcon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Fit,
                                error = rememberVectorPainter(Icons.Rounded.ImageNotSupported)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            
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
                                val priceText = NumberFormat.getCurrencyInstance(Locale.US).format(marketPriceValue)
                                Text(priceText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { onEvent(CardDetailEvent.RefreshPrice) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    if (uiState.isRefreshingPrice) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Price", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Text("Estimated Market Value", style = MaterialTheme.typography.labelLarge)
                            if (currentPrice != null) {
                                Text(
                                    "Source: ${sourceStr.uppercase()} • Updated ${getRelativeTime(timestampValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    if (isVintage && uiState.vintagePrices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Edition Pricing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val variantsByEdition = uiState.vintagePrices
                                    .filter { it.condition == condition && it.finish == finish }
                                    .groupBy { it.printing }

                                if (variantsByEdition.isEmpty()) {
                                    Text(
                                        "No market data for current selection",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    val editions = listOf(
                                        PricingUtils.PRINTING_1ST_EDITION,
                                        PricingUtils.PRINTING_SHADOWLESS,
                                        PricingUtils.PRINTING_UNLIMITED
                                    )
                                    
                                    editions.forEach { edition ->
                                        val price = variantsByEdition[edition]?.firstOrNull()?.marketPrice
                                        if (price != null) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    edition.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (printing == edition) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (printing == edition) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    NumberFormat.getCurrencyInstance(Locale.US).format(price),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
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
                                options = listOf(
                                    PricingUtils.CONDITION_NM,
                                    PricingUtils.CONDITION_LP,
                                    PricingUtils.CONDITION_MP,
                                    PricingUtils.CONDITION_HP,
                                    PricingUtils.CONDITION_DMG
                                ),
                                onSelected = { condition = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            DetailDropdown(
                                label = "Printing",
                                value = printing,
                                options = listOf(
                                    PricingUtils.PRINTING_UNLIMITED,
                                    PricingUtils.PRINTING_SHADOWLESS,
                                    PricingUtils.PRINTING_PROMO,
                                    PricingUtils.PRINTING_1ST_EDITION
                                ),
                                onSelected = { printing = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            DetailDropdown(
                                label = "Finish",
                                value = finish,
                                options = listOf(
                                    PricingUtils.FINISH_NORMAL,
                                    PricingUtils.FINISH_HOLOFOIL,
                                    PricingUtils.FINISH_REVERSE_HOLO,
                                    PricingUtils.FINISH_TEXTURED,
                                    PricingUtils.FINISH_GOLD
                                ),
                                onSelected = { finish = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Folders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        uiState.folders.forEach { folder ->
                            val isInFolder = uiState.cardFolderIds.contains(folder.id)
                            FilterChip(
                                selected = isInFolder,
                                onClick = { 
                                    if (isInFolder) onEvent(CardDetailEvent.RemoveCardFromFolder(folder.id))
                                    else onEvent(CardDetailEvent.AddCardToFolder(folder.id))
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

@Preview(showBackground = true)
@Composable
private fun CardDetailPreview() {
    MaterialTheme {
        val mockId = "swsh1-1"
        CardDetailContent(
            uiState = CardDetailUiState(
                isLoading = false,
                cardWithDetails = CardWithDetails(
                    userCard = UserCardEntity(id = 1L, cardId = mockId, quantity = 2, condition = "Near Mint", finish = "Holo"),
                    card = CardEntity(id = mockId, localId = "1", name = "Charizard", image = "url", setId = "swsh1", rarity = "Rare Holo", category = "Pokemon", types = "Fire", dexId = "6"),
                    set = SetEntity(id = "swsh1", name = "Sword & Shield", series = "Sword & Shield", logo = "url", symbol = "url", totalCards = 202, officialCards = 202, releaseDate = "2020-02-07")
                ),
                folders = listOf(FolderEntity(id = 1L, name = "Favorites", icon = "star")),
                cardFolderIds = setOf(1L),
                prices = listOf(PriceEntity(cardId = mockId, finish = "Holo", condition = "Near Mint", marketPrice = 45.99, lowPrice = 40.0, midPrice = 45.0, highPrice = 50.0, source = "TCGPlayer", timestamp = System.currentTimeMillis()))
            ),
            onNavigateBack = {},
            onEvent = {}
        )
    }
}

@Composable
fun CardDetailScreen(
    repository: VaultioRepository,
    userCardId: Long,
    onNavigateBack: () -> Unit,
) {
    val viewModel: CardDetailViewModel = viewModel(
        factory = CardDetailViewModelFactory(repository, SavedStateHandle(mapOf("userCardId" to userCardId)))
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                CardDetailEffect.Navigation.Back -> onNavigateBack()
            }
        }
    }

    CardDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
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

@Composable
fun rememberVectorPainter(image: androidx.compose.ui.graphics.vector.ImageVector) = 
    androidx.compose.ui.graphics.vector.rememberVectorPainter(image)
