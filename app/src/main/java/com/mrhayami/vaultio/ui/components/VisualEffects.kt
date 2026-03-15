package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

fun Modifier.shimmerEffect(
    show: Boolean = true,
    colors: List<Color> = listOf(
        Color.White.copy(alpha = 0.0f),
        Color.White.copy(alpha = 0.4f),
        Color.White.copy(alpha = 0.0f),
    )
): Modifier = composed {
    if (!show) return@composed this
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(translateAnim, translateAnim),
                end = Offset(translateAnim + 500f, translateAnim + 500f)
            ),
            blendMode = BlendMode.SrcOver
        )
    }
}

fun Modifier.sparkleEffect(
    show: Boolean = true,
    sparkleCount: Int = 70
): Modifier = composed {
    if (!show) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_progress"
    )

    val sparkleProperties = remember {
        List(sparkleCount) {
            SparkleProp(
                pos = Offset(Random.nextFloat(), Random.nextFloat()),
                baseSize = Random.nextFloat() * 1.2f + 0.3f,
                color = when(Random.nextInt(6)) {
                    0 -> Color(0xFFFFF9C4) // Gold
                    1 -> Color(0xFFE1F5FE) // Blue
                    2 -> Color(0xFFF3E5F5) // Purple
                    3 -> Color(0xFFF1F8E9) // Green
                    4 -> Color(0xFFFFF3E0) // Orange
                    else -> Color.White
                },
                blinkCycles = Random.nextInt(1, 5),
                rotationCycles = Random.nextInt(-2, 3).let { if (it == 0) 1 else it },
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                hasHighlight = Random.nextFloat() > 0.5f
            )
        }
    }

    this.drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val pi2 = 2f * PI.toFloat()
        
        sparkleProperties.forEach { prop ->
            val angle = progress * pi2 * prop.blinkCycles + prop.phase
            val alpha = (sin(angle) * 0.5f + 0.5f).pow(3)
            
            if (alpha > 0.05f) {
                val x = prop.pos.x * width
                val y = prop.pos.y * height
                val sizePx = prop.baseSize.dp.toPx() * (0.6f + alpha * 0.4f)
                
                drawCircle(
                    color = prop.color.copy(alpha = alpha * 0.8f),
                    radius = sizePx,
                    center = Offset(x, y)
                )
                
                drawCircle(
                    color = prop.color.copy(alpha = alpha * 0.15f),
                    radius = sizePx * 4.5f,
                    center = Offset(x, y)
                )

                if (prop.hasHighlight && alpha > 0.65f) {
                    val flareAlpha = (alpha - 0.65f) / 0.35f
                    val flareSize = sizePx * 5.5f
                    
                    withTransform({
                        translate(x, y)
                        rotate(progress * 360f * prop.rotationCycles + (prop.phase * 180f / PI.toFloat()))
                    }) {
                        val path = Path().apply {
                            moveTo(0f, -flareSize)
                            quadraticTo(flareSize * 0.06f, 0f, flareSize, 0f)
                            quadraticTo(flareSize * 0.06f, 0f, 0f, flareSize)
                            quadraticTo(-flareSize * 0.06f, 0f, -flareSize, 0f)
                            quadraticTo(-flareSize * 0.06f, 0f, 0f, -flareSize)
                            close()
                        }
                        drawPath(path = path, color = Color.White.copy(alpha = flareAlpha * 0.85f))
                    }
                }
            }
        }
    }
}

private data class SparkleProp(
    val pos: Offset,
    val baseSize: Float,
    val color: Color,
    val blinkCycles: Int,
    val rotationCycles: Int,
    val phase: Float,
    val hasHighlight: Boolean
)

fun Modifier.energyEffect(
    type: String?,
    show: Boolean = true
): Modifier = composed {
    if (!show || type == null) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "energy")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_progress"
    )

    val normalizedType = type.lowercase()

    when (normalizedType) {
        "fire" -> embersEffect(progress)
        "grass" -> forestSpiritEffect(progress)
        "water" -> bubblesEffect(progress)
        "lightning", "electric" -> lightningStormEffect(progress)
        "metal", "steel" -> this
        "fighting" -> impactShockwaveEffect(progress)
        "darkness", "dark" -> darkAuraEffect(progress)
        "psychic" -> telekinesisRipplesEffect(progress)
        "fairy" -> enchantedGlitterEffect(progress)
        "dragon" -> dragonBreathEffect(progress)
        "colorless" -> gustsOfWindEffect(progress)
        else -> this
    }
}

