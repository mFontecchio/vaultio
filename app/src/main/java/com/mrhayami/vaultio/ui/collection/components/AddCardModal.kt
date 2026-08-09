package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.ui.collection.CollectionEvent
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCardModal(
    searchResults: List<TcgDexCard>,
    isSearching: Boolean,
    folders: List<FolderEntity>,
    setsMap: Map<String, com.mrhayami.vaultio.data.local.SetEntity>,
    onEvent: (CollectionEvent) -> Unit,
    onWishlistConfirm: ((TcgDexCard, Int, String, String, String) -> Unit)? = null,
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }
                                innerTextField()
                            }
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(searchResults, key = { it.id }) { card ->
                    val itemModifier = remember(card) {
                        Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCard = card }
                    }
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
                            val set = setsMap[setId]
                            val setName = set?.name ?: setId
                            val officialCount = set?.officialCards ?: 0
                            val cardNumberText =
                                if (officialCount > 0) "${card.localId}/$officialCount" else card.localId
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
                        modifier = itemModifier
                    )
                }
            }
        }
    } else {
        MetadataModal(
            card = selectedCard!!,
            folders = folders,
            onConfirm = { q, c, p, f, folderIds ->
                onEvent(CollectionEvent.OnAddUserCard(selectedCard!!, q, c, p, f, folderIds))
                onDismiss()
            },
            onWishlistConfirm = onWishlistConfirm?.let { callback ->
                { q, c, p, f ->
                    callback(selectedCard!!, q, c, p, f)
                }
            },
            onBack = { selectedCard = null }
        )
    }
}

@VaultioPreviews
@Composable
private fun AddCardModalPreview() {
    VaultioPreview {
        AddCardModal(
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
            ),
            setsMap = emptyMap(),
            onEvent = {},
            onDismiss = {}
        )
    }
}
