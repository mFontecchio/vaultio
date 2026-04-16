package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.ui.collection.PokedexEntry
import com.mrhayami.vaultio.ui.collection.PokedexSettings

@Composable
fun PokedexView(
    entries: List<PokedexEntry>,
    settings: PokedexSettings,
    onDexClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.navigationBarsPadding()
    ) {
        items(entries, key = { it.dexNumber }) { entry ->
            val isCollected = entry.isCollected
            val spriteType = if (settings.useShinySprites) "shiny" else "pokemon"
            val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/$spriteType/${entry.dexNumber}.png"

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(
                        if (isCollected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onDexClick(entry.dexNumber) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("#${entry.dexNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = spriteUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit,
                            alpha = if (isCollected) 1f else 0.3f,
                            colorFilter = if (isCollected) null else ColorFilter.colorMatrix(
                                ColorMatrix().apply { setToSaturation(0f) }
                            )
                        )
                    }
                    if (isCollected && entry.pokemonName != null) {
                        Text(
                            entry.pokemonName,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
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
                    isCollected = it % 2 == 0
                )
            },
            settings = PokedexSettings(),
            onDexClick = {}
        )
    }
}
