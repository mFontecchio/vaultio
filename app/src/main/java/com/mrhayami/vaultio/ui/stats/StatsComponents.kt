package com.mrhayami.vaultio.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SummaryCards(totalValue: Double, cardCount: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

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
fun DonutChart(
    data: List<Pair<String, Int>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thickness: Dp = 30.dp
) {
    if (data.isEmpty()) return

    val total = remember(data) { data.sumOf { it.second }.toFloat() }
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
    val numLines = 4

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width.coerceAtMost(size.height) / 2
        val angleStep = ((2 * PI) / data.size).toFloat()

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

        data.forEachIndexed { index, pair ->
            val angle = index * angleStep - PI.toFloat() / 2
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)

            drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1.dp.toPx())

            val label = pair.first
            val textLayoutResult = textMeasurer.measure(label, style = labelStyle)
            val textWidth = textLayoutResult.size.width
            val textHeight = textLayoutResult.size.height

            val labelPadding = 12.dp.toPx()
            val labelX = centerX + (radius + labelPadding) * cos(angle) - textWidth / 2
            val labelY = centerY + (radius + labelPadding) * sin(angle) - textHeight / 2

            drawText(textLayoutResult, topLeft = Offset(labelX, labelY))
        }

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
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(enable = true)
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
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

@VaultioPreviews
@Composable
private fun SummaryCardsPreview() {
    VaultioPreview {
        SummaryCards(totalValue = 2450.75, cardCount = 142)
    }
}

@VaultioPreviews
@Composable
private fun DonutChartPreview() {
    VaultioPreview {
        val data = mapOf(
            "Common" to 50,
            "Uncommon" to 30,
            "Rare" to 20,
            "Ultra Rare" to 10,
        ).toList()
        val colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
        )
        DonutChart(
            data = data,
            colors = colors,
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
        )
    }
}

@VaultioPreviews
@Composable
private fun RadarChartPreview() {
    VaultioPreview {
        RadarChart(
            data = mapOf(
                "Fire" to 25,
                "Water" to 20,
                "Grass" to 30,
                "Lightning" to 15,
                "Psychic" to 10,
            ).toList(),
            modifier = Modifier
                .size(280.dp)
                .padding(24.dp),
        )
    }
}

@VaultioPreviews
@Composable
private fun MostValuableCardItemPreview() {
    VaultioPreview {
        val mockCard = CardEntity(
            id = "swsh4-1",
            localId = "1",
            name = "Charizard VMAX",
            image = null,
            setId = "swsh4",
            rarity = "Rare Holo VMAX",
            category = "Pokémon",
            types = "Fire",
            dexId = "6",
        )
        val mockSet = SetEntity(
            id = "swsh4",
            name = "Vivid Voltage",
            series = "Sword & Shield",
            logo = null,
            symbol = null,
            totalCards = 185,
            officialCards = 185,
            releaseDate = "2020/11/13",
        )
        val mockUserCard = UserCardEntity(
            id = 1,
            cardId = "swsh4-1",
            quantity = 2,
        )
        MostValuableCardItem(
            cardWithValue = CardWithValue(
                details = CardWithDetails(
                    userCard = mockUserCard,
                    card = mockCard,
                    set = mockSet,
                ),
                value = 150.50,
            ),
            onClick = {},
        )
    }
}

@VaultioPreviews
@Composable
private fun SetCompletionItemPreview() {
    VaultioPreview {
        SetCompletionItem(
            info = SetCompletionInfo(
                setId = "swsh4",
                setName = "Vivid Voltage",
                logo = null,
                collectedCount = 45,
                totalCount = 185,
                completionPercentage = 24.3f,
            ),
        )
    }
}

@VaultioPreviews
@Composable
private fun SectionTitlePreview() {
    VaultioPreview {
        SectionTitle(title = "Top 5 Most Valuable Cards")
    }
}
