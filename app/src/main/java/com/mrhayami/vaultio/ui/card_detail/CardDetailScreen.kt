package com.mrhayami.vaultio.ui.card_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.repository.VaultioRepository
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
                title = { Text(uiState.cardWithDetails?.card?.name ?: "") },
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
                }
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

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = "${card.image}/high.png",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Fit
                )

                // Set and Rarity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (set.symbol != null) {
                        AsyncImage(
                            model = set.symbol,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(set.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "#${card.localId} / ${set.totalCards}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (card.rarity != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small) {
                            Text(
                                card.rarity,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val priceText = currentPrice?.marketPrice?.let {
                                NumberFormat.getCurrencyInstance(Locale.US).format(it)
                            } ?: "$0.00"
                            Text("Market Value: $priceText", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.refreshPrice() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Price")
                            }
                        }
                        if (currentPrice != null) {
                            Text(
                                "Source: ${currentPrice.source.uppercase()} • Updated ${getRelativeTime(currentPrice.timestamp)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Collection Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Text(
                    "Collected on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(userCard.dateAdded))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Quantity Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (quantity > 1) quantity-- }) { Icon(Icons.Rounded.Remove, null) }
                    Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { quantity++ }) { Icon(Icons.Rounded.Add, null) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdowns for Condition, Printing, Finish
                DetailDropdown(
                    label = "Condition",
                    value = condition,
                    options = listOf("Near Mint", "Lightly Played", "Moderately Played", "Heavily Played", "Damaged"),
                    onSelected = { condition = it }
                )

                DetailDropdown(
                    label = "Printing",
                    value = printing,
                    options = listOf("Standard", "First Edition", "Unlimited", "Promo"),
                    onSelected = { printing = it }
                )

                DetailDropdown(
                    label = "Finish",
                    value = finish,
                    options = listOf("Non Holo", "Holo", "Reverse Holo", "Textured", "Gold"),
                    onSelected = { finish = it }
                )

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
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
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
