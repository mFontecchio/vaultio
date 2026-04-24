package com.mrhayami.vaultio.ui.stats

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
                    item { SectionTitle("Top 5 Most Valuable Cards") }
                    items(
                        state.mostValuableCards,
                        key = { it.details.userCard.id }) { cardWithValue ->
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Total Value Section
            Column(
                modifier = Modifier.weight(1.1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Total Value",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = formatCurrency(totalValue),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Subtle Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Total Cards Section
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Total Cards",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = cardCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RarityDistributionChart(distribution: Map<String, Int>) {
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

            // Legend
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

@Composable
fun DonutChart(
    data: List<Pair<String, Int>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thickness: androidx.compose.ui.unit.Dp = 30.dp
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.second }.toFloat()
    val density = LocalDensity.current
    val thicknessPx = with(density) { thickness.toPx() }

    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = thicknessPx)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun TypeDistributionChart(distribution: Map<String, Int>) {
    val sortedData = remember(distribution) {
        // Take top 7 types to keep the radar chart readable
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

@Composable
fun RadarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxVal = remember(data) { data.maxOf { it.second }.toFloat().coerceAtLeast(1f) }
    val numLines = 4 // background grid circles

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width.coerceAtMost(size.height) / 2
        val angleStep = (2 * PI / data.size).toFloat()

        // Draw background grid
        for (i in 1..numLines) {
            val currentRadius = radius * (i.toFloat() / numLines)
            val path = Path()
            for (j in data.indices) {
                val angle = j * angleStep - PI.toFloat() / 2
                val x = centerX + currentRadius * cos(angle)
                val y = centerY + currentRadius * sin(angle)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // Draw axes and labels
        data.forEachIndexed { index, pair ->
            val angle = index * angleStep - PI.toFloat() / 2
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)

            // Axis line
            drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1.dp.toPx())

            // Label
            val label = pair.first
            val textLayoutResult = textMeasurer.measure(label, style = labelStyle)
            val textWidth = textLayoutResult.size.width
            val textHeight = textLayoutResult.size.height

            // Position label outside the radius
            val labelPadding = 12.dp.toPx()
            val labelX = centerX + (radius + labelPadding) * cos(angle) - textWidth / 2
            val labelY = centerY + (radius + labelPadding) * sin(angle) - textHeight / 2

            drawText(textLayoutResult, topLeft = Offset(labelX, labelY))
        }

        // Draw data polygon
        val dataPath = Path()
        data.forEachIndexed { index, pair ->
            val angle = index * angleStep - PI.toFloat() / 2
            val valueRadius = radius * (pair.second / maxVal)
            val x = centerX + valueRadius * cos(angle)
            val y = centerY + valueRadius * sin(angle)
            if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(dataPath, color.copy(alpha = 0.3f), style = Fill)
        drawPath(dataPath, color, style = Stroke(width = 2.dp.toPx()))

        // Draw points
        data.forEachIndexed { index, pair ->
            val angle = index * angleStep - PI.toFloat() / 2
            val valueRadius = radius * (pair.second / maxVal)
            val x = centerX + valueRadius * cos(angle)
            val y = centerY + valueRadius * sin(angle)
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun MostValuableCardItem(cardWithValue: CardWithValue, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageUrl = remember(cardWithValue.details.card.image) {
        "${cardWithValue.details.card.image}/low.webp"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                if (cardWithValue.details.userCard.quantity > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shape = RoundedCornerShape(bottomStart = 4.dp, topEnd = 4.dp),
                        modifier = Modifier.clip(RoundedCornerShape(topEnd = 4.dp))
                    ) {
                        Text(
                            text = "x${cardWithValue.details.userCard.quantity}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = cardWithValue.details.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cardWithValue.details.card.rarity ?: "Unknown Rarity"} • ${cardWithValue.details.set.name}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(cardWithValue.value * cardWithValue.details.userCard.quantity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (cardWithValue.details.userCard.quantity > 1) {
                    Text(
                        text = "${formatCurrency(cardWithValue.value)} ea.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
