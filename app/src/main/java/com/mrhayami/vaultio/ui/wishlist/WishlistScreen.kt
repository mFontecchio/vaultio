package com.mrhayami.vaultio.ui.wishlist

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.WishlistCardEntity
import com.mrhayami.vaultio.data.local.WishlistCardWithDetails
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.collection.CollectionEvent
import com.mrhayami.vaultio.ui.collection.components.AddCardModal
import com.mrhayami.vaultio.ui.components.AddScanFabMenu
import com.mrhayami.vaultio.ui.components.ConfirmDestructiveDialog
import com.mrhayami.vaultio.ui.components.EmptyState
import com.mrhayami.vaultio.ui.components.MicroCaptureFanfare
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WishlistScreen(
    repository: VaultioRepository,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit = {},
    onNavigateToCardDetail: (Long) -> Unit = {},
    hideBackButton: Boolean = false,
    onItemClick: ((WishlistItemUiModel) -> Unit)? = null,
    viewModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(repository))
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showAddModal by remember { mutableStateOf(false) }
    var fanfarePosition by remember { mutableStateOf<Offset?>(null) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is WishlistEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
                is WishlistEffect.NavigateToCardDetail -> onNavigateToCardDetail(effect.userCardId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlist") },
                navigationIcon = {
                    if (!hideBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AddScanFabMenu(
                onAddCard = { showAddModal = true },
                onScan = onNavigateToScanner,
                addLabel = "Add"
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.wishlistItems.isEmpty()) {
            EmptyState(
                title = "Your wishlist is empty",
                message = "Add cards you want so you can track them and move them into your collection.",
                icon = Icons.Rounded.FavoriteBorder,
                primaryLabel = "Add cards",
                onPrimaryClick = { showAddModal = true },
                secondaryLabel = "Open scanner",
                onSecondaryClick = onNavigateToScanner,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
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
                item(key = "wishlist_value_header") {
                    Text(
                        text = String.format(
                            Locale.US,
                            "$%.2f Est. Value",
                            uiState.totalWishlistValue
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.wishlistItems, key = { it.details.wishlistCard.id }) { item ->
                    WishlistItem(
                        item = item,
                        onClick = {
                            if (onItemClick != null) {
                                onItemClick(item)
                            } else {
                                viewModel.onEvent(
                                    WishlistEvent.OpenOwnedCard(item.details.wishlistCard.cardId)
                                )
                            }
                        },
                        onDelete = {
                            pendingDeleteId = item.details.wishlistCard.id
                        },
                        onMoveToCollection = { pos ->
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            fanfarePosition = pos
                            viewModel.onEvent(
                                WishlistEvent.MoveToCollection(item.details.wishlistCard.id)
                            )
                        },
                        modifier = Modifier.animateItem()
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
                    searchResults = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    folders = emptyList(), // Wishlist doesn't use folders for now
                    setsMap = emptyMap(), // Can be improved by passing sets if needed
                    onEvent = { event: CollectionEvent ->
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
                    onWishlistConfirm = { card: TcgDexCard, q: Int, c: String, p: String, f: String ->
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

        pendingDeleteId?.let { id ->
            ConfirmDestructiveDialog(
                title = "Remove from wishlist?",
                message = "This card will be removed from your wishlist.",
                confirmLabel = "Remove",
                onConfirm = {
                    viewModel.onEvent(WishlistEvent.RemoveFromWishlist(id))
                },
                onDismiss = { pendingDeleteId = null }
            )
        }

        fanfarePosition?.let { pos ->
            MicroCaptureFanfare(
                center = pos,
                onAnimationFinished = { fanfarePosition = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistItem(
    item: WishlistItemUiModel,
    onDelete: () -> Unit,
    onMoveToCollection: (Offset) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    val catchInteractionSource = remember { MutableInteractionSource() }
    val isCatchPressed by catchInteractionSource.collectIsPressedAsState()
    val catchScale by animateFloatAsState(
        targetValue = if (isCatchPressed) 0.92f else 1f,
        animationSpec = spring(),
        label = "catchPressScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                    model = "${item.details.card.image}/low.webp",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.details.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${item.details.set.name} • #${item.details.card.localId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${item.details.wishlistCard.condition} • ${item.details.wishlistCard.finish}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.details.wishlistCard.quantity > 1) {
                    Text(
                        "Qty: ${item.details.wishlistCard.quantity}",
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
                    IconButton(
                        onClick = { onMoveToCollection(buttonPosition) },
                        interactionSource = catchInteractionSource,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = catchScale
                                scaleY = catchScale
                            }
                            .onGloballyPositioned { layoutCoordinates ->
                                val windowPos = layoutCoordinates.positionInWindow()
                                buttonPosition = Offset(
                                    windowPos.x + layoutCoordinates.size.width / 2f,
                                    windowPos.y + layoutCoordinates.size.height / 2f
                                )
                            }
                    ) {
                        Icon(
                            Icons.Rounded.CatchingPokemon,
                            contentDescription = "Move to collection",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.details.wishlistCard.quantity > 1) {
                        Text(
                            text = "Total: $${
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    item.price * item.details.wishlistCard.quantity
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@VaultioPreviews
@Composable
private fun WishlistItemPreview() {
    val sampleDetails = WishlistCardWithDetails(
        wishlistCard = WishlistCardEntity(
            id = 1,
            cardId = "swsh11-1",
            quantity = 2,
            condition = "Near Mint",
            finish = "Normal"
        ),
        card = CardEntity(
            id = "swsh11-1",
            localId = "1",
            name = "Giratina VSTAR",
            image = "https://images.pokemontcg.io/swsh11/131",
            setId = "swsh11",
            rarity = "Rare Holo VSTAR",
            category = "Pokemon",
            types = "Dragon",
            dexId = "487"
        ),
        set = SetEntity(
            id = "swsh11",
            name = "Lost Origin",
            series = "Sword & Shield",
            logo = "https://images.pokemontcg.io/swsh11/logo.png",
            symbol = "https://images.pokemontcg.io/swsh11/symbol.png",
            totalCards = 196,
            releaseDate = "2022-09-09"
        )
    )

    val sampleItem = WishlistItemUiModel(
        details = sampleDetails,
        price = 12.50
    )

    VaultioPreview {
        WishlistItem(
            item = sampleItem,
            onDelete = {},
            onMoveToCollection = {}
        )
    }
}

@VaultioPreviews
@Composable
private fun WishlistEmptyStatePreview() {
    VaultioPreview {
        EmptyState(
            title = "Your wishlist is empty",
            message = "Add cards you want so you can track them and move them into your collection.",
            icon = Icons.Rounded.FavoriteBorder,
            primaryLabel = "Add cards",
            onPrimaryClick = {},
            secondaryLabel = "Open scanner",
            onSecondaryClick = {}
        )
    }
}
