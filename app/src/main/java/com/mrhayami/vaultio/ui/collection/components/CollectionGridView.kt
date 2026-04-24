package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.ui.collection.CardUiModel
import com.mrhayami.vaultio.ui.collection.GridSettings
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.EntranceType
import com.mrhayami.vaultio.ui.components.shimmerEffect
import com.mrhayami.vaultio.ui.components.staggeredEntrance
import com.mrhayami.vaultio.ui.theme.VaultioTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionGridView(
    userCards: List<CardUiModel>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    settings: GridSettings,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridLoadTime = remember { System.currentTimeMillis() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.columns),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.navigationBarsPadding()
    ) {
        itemsIndexed(userCards, key = { _, item -> item.details.userCard.id }) { index, item ->
            val id = item.details.userCard.id
            val isSelected = selectedIds.contains(id)
            val isNew =
                remember(item.details.userCard.dateAdded) { System.currentTimeMillis() - item.details.userCard.dateAdded < 60_000 }

            val isInitialLoad = remember { System.currentTimeMillis() - gridLoadTime < 500 }

            val onClick = remember(id, onCardClick) { { onCardClick(id) } }
            val onLongClick = remember(id, onCardLongClick) { { onCardLongClick(id) } }

            Card(
                modifier = Modifier
                    .staggeredEntrance(
                        index = index,
                        type = EntranceType.ScaleUp,
                        enabled = isInitialLoad
                    )
                    .animateItem()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(
                    3.dp,
                    MaterialTheme.colorScheme.primary
                ) else null
            ) {
                Box {
                    val imageModifier = remember(isNew) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.718f)
                            .shimmerEffect(show = isNew)
                    }
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data("${item.details.card.image}/high.webp")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = imageModifier,
                        contentScale = ContentScale.FillBounds
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else if (settings.showBadges) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                "${item.details.userCard.quantity}",
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }

                    if (!isSelected) {
                        CardAttributeBadges(
                            finish = item.details.userCard.finish,
                            printing = item.details.userCard.printing,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionGridViewPreview() {
    VaultioTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CollectionGridView(
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
                    ),
                    CardUiModel(
                        mockCardWithDetails(
                            4L,
                            "Blastoise",
                            "https://images.pokemontcg.io/swsh4/1"
                        ), 89.99
                    )
                ),
                selectedIds = emptySet(),
                isSelectionMode = false,
                settings = GridSettings(columns = 2, showBadges = true),
                onCardClick = {},
                onCardLongClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Selection Mode")
@Composable
private fun CollectionGridViewSelectionPreview() {
    VaultioTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CollectionGridView(
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
                    ),
                    CardUiModel(
                        mockCardWithDetails(
                            4L,
                            "Blastoise",
                            "https://images.pokemontcg.io/swsh4/1"
                        ), 89.99
                    )
                ),
                selectedIds = setOf(1L, 3L),
                isSelectionMode = true,
                settings = GridSettings(columns = 2, showBadges = true),
                onCardClick = {},
                onCardLongClick = {}
            )
        }
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

