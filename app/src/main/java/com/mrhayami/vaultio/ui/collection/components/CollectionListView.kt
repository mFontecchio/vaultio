package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.ui.collection.CardUiModel
import com.mrhayami.vaultio.ui.collection.ListSettings
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.EntranceType
import com.mrhayami.vaultio.ui.components.shimmerEffect
import com.mrhayami.vaultio.ui.components.staggeredEntrance
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionListView(
    userCards: List<CardUiModel>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    settings: ListSettings,
    preferSetLogo: Boolean,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listLoadTime = remember { System.currentTimeMillis() }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = modifier.navigationBarsPadding()
    ) {
        itemsIndexed(userCards, key = { _, item -> item.details.userCard.id }) { index, item ->
            val id = item.details.userCard.id
            val isSelected = selectedIds.contains(id)
            val isNew =
                remember(item.details.userCard.dateAdded) { System.currentTimeMillis() - item.details.userCard.dateAdded < 60_000 } // 1 minute

            val isInitialLoad = remember { System.currentTimeMillis() - listLoadTime < 500 }

            val onClick = remember(id, onCardClick) { { onCardClick(id) } }
            val onLongClick = remember(id, onCardLongClick) { { onCardLongClick(id) } }

            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.details.card.name, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        CardAttributeBadges(
                            finish = item.details.userCard.finish,
                            printing = item.details.userCard.printing
                        )
                    }
                },
                supportingContent = {
                    if (!settings.isCompact) {
                        Text("${item.details.set.name} • ${item.details.card.localId}")
                    }
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(if (settings.isCompact) 48.dp else 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .shimmerEffect(show = isNew)
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data("${item.details.card.image}/low.webp")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter
                        )
                    }
                },
                trailingContent = {
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val setIcon = if (preferSetLogo) {
                                    item.details.set.logo ?: item.details.set.symbol
                                    ?: "https://assets.tcgdex.net/en/sets/${item.details.set.id}/logo.png"
                                } else {
                                    item.details.set.symbol ?: item.details.set.logo
                                    ?: "https://assets.tcgdex.net/en/sets/${item.details.set.id}/symbol.png"
                                }

                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(setIcon)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 4.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    "x${item.details.userCard.quantity}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (settings.showPrices) {
                                Text(
                                    "$${String.format(Locale.US, "%.2f", item.price)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .staggeredEntrance(
                        index = index,
                        type = EntranceType.SlideIn,
                        enabled = isInitialLoad
                    )
                    .animateItem()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.5f
                        ) else MaterialTheme.colorScheme.surface
                    )
            )
        }
    }
}

@VaultioPreviews
@Composable
private fun CollectionListViewPreview() {
    VaultioPreview {
        CollectionListView(
            userCards = listOf(
                CardUiModel(
                    mockCardWithDetails(
                        1L,
                        "Pikachu",
                        "https://images.pokemontcg.io/swsh1/1"
                    ), 1.99
                ),
                CardUiModel(
                    mockCardWithDetails(
                        2L,
                        "Charizard",
                        "https://images.pokemontcg.io/swsh4/25"
                    ), 299.99
                ),
                CardUiModel(
                    mockCardWithDetails(
                        3L,
                        "Mewtwo",
                        "https://images.pokemontcg.io/base1/10"
                    ), 49.99
                )
            ),
            selectedIds = emptySet(),
            isSelectionMode = false,
            settings = ListSettings(showPrices = true, isCompact = false),
            preferSetLogo = true,
            onCardClick = {},
            onCardLongClick = {}
        )
    }
}

@VaultioPreviews
@Composable
private fun CollectionListViewCompactPreview() {
    VaultioPreview {
        CollectionListView(
            userCards = listOf(
                CardUiModel(
                    mockCardWithDetails(
                        1L,
                        "Pikachu",
                        "https://images.pokemontcg.io/swsh1/1"
                    ), 1.99
                ),
                CardUiModel(
                    mockCardWithDetails(
                        2L,
                        "Charizard",
                        "https://images.pokemontcg.io/swsh4/25"
                    ), 299.99
                )
            ),
            selectedIds = emptySet(),
            isSelectionMode = false,
            settings = ListSettings(showPrices = true, isCompact = true),
            preferSetLogo = true,
            onCardClick = {},
            onCardLongClick = {}
        )
    }
}

@VaultioPreviews
@Composable
private fun CollectionListViewSelectionPreview() {
    VaultioPreview {
        CollectionListView(
            userCards = listOf(
                CardUiModel(
                    mockCardWithDetails(
                        1L,
                        "Pikachu",
                        "https://images.pokemontcg.io/swsh1/1"
                    ), 1.99
                ),
                CardUiModel(
                    mockCardWithDetails(
                        2L,
                        "Charizard",
                        "https://images.pokemontcg.io/swsh4/25"
                    ), 299.99
                )
            ),
            selectedIds = setOf(1L),
            isSelectionMode = true,
            settings = ListSettings(showPrices = true, isCompact = false),
            preferSetLogo = true,
            onCardClick = {},
            onCardLongClick = {}
        )
    }
}

private fun mockCardWithDetails(id: Long, name: String, imageUrl: String) = CardWithDetails(
    userCard = UserCardEntity(
        id = id,
        cardId = id.toString(),
        quantity = 1,
        dateAdded = System.currentTimeMillis(),
        finish = "Normal",
        condition = "Near Mint",
        printing = "Standard"
    ),
    card = CardEntity(
        id = id.toString(),
        localId = id.toString(),
        name = name,
        image = imageUrl,
        setId = "swsh1",
        rarity = "Rare",
        category = "Pokemon",
        types = "Lightning",
        dexId = id.toString()
    ),
    set = SetEntity(
        id = "swsh1",
        name = "Sword & Shield",
        series = "Sword & Shield",
        logo = "https://assets.tcgdex.net/en/sets/swsh1/logo.png",
        symbol = "https://assets.tcgdex.net/en/sets/swsh1/symbol.png",
        totalCards = 202,
        releaseDate = "2020-02-07"
    )
)
