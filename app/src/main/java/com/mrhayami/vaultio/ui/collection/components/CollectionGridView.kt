package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.ui.collection.GridSettings
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.shimmerEffect
import com.mrhayami.vaultio.ui.theme.VaultioTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionGridView(
    userCards: List<CardWithDetails>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    settings: GridSettings,
    onCardClick: (Long) -> Unit,
    onCardLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.columns),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.navigationBarsPadding()
    ) {
        itemsIndexed(userCards, key = { _, item -> item.userCard.id }) { index, item ->
            val isSelected = selectedIds.contains(item.userCard.id)
            val isNew = System.currentTimeMillis() - item.userCard.dateAdded < 60_000 // 1 minute
            
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 400, delayMillis = (index % 12) * 40)) +
                        scaleIn(initialScale = 0.8f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 400, delayMillis = (index % 12) * 40)),
                modifier = Modifier.animateItem()
            ) {
                Card(
                    modifier = Modifier.combinedClickable(
                        onClick = { onCardClick(item.userCard.id) },
                        onLongClick = { onCardLongClick(item.userCard.id) }
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Box {
                        AsyncImage(
                            model = "${item.card.image}/high.webp",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.718f)
                                .shimmerEffect(show = isNew),
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
                        
                        if (!isSelected) {
                            CardAttributeBadges(
                                finish = item.userCard.finish,
                                printing = item.userCard.printing,
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
}

@Preview(showBackground = true)
@Composable
private fun CollectionGridViewPreview() {
    VaultioTheme {
        CollectionGridView(
            userCards = listOf(
                mockCardWithDetails("1", "Pikachu"),
                mockCardWithDetails("2", "Charizard"),
                mockCardWithDetails("3", "Mewtwo"),
                mockCardWithDetails("4", "Blastoise")
            ),
            selectedIds = setOf(2L),
            isSelectionMode = false,
            settings = GridSettings(columns = 2, showBadges = true),
            onCardClick = {},
            onCardLongClick = {}
        )
    }
}

private fun mockCardWithDetails(id: String, name: String) = CardWithDetails(
    userCard = UserCardEntity(id = id.toLong(), cardId = id, quantity = 2),
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
        logo = "",
        symbol = "",
        totalCards = 202,
        releaseDate = "2020-02-07"
    )
)