private fun Modifier.embersEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val random = Random(42)
    repeat(30) { i ->
        val phase = (progress + i / 30f) % 1f
        val speedFactor = 0.7f + (random.nextFloat() * 0.6f)
        val actualPhase = (phase * speedFactor) % 1f
        
        val x = (random.nextFloat() * size.width) + sin(actualPhase * PI.toFloat() * 8).toFloat() * 12f
        val y = size.height * (1f - actualPhase)
        val alpha = sin(actualPhase * PI.toFloat()).toFloat()
        val sizePx = (1f + random.nextFloat() * 3f).dp.toPx()
        
        val emberColor = when(i % 4) {
            0 -> Color(0xFFFF3D00)
            1 -> Color(0xFFFF9100)
            2 -> Color(0xFFFFC400)
            else -> Color(0xFFFFEA00)
        }
        
        drawCircle(
            color = emberColor.copy(alpha = alpha * 0.9f),
            radius = sizePx,
            center = Offset(x, y),
            blendMode = BlendMode.Screen
        )
        
        if (alpha > 0.4f) {
            drawCircle(
                color = emberColor.copy(alpha = alpha * 0.25f),
                radius = sizePx * 5f,
                center = Offset(x, y),
                blendMode = BlendMode.Screen
            )
        }
    }
}

/**
 * Expert Take: "Forest Spirit" Effect for Grass Energy
 * Focuses on atmospheric lighting and floating organic matter rather than a literal leaf shower.
 */
private fun Modifier.forestSpiritEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    
    // 1. Sunlight Dappling: Shifting soft radial gradients representing light through a canopy
    repeat(3) { i ->
        val phase = (progress + i / 3f) % 1f
        val xOffset = sin(phase * 2 * PI.toFloat()).toFloat() * 50.dp.toPx()
        val yOffset = cos(phase * 2 * PI.toFloat()).toFloat() * 30.dp.toPx()
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC8E6C9).copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width / 2 + xOffset, size.height / 4 + yOffset),
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width / 2 + xOffset, size.height / 4 + yOffset)
        )
    }

    // 2. Rising Forest Spores: Tiny glowing specs that drift lazily
    repeat(25) { i ->
        val random = Random(43L + i)
        val speed = 0.3f + random.nextFloat() * 0.4f
        val particleProgress = (progress * speed + (i.toFloat() / 25f)) % 1f
        
        val startX = random.nextFloat() * size.width
        val x = startX + sin(particleProgress * PI.toFloat() * 4).toFloat() * 30.dp.toPx()
        val y = size.height * (1.1f - particleProgress * 1.2f)
        
        val alpha = sin(particleProgress * PI.toFloat()).toFloat() * 0.5f
        
        drawCircle(
            color = Color(0xFFE8F5E9).copy(alpha = alpha),
            radius = (1f + random.nextFloat() * 2f).dp.toPx(),
            center = Offset(x, y),
            blendMode = BlendMode.Plus
        )
    }

    // 3. Floating Petals: Very slow, high-quality drifting petals
    repeat(4) { i ->
        val random = Random(100L + i)
        val petalProgress = (progress * 0.15f + (i.toFloat() / 4f)) % 1f
        
        // Falling in a slow, wide Bezier-like curve
        val startX = random.nextFloat() * size.width
        val x = startX + sin(petalProgress * PI.toFloat() * 2).toFloat() * 60.dp.toPx()
        val y = -50.dp.toPx() + (size.height + 100.dp.toPx()) * petalProgress
        
        val alpha = sin(petalProgress * PI.toFloat()).toFloat() * 0.4f
        val rotation = petalProgress * 360f + (i * 90f)
        
        withTransform({
            translate(x, y)
            rotate(rotation)
            scale(abs(sin(petalProgress * 10f)).coerceAtLeast(0.3f), 1f)
        }) {
            val petalWidth = 10.dp.toPx()
            val petalHeight = 14.dp.toPx()
            val path = Path().apply {
                moveTo(0f, -petalHeight / 2)
                quadraticTo(petalWidth / 2, 0f, 0f, petalHeight / 2)
                quadraticTo(-petalWidth / 2, 0f, 0f, -petalHeight / 2)
                close()
            }
            drawPath(path, color = Color(0xFF81C784).copy(alpha = alpha))
        }
    }
}

