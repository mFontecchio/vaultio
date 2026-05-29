package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.ui.collection.PokedexEntry
import com.mrhayami.vaultio.ui.collection.PokedexSettings
import com.mrhayami.vaultio.ui.components.EntranceType
import com.mrhayami.vaultio.ui.components.staggeredEntrance

@Composable
fun PokedexView(
    entries: List<PokedexEntry>,
    settings: PokedexSettings,
    onDexClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridLoadTime = remember { System.currentTimeMillis() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.navigationBarsPadding()
    ) {
        itemsIndexed(entries, key = { _, it -> it.dexNumber }) { index, entry ->
            val isCollected = entry.isCollected
            val isInitialLoad = remember { System.currentTimeMillis() - gridLoadTime < 500 }

            Box(
                modifier = Modifier
                    .aspectRatio(1f / 1.15f)
                    .staggeredEntrance(
                        index = index,
                        type = EntranceType.ScaleUp,
                        enabled = isInitialLoad
                    )
                    .animateItem()
                    .background(
                        if (isCollected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onDexClick(entry.dexNumber) },
                contentAlignment = Alignment.Center
            ) {
                if (entry.cardCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            "${entry.cardCount}",
                            modifier = Modifier.padding(3.dp, 2.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(entry.spriteUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit,
                            alpha = if (isCollected) 1f else 0.3f,
                            colorFilter = if (isCollected) null else ColorFilter.colorMatrix(
                                ColorMatrix().apply { setToSaturation(0f) }
                            )
                        )
                    }
                    Text(
                        "#${entry.dexNumber}",
                        fontSize = 10.sp,
                        maxLines = 1,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false // Removes the top/bottom extra space
                            )
                        )
                    )
                    if (isCollected && entry.pokemonName != null) {
                        Text(
                            entry.pokemonName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false // Removes the top/bottom extra space
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PokedexViewPreview() {
    MaterialTheme {
        PokedexView(
            entries = List(8) { 
                PokedexEntry(
                    dexNumber = it + 1,
                    pokemonName = "Pokemon ${it + 1}",
                    cardCount = 1,
                    totalQuantity = 1,
                    representativeImage = null,
                    spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${it + 1}.png",
                    isCollected = it % 2 == 0
                )
            },
            settings = PokedexSettings(),
            onDexClick = {}
        )
    }
}
