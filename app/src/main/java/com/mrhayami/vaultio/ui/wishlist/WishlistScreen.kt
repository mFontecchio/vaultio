package com.mrhayami.vaultio.ui.wishlist

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.WishlistCardWithDetails
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.collection.AddCardModal
import com.mrhayami.vaultio.ui.collection.CollectionEvent
import com.mrhayami.vaultio.ui.collection.CollectionUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    repository: VaultioRepository,
    onNavigateBack: () -> Unit,
    viewModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(repository))
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is WishlistEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wishlist", fontWeight = FontWeight.Bold)
                        Text(
                            "$${
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    uiState.totalWishlistValue
                                )
                            } Est. Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add to Wishlist")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.wishlistItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your wishlist is empty.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.wishlistItems, key = { it.wishlistCard.id }) { item ->
                    WishlistItem(
                        item = item,
                        onDelete = { viewModel.onEvent(WishlistEvent.RemoveFromWishlist(item.wishlistCard.id)) },
                        onMoveToCollection = { viewModel.onEvent(WishlistEvent.MoveToCollection(item.wishlistCard.id)) }
                    )
                }
            }
        }

        if (showAddModal) {
            ModalBottomSheet(
                onDismissRequest = { showAddModal = false },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                AddCardModal(
                    uiState = CollectionUiState(
                        searchResults = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        folders = emptyList() // Wishlist doesn't use folders for now
                    ),
                    onEvent = { event ->
                        when (event) {
                            is CollectionEvent.OnSearchRemoteCards -> viewModel.onEvent(
                                WishlistEvent.SearchRemoteCards(event.query)
                            )

                            is CollectionEvent.OnAddUserCard -> {
                                viewModel.onEvent(
                                    WishlistEvent.AddCardToWishlist(
                                        event.card,
                                        event.quantity,
                                        event.condition,
                                        event.printing,
                                        event.finish
                                    )
                                )
                                showAddModal = false
                            }

                            else -> {}
                        }
                    },
                    onWishlistConfirm = { card, q, c, p, f ->
                        viewModel.onEvent(
                            WishlistEvent.AddCardToWishlist(
                                card, q, c, p, f
                            )
                        )
                        showAddModal = false
                    },
                    onDismiss = { showAddModal = false }
                )
            }
        }
    }
}

@Composable
fun WishlistItem(
    item: WishlistCardWithDetails,
    onDelete: () -> Unit,
    onMoveToCollection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = "${item.card.image}/low.webp",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${item.set.name} • #${item.card.localId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${item.wishlistCard.condition} • ${item.wishlistCard.finish}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.wishlistCard.quantity > 1) {
                    Text(
                        "Qty: ${item.wishlistCard.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Row {
                    IconButton(onClick = onMoveToCollection) {
                        Icon(
                            Icons.Rounded.ShoppingCart,
                            contentDescription = "Move to Collection",
                            tint = Color(0xFF00E676)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