private fun Modifier.bubblesEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val random = Random(44)
    repeat(20) { i ->
        val phase = (progress + i / 20f) % 1f
        val x = random.nextFloat() * size.width
        val y = size.height * (1f - phase)
        val alpha = sin(phase * PI.toFloat()).toFloat()
        val radius = (4f + random.nextFloat() * 8f).dp.toPx()

        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.3f),
            radius = radius,
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun Modifier.lightningStormEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val topBound = size.height * 0.45f
    val stormIndex = (progress * 18).toInt()
    val stormRandom = Random(stormIndex)
    
    if (stormRandom.nextFloat() > 0.85f) {
        val pulseCount = stormRandom.nextInt(1, 4)
        val chunkProgress = (progress * 18 % 1f)
        val pulseWindow = chunkProgress * pulseCount
        val currentPulseIndex = floor(pulseWindow).toInt()
        
        val pulseProgress = pulseWindow % 1f
        if (pulseProgress < 0.25f) {
            val alphaFactor = (1f - (pulseProgress / 0.25f)).pow(1.5f)
            val pulseRandom = Random(stormIndex + currentPulseIndex * 79)
            val startX = pulseRandom.nextFloat() * size.width
            val flashAlpha = (0.12f + pulseRandom.nextFloat() * 0.18f) * alphaFactor
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE1F5FE).copy(alpha = flashAlpha), Color.Transparent),
                    startY = 0f,
                    endY = topBound
                ),
                blendMode = BlendMode.Screen
            )
            
            val boltPath = Path().apply {
                moveTo(startX, 0f)
                var currX = startX
                var currY = 0f
                while (currY < topBound * 0.8f) {
                    currX += (pulseRandom.nextFloat() - 0.5f) * 160.dp.toPx()
                    currY += pulseRandom.nextFloat() * 80.dp.toPx()
                    lineTo(currX, currY)
                    if (pulseRandom.nextFloat() > 0.65f) {
                        val branchX = currX + (pulseRandom.nextFloat() - 0.5f) * 90.dp.toPx()
                        val branchY = currY + pulseRandom.nextFloat() * 45.dp.toPx()
                        moveTo(currX, currY)
                        lineTo(branchX, branchY)
                        moveTo(currX, currY)
                    }
                }
            }
            
            drawPath(path = boltPath, color = Color.White.copy(alpha = flashAlpha * 3f), style = Stroke(width = 1.3.dp.toPx()), blendMode = BlendMode.Screen)
            drawPath(path = boltPath, color = Color(0xFF03A9F4).copy(alpha = flashAlpha), style = Stroke(width = 5.dp.toPx()), blendMode = BlendMode.Screen)
        }
    }
}

private fun Modifier.impactShockwaveEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val eventIndex = (progress * 8).toInt()
    val random = Random(eventIndex)
    
    if (random.nextFloat() > 0.7f) {
        val eventProgress = (progress * 8 % 1f)
        val alpha = (1f - eventProgress).pow(2)
        val centerX = random.nextFloat() * size.width
        val centerY = random.nextFloat() * size.height * 0.6f
        val radius = eventProgress * size.width * 0.4f
        
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.2f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )
        
        repeat(5) { j ->
            val pRandom = Random(eventIndex + j)
            val angle = pRandom.nextFloat() * 2f * PI.toFloat()
            val dist = radius + pRandom.nextFloat() * 20.dp.toPx()
            drawCircle(
                color = Color(0xFFD7CCC8).copy(alpha = alpha * 0.4f),
                radius = 2.dp.toPx(),
                center = Offset(centerX + cos(angle) * dist, centerY + sin(angle) * dist)
            )
        }
    }
}

