package com.mrhayami.vaultio.ui.stats

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsViewState,
    onEvent: (StatsEvent) -> Unit,
    sideEffects: Flow<StatsEffect>,
    onNavigation: (StatsEffect.Navigation) -> Unit
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
        if (state.isLoading && state.cardCount == 0) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryCards(state.totalValue, state.cardCount)
                }

                if (state.snapshots.isNotEmpty()) {
                    item {
                        SectionTitle("Value History")
                        ValueHistoryChart(state.snapshots)
                    }
                }

                if (state.distributionByRarity.isNotEmpty()) {
                    item {
                        SectionTitle("Rarity Distribution")
                        RarityDistributionChart(state.distributionByRarity)
                    }
                }

                if (state.distributionByType.isNotEmpty()) {
                    item {
                        SectionTitle("Type Distribution")
                        TypeDistributionChart(state.distributionByType)
                    }
                }

                if (state.mostValuableCards.isNotEmpty()) {
                    item { SectionTitle("Most Valuable Cards") }
                    items(state.mostValuableCards) { cardWithValue ->
                        MostValuableCardItem(
                            cardWithValue = cardWithValue,
                            onClick = {
                                onNavigation(
                                    StatsEffect.Navigation.GoToCardDetail(
                                        cardWithValue.details.userCard.id
                                    )
                                )
                            }
                        )
                    }
                }

                if (state.setCompletion.isNotEmpty()) {
                    item { SectionTitle("Set Completion") }
                    items(state.setCompletion) { info ->
                        SetCompletionItem(info)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCards(totalValue: Double, cardCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Rounded.TrendingUp, null)
                Spacer(Modifier.height(8.dp))
                Text("Total Value", style = MaterialTheme.typography.labelMedium)
                Text(
                    formatCurrency(totalValue),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.Collections, null)
                Spacer(Modifier.height(8.dp))
                Text("Total Cards", style = MaterialTheme.typography.labelMedium)
                Text(
                    cardCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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

@Composable
fun RarityDistributionChart(distribution: Map<String, Int>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val sortedData = remember(distribution) {
        distribution.toList().sortedByDescending { it.second }.take(8)
    }
    val bottomAxisValueFormatter = remember(sortedData) {
        CartesianValueFormatter { _, value, _ ->
            sortedData.getOrNull(value.toInt())?.first.orEmpty()
        }
    }

    LaunchedEffect(sortedData) {
        modelProducer.runTransaction {
            columnSeries {
                series(sortedData.map { it.second.toFloat() })
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisValueFormatter,
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp),
            )
        }
    }
}

@Composable
fun TypeDistributionChart(distribution: Map<String, Int>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val sortedData = remember(distribution) {
        distribution.toList().sortedByDescending { it.second }.take(10)
    }
    val bottomAxisValueFormatter = remember(sortedData) {
        CartesianValueFormatter { _, value, _ ->
            sortedData.getOrNull(value.toInt())?.first.orEmpty()
        }
    }

    LaunchedEffect(sortedData) {
        modelProducer.runTransaction {
            columnSeries {
                series(sortedData.map { it.second.toFloat() })
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisValueFormatter,
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp),
            )
        }
    }
}

@Composable
fun MostValuableCardItem(cardWithValue: CardWithValue, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cardWithValue.details.card.image,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = cardWithValue.details.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cardWithValue.details.card.rarity} • ${cardWithValue.details.set.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = formatCurrency(cardWithValue.value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SetCompletionItem(info: SetCompletionInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = info.logo,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    info.setName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${info.collectedCount}/${info.totalCount}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { info.completionPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
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

@Preview(showBackground = true)
@Composable
private fun SummaryCardsPreview() {
    VaultioTheme {
        Surface(Modifier.padding(16.dp)) {
            SummaryCards(totalValue = 1234.56, cardCount = 789)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TypeDistributionChartPreview() {
    val mockDistribution = mapOf(
        "Fire" to 25,
        "Water" to 20,
        "Grass" to 30,
        "Lightning" to 15,
        "Psychic" to 10,
        "Fighting" to 8,
        "Darkness" to 12,
        "Metal" to 5,
        "Colorless" to 18,
        "Dragon" to 4
    )
    VaultioTheme {
        Surface(Modifier.padding(16.dp)) {
            TypeDistributionChart(distribution = mockDistribution)
        }
    }
}

@Preview(showBackground = true, name = "Most Valuable Card Item")
@Composable
private fun MostValuableCardItemPreview() {
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

    val mockCardWithValue = CardWithValue(mockCardWithDetails, 150.50)

    VaultioTheme {
        Surface(Modifier.padding(16.dp)) {
            MostValuableCardItem(
                cardWithValue = mockCardWithValue,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetCompletionItemPreview() {
    val mockInfo = SetCompletionInfo(
        setId = "swsh4",
        setName = "Vivid Voltage",
        logo = null,
        collectedCount = 45,
        totalCount = 185,
        completionPercentage = 24.3f
    )
    VaultioTheme {
        Surface(Modifier.padding(16.dp)) {
            SetCompletionItem(info = mockInfo)
        }
    }
}
