package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.ui.collection.ListSettings
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.shimmerEffect
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionListView(
    userCards: List<CardWithDetails>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    settings: ListSettings,
    preferSetLogo: Boolean,
    allPrices: List<PriceEntity>,
    allVintagePrices: List<VintagePriceEntity>,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = modifier.navigationBarsPadding()
    ) {
        itemsIndexed(userCards, key = { _, item -> item.userCard.id }) { index, item ->
            val isSelected = selectedIds.contains(item.userCard.id)
            val isNew = System.currentTimeMillis() - item.userCard.dateAdded < 60_000 // 1 minute
            
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = (index % 12) * 30)) + 
                        slideInHorizontally(tween(400, delayMillis = (index % 12) * 30)) { -it / 8 },
                modifier = Modifier.animateItem()
            ) {
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.card.name, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            CardAttributeBadges(
                                finish = item.userCard.finish,
                                printing = item.userCard.printing
                            )
                        }
                    },
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
                                .shimmerEffect(show = isNew)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val setIcon = if (preferSetLogo) {
                                        item.set.logo ?: item.set.symbol ?: "https://assets.tcgdex.net/en/sets/${item.set.id}/logo.png"
                                    } else {
                                        item.set.symbol ?: item.set.logo ?: "https://assets.tcgdex.net/en/sets/${item.set.id}/symbol.png"
                                    }
                                    
                                    AsyncImage(
                                        model = setIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text("x${item.userCard.quantity}", fontWeight = FontWeight.Bold)
                                }
                                if (settings.showPrices) {
                                    val price = item.userCard.manualPrice ?: run {
                                        val cardId = item.card.id
                                        val finish = item.userCard.finish
                                        val condition = item.userCard.condition
                                        val printing = item.userCard.printing
                                        
                                        val foundPrice = allPrices.find { 
                                            it.cardId == cardId && it.finish == finish && it.condition == condition 
                                        }?.marketPrice ?: allVintagePrices.find {
                                            it.cardId == cardId && it.finish == finish && it.condition == condition && it.printing == printing
                                        }?.marketPrice
                                        
                                        foundPrice ?: 0.0
                                    }
                                    Text("$${String.format(Locale.US, "%.2f", price)}", style = MaterialTheme.typography.bodySmall)
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
}

@Preview(showBackground = true)
@Composable
private fun CollectionListViewPreview() {
    VaultioTheme {
        CollectionListView(
            userCards = listOf(
                mockCardWithDetails("1", "Pikachu"),
                mockCardWithDetails("2", "Charizard"),
                mockCardWithDetails("3", "Mewtwo")
            ),
            selectedIds = setOf(2L),
            isSelectionMode = false,
            settings = ListSettings(showPrices = true, isCompact = false),
            preferSetLogo = true,
            allPrices = emptyList(),
            allVintagePrices = emptyList(),
            onCardClick = {},
            onCardLongClick = {}
        )
    }
}

private fun mockCardWithDetails(id: String, name: String) = CardWithDetails(
    userCard = UserCardEntity(
        id = id.toLong(),
        cardId = id,
        quantity = 1,
        dateAdded = System.currentTimeMillis()
    ),
    card = CardEntity(
        id = id,
        localId = id,
        name = name,
        image = "https://images.pokemontcg.io/swsh1/1",
        setId = "swsh1",
        rarity = "Rare",
        category = "Pokemon",
        types = "Lightning",
        dexId = "25"
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
