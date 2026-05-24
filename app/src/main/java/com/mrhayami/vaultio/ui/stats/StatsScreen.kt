package com.mrhayami.vaultio.ui.stats

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.CollectionSnapshotEntity
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsViewState,
    onEvent: (StatsEvent) -> Unit,
    sideEffects: Flow<StatsEffect>,
    onNavigation: (StatsEffect.Navigation) -> Unit,
) {
    LaunchedEffect(Unit) {
        onEvent(StatsEvent.OnScreenOpened)
        sideEffects.collect { effect ->
            when (effect) {
                is StatsEffect.Navigation -> onNavigation(effect)
                is StatsEffect.ShowError -> { /* Handle error */
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection Statistics") },
                navigationIcon = {
                    IconButton(onClick = { onNavigation(StatsEffect.Navigation.GoBack) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        StatsContent(
            state = state,
            onNavigation = onNavigation,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun StatsContent(
    state: StatsViewState,
    onNavigation: (StatsEffect.Navigation) -> Unit,
    modifier: Modifier = Modifier
) {
    if ((state.isLoading && state.cardCount == 0)) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (state.cardCount == 0) {
        EmptyStatsContent(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            summarySection(state.totalValue, state.cardCount)

            if (state.snapshots.isNotEmpty()) {
                valueHistorySection(state.snapshots)
            }

            if (state.distributionByRarity.isNotEmpty()) {
                rarityDistributionSection(state.distributionByRarity)
            }

            if (state.distributionByType.isNotEmpty()) {
                typeDistributionSection(state.distributionByType)
            }

            if (state.mostValuableCards.isNotEmpty()) {
                mostValuableCardsSection(state.mostValuableCards, onNavigation)
            }

            if (state.setCompletion.isNotEmpty()) {
                setCompletionSection(state.setCompletion)
            }
        }
    }
}

@Composable
private fun EmptyStatsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No cards in collection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Scan some cards to see statistics!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun LazyListScope.summarySection(totalValue: Double, cardCount: Int) {
    item {
        SummaryCards(totalValue, cardCount)
    }
}

private fun LazyListScope.valueHistorySection(snapshots: List<CollectionSnapshotEntity>) {
    item {
        SectionTitle("Value History")
        ValueHistoryChart(snapshots)
    }
}

@Composable
fun ValueHistoryChart(snapshots: List<CollectionSnapshotEntity>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(snapshots) {
        if (snapshots.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(snapshots.map { it.totalValue })
                }
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            val latest = snapshots.lastOrNull()?.totalValue ?: 0.0
            val oldest = snapshots.firstOrNull()?.totalValue ?: 0.0
            val diff = latest - oldest
            val percent = if (oldest > 0) (diff / oldest) * 100 else 0.0

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (diff >= 0) "+${formatCurrency(diff)}" else formatCurrency(diff),
                    color = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${String.format(Locale.US, "%.1f", percent)}%)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("Since first snapshot", style = MaterialTheme.typography.labelSmall)

            Spacer(Modifier.height(16.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp),
            )
        }
    }
}

private fun LazyListScope.rarityDistributionSection(distribution: Map<String, Int>) {
    item {
        SectionTitle("Rarity Distribution")
        RarityDistributionSection(distribution)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RarityDistributionSection(distribution: Map<String, Int>) {
    val sortedData = remember(distribution) {
        distribution.toList().sortedByDescending { it.second }.take(6)
    }
    val totalCount = remember(distribution) { distribution.values.sum() }

    val rarityColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                DonutChart(
                    data = sortedData,
                    colors = rarityColors,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedData.forEachIndexed { index, pair ->
                    val color = rarityColors[index % rarityColors.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${pair.first} (${(pair.second * 100f / totalCount).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.typeDistributionSection(distribution: Map<String, Int>) {
    item {
        SectionTitle("Type Distribution")
        TypeDistributionSection(distribution)
    }
}

@Composable
private fun TypeDistributionSection(distribution: Map<String, Int>) {
    val sortedData = remember(distribution) {
        distribution.toList().sortedByDescending { it.second }.take(7)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RadarChart(
                data = sortedData,
                modifier = Modifier
                    .size(280.dp)
                    .padding(24.dp)
            )
        }
    }
}

private fun LazyListScope.mostValuableCardsSection(
    cards: List<CardWithValue>,
    onNavigation: (StatsEffect.Navigation) -> Unit,
) {
    item { SectionTitle("Top 5 Most Valuable Cards") }
    items(cards, key = { it.details.userCard.id }) { cardWithValue ->
        MostValuableCardItem(
            cardWithValue = cardWithValue
        ) {
            onNavigation(
                StatsEffect.Navigation.GoToCardDetail(
                    cardWithValue.details.userCard.id
                )
            )
        }
    }
}

private fun LazyListScope.setCompletionSection(completion: List<SetCompletionInfo>) {
    item { SectionTitle("Set Completion") }
    items(completion) { info ->
        SetCompletionItem(info)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun StatsScreenPreview() {
    val mockSnapshots = listOf(
        CollectionSnapshotEntity(
            id = 1,
            totalValue = 1000.0,
            cardCount = 50,
            timestamp = System.currentTimeMillis() - 86400000 * 2
        ),
        CollectionSnapshotEntity(
            id = 2,
            totalValue = 1200.0,
            cardCount = 55,
            timestamp = System.currentTimeMillis() - 86400000 * 1
        ),
        CollectionSnapshotEntity(
            id = 3,
            totalValue = 1500.0,
            cardCount = 60,
            timestamp = System.currentTimeMillis()
        )
    )

    val mockCard = CardEntity(
        id = "swsh4-1",
        localId = "1",
        name = "Charizard VMAX",
        image = null,
        setId = "swsh4",
        rarity = "Rare Holo VMAX",
        category = "Pokémon",
        types = "Fire",
        dexId = "6"
    )

    val mockSet = SetEntity(
        id = "swsh4",
        name = "Vivid Voltage",
        series = "Sword & Shield",
        logo = null,
        symbol = null,
        totalCards = 185,
        officialCards = 185,
        releaseDate = "2020/11/13"
    )

    val mockUserCard = UserCardEntity(
        id = 1,
        cardId = "swsh4-1",
        quantity = 1
    )

    val mockCardWithDetails = CardWithDetails(
        userCard = mockUserCard,
        card = mockCard,
        set = mockSet
    )

    val mockState = StatsViewState(
        isLoading = false,
        totalValue = 2450.75,
        cardCount = 142,
        snapshots = mockSnapshots,
        mostValuableCards = listOf(
            CardWithValue(mockCardWithDetails, 150.50),
            CardWithValue(
                mockCardWithDetails.copy(
                    card = mockCard.copy(
                        name = "Pikachu VMAX",
                        rarity = "Rare Rainbow"
                    )
                ), 250.00
            ),
            CardWithValue(
                mockCardWithDetails.copy(
                    card = mockCard.copy(
                        name = "Rayquaza VMAX",
                        rarity = "Rare Alt Art"
                    )
                ), 450.00
            )
        ),
        distributionByRarity = mapOf(
            "Common" to 50,
            "Uncommon" to 30,
            "Rare" to 20,
            "Rare Holo" to 15,
            "Ultra Rare" to 10
        ),
        distributionByType = mapOf(
            "Fire" to 25,
            "Water" to 20,
            "Grass" to 30,
            "Lightning" to 15,
            "Psychic" to 10
        ),
        setCompletion = listOf(
            SetCompletionInfo("swsh4", "Vivid Voltage", null, 45, 185, 24.3f),
            SetCompletionInfo("swsh3", "Darkness Ablaze", null, 120, 189, 63.5f),
            SetCompletionInfo("base1", "Base Set", null, 102, 102, 100f)
        )
    )

    VaultioTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StatsScreen(
                state = mockState,
                onEvent = {},
                sideEffects = emptyFlow(),
                onNavigation = {}
            )
        }
    }
}