private fun Modifier.darkAuraEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    repeat(4) { i ->
        val auraProgress = (progress + i / 4f) % 1f
        val alpha = sin(auraProgress * PI.toFloat()).toFloat() * 0.3f
        val radius = (0.5f + auraProgress * 0.5f) * size.width
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width / 2, size.height * 0.3f),
                radius = radius
            ),
            radius = radius,
            center = Offset(size.width / 2, size.height * 0.3f),
            blendMode = BlendMode.Multiply
        )
    }
}

private fun Modifier.telekinesisRipplesEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    repeat(3) { i ->
        val rippleProgress = (progress + i / 3f) % 1f
        val radius = rippleProgress * size.width
        val alpha = (1f - rippleProgress) * 0.5f
        drawCircle(color = Color(0xFFE1BEE7).copy(alpha = alpha), radius = radius, center = Offset(size.width / 2, size.height / 2), style = Stroke(width = 2.dp.toPx()))
    }
}

private fun Modifier.enchantedGlitterEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val random = Random(49)
    repeat(20) { i ->
        val pProgress = (progress + i / 20f) % 1f
        val alpha = sin(pProgress * PI.toFloat()).toFloat()
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        
        drawCircle(
            color = Color(0xFFF48FB1).copy(alpha = alpha * 0.6f),
            radius = 3.dp.toPx(),
            center = Offset(x, y)
        )
        
        if (alpha > 0.7f) {
            val crossSize = 6.dp.toPx() * alpha
            drawLine(Color.White.copy(alpha = alpha), Offset(x - crossSize, y), Offset(x + crossSize, y), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = alpha), Offset(x, y - crossSize), Offset(x, y + crossSize), strokeWidth = 1.dp.toPx())
        }
    }
}

private fun Modifier.dragonBreathEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val waveProgress = progress * 2f * PI.toFloat()
    val brush = Brush.verticalGradient(
        0.0f to Color(0xFF673AB7).copy(alpha = 0.1f + sin(waveProgress).absoluteValue * 0.1f),
        0.5f to Color(0xFF311B92).copy(alpha = 0.2f + cos(waveProgress).absoluteValue * 0.1f),
        1.0f to Color.Transparent
    )
    drawRect(brush = brush)
    
    val random = Random(50)
    repeat(5) { i ->
        val wProgress = (progress * 0.6f + i / 5f) % 1f
        val x = (random.nextFloat() * size.width) + sin(wProgress * PI.toFloat() * 4).toFloat() * 20.dp.toPx()
        val y = size.height * (1f - wProgress)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7E57C2).copy(alpha = 0.2f * sin(wProgress * PI.toFloat()).toFloat()), Color.Transparent),
                center = Offset(x, y),
                radius = 60.dp.toPx()
            ),
            radius = 60.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

private fun Modifier.gustsOfWindEffect(progress: Float): Modifier = drawWithContent {
    drawContent()
    val random = Random(47)
    repeat(5) { i ->
        val phase = (progress + i / 5f) % 1f
        val x = phase * size.width * 2.5f - size.width * 0.75f
        val y = random.nextFloat() * size.height
        val alpha = (1f - abs(phase - 0.5f) * 2).pow(2) * 0.2f
        val path = Path().apply {
            moveTo(x, y)
            cubicTo(x + size.width * 0.2f, y - 40.dp.toPx(), x + size.width * 0.4f, y + 40.dp.toPx(), x + size.width * 0.6f, y)
        }
        drawPath(path = path, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)), start = Offset(x, y), end = Offset(x + size.width * 0.6f, y)), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
        val path2 = Path().apply { moveTo(x - 30.dp.toPx(), y + 15.dp.toPx()); cubicTo(x + size.width * 0.15f, y - 25.dp.toPx(), x + size.width * 0.35f, y + 55.dp.toPx(), x + size.width * 0.55f, y + 15.dp.toPx()) }
        drawPath(path = path2, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha * 0.5f), Color.White.copy(alpha = 0f)), start = Offset(x - 30.dp.toPx(), y + 15.dp.toPx()), end = Offset(x + size.width * 0.55f, y + 15.dp.toPx())), style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round))
    }
}
