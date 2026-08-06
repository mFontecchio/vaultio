package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.PricingUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * ADVANCED TCG HOLOFOIL SHADER
 * 
 * Inspired by https://poke-holo.simey.me/
 * Recreates physical properties:
 * 1. Dynamic Spectral Gradient (Color Dodge)
 * 2. Specular Flare bands (Screen)
 * 3. Static Galaxy/Cosmos grain pattern with clusters
 * 4. 3D Gyro tilt (simulated via drag) with integrated dynamic shadow
 */
fun Modifier.holoEffect(
    finish: String,
    show: Boolean = true,
    useGyro: Boolean = true,
    isFullArt: Boolean = false,
    cornerRadius: Dp = 12.dp
): Modifier = composed {
    if (!show || finish == PricingUtils.FINISH_NORMAL) {
        // Still apply the shadow and 3D base layer even if no holo finish
        return@composed this.graphicsLayer {
            cameraDistance = 15f * density
            shadowElevation = 6.dp.toPx()
            shape = RoundedCornerShape(cornerRadius)
            clip = false
        }
    }

    var gyroX by remember { mutableFloatStateOf(0f) }
    var gyroY by remember { mutableFloatStateOf(0f) }

    val interactiveModifier = if (useGyro) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    gyroX = 0f
                    gyroY = 0f
                },
                onDragCancel = {
                    gyroX = 0f
                    gyroY = 0f
                }
            ) { change, dragAmount ->
                change.consume()
                gyroY = (gyroY + dragAmount.x * 0.1f).coerceIn(-15f, 15f)
                gyroX = (gyroX - dragAmount.y * 0.1f).coerceIn(-15f, 15f)
            }
        }
    } else {
        Modifier
    }

    // Smoothly animate the gyro changes
    val effX by animateFloatAsState(
        targetValue = gyroX,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "effX"
    )
    val effY by animateFloatAsState(
        targetValue = gyroY,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "effY"
    )

    // Dense Galaxy grains pattern
    val grainProperties = remember {
        val random = Random(42)
        List(300) {
            HoloGrain(
                pos = Offset(random.nextFloat(), random.nextFloat()),
                size = random.nextFloat() * 1.5f + 0.3f,
                reflectivity = random.nextFloat() * 0.6f + 0.4f,
                parallaxFactor = random.nextFloat() * 10f + 2f,
                patternType = when {
                    random.nextFloat() > 0.95f -> GrainType.STAR
                    random.nextFloat() > 0.85f -> GrainType.GLOW_DOT
                    else -> GrainType.DOT
                }
            )
        }
    }

    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
            Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFF4B0082),
            Color(0xFF8B00FF), Color(0xFFFF0000)
        )
    }

    this
        .then(interactiveModifier)
        .graphicsLayer {
            rotationX = effX
            rotationY = effY
            cameraDistance = 15f * density

            // Integrated Shadow that follows the card tilt
            shadowElevation = 6.dp.toPx()
            shape = RoundedCornerShape(cornerRadius)

            // CRITICAL: Disable clipping so tilted edges and shadow are not cut off
            clip = false
        }
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height

            if (w <= 0f || h <= 0f) return@drawWithContent

            // Mask the holo effects to the card's rounded corners
            val cardPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, w, h),
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                )
            }

            clipPath(cardPath) {
                val isReverseHolo = finish == PricingUtils.FINISH_REVERSE_HOLO

                // Light direction moves with card tilt
                val lX = (gyroY / 15f).coerceIn(-1f, 1f)
                val lY = (-gyroX / 15f).coerceIn(-1f, 1f)
                val sheenProgress = (lX + lY + 1f) / 2f

                // 1. DYNAMIC SPECTRAL SWEEP (The "Foil Sheen")
                drawRect(
                    brush = Brush.linearGradient(
                        colors = rainbowColors,
                        start = Offset(sheenProgress * w * 2f - w, 0f),
                        end = Offset(sheenProgress * w * 2f, h)
                    ),
                    alpha = if (isReverseHolo) 0.15f else 0.22f,
                    blendMode = BlendMode.ColorDodge
                )

                // 2. SPECULAR SHINE / FLARE (Glossy bands)
                drawRect(
                    brush = Brush.linearGradient(
                        0.45f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.35f),
                        0.55f to Color.Transparent,
                        start = Offset(sheenProgress * w * 3f - w * 1.5f, 0f),
                        end = Offset(sheenProgress * w * 3f, h)
                    ),
                    blendMode = BlendMode.Screen
                )

                // 3. COSMOS GRAINS WITH PARALLAX
                grainProperties.forEach { grain ->
                    // Parallax shift based on tilt to simulate depth
                    val px = grain.pos.x * w + (lX * grain.parallaxFactor)
                    val py = grain.pos.y * h + (lY * grain.parallaxFactor)

                    // Calculate if grain is hit by the light sheen
                    val distToSheen = abs(grain.pos.x + grain.pos.y - sheenProgress * 2f) / 2f
                    val influence = (1f - (distToSheen * 1.5f)).coerceIn(0f, 1f).pow(5)
                    val intensity = influence * grain.reflectivity

                    // Masking Logic (Artwork area approx center-top)
                    val isInArtArea = grain.pos.x in 0.15f..0.85f && grain.pos.y in 0.12f..0.62f
                    val shouldShowGrain =
                        if (isReverseHolo) !isInArtArea else (isFullArt || isInArtArea)

                    if (intensity > 0.01f && shouldShowGrain) {
                        val colorPos = (grain.pos.x + grain.pos.y + sheenProgress) % 1f
                        val grainColor = interpolateRainbow(
                            rainbowColors,
                            colorPos
                        ).copy(alpha = intensity * 0.8f)

                        when (grain.patternType) {
                            GrainType.DOT -> {
                                drawCircle(
                                    color = grainColor,
                                    radius = grain.size.dp.toPx(),
                                    center = Offset(px, py),
                                    blendMode = BlendMode.Screen
                                )
                            }

                            GrainType.GLOW_DOT -> {
                                drawCircle(
                                    color = grainColor,
                                    radius = grain.size.dp.toPx() * 1.5f,
                                    center = Offset(px, py),
                                    blendMode = BlendMode.Screen
                                )
                                drawCircle(
                                    color = grainColor.copy(alpha = intensity * 0.3f),
                                    radius = grain.size.dp.toPx() * 4f,
                                    center = Offset(px, py),
                                    blendMode = BlendMode.Screen
                                )
                            }

                            GrainType.STAR -> {
                                val starSize = grain.size.dp.toPx() * 6f * intensity
                                if (starSize > 0f) {
                                    withTransform({
                                        translate(px, py)
                                        rotate(effX * 2f + effY * 2f + grain.pos.x * 360f)
                                    }) {
                                        drawLine(
                                            color = grainColor,
                                            start = Offset(-starSize, 0f),
                                            end = Offset(starSize, 0f),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        drawLine(
                                            color = grainColor,
                                            start = Offset(0f, -starSize),
                                            end = Offset(0f, starSize),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun interpolateRainbow(colors: List<Color>, progress: Float): Color {
    val p = progress.coerceIn(0f, 1f) * (colors.size - 1)
    val i = p.toInt()
    val t = p - i
    val c1 = colors[i]
    val c2 = colors[(i + 1) % colors.size]
    return Color(
        red = c1.red + (c2.red - c1.red) * t,
        green = c1.green + (c2.green - c1.green) * t,
        blue = c1.blue + (c2.blue - c1.blue) * t
    )
}

private enum class GrainType { DOT, GLOW_DOT, STAR }

private data class HoloGrain(
    val pos: Offset,
    val size: Float,
    val reflectivity: Float,
    val parallaxFactor: Float,
    val patternType: GrainType
)

/**
 * LEGACY / COMPATIBILITY WRAPPERS
 */
fun Modifier.shimmerEffect(
    show: Boolean = true,
    cornerRadius: Dp = 12.dp,
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

    this
        .graphicsLayer(clip = false)
        .drawWithContent {
            drawContent()
            val cardPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, size.width, size.height),
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                )
            }
            clipPath(cardPath) {
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
}

fun Modifier.energyEffect(
    type: String?,
    show: Boolean = true
): Modifier = composed {
    if (!show || type == null) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "energy")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "energy_progress"
    )

    val normalizedType = type.lowercase()
    val reusablePath = remember { Path() }

    this
        .graphicsLayer(clip = false)
        .drawWithContent {
            drawContent()
            if (size.width <= 0f || size.height <= 0f) return@drawWithContent

            when (normalizedType) {
                "fire" -> drawEmbers(progress)
                "grass" -> drawForestSpirit(progress, reusablePath)
                "water" -> drawBubbles(progress)
                "lightning", "electric" -> drawLightningStorm(progress, reusablePath)
                "fighting" -> drawImpactShockwave(progress)
                "darkness", "dark" -> drawDarkAura(progress)
                "psychic" -> drawTelekinesisRipples(progress)
                "fairy" -> drawEnchantedGlitter(progress)
                "dragon" -> drawDragonBreath(progress)
                "colorless" -> drawGustsOfWind(progress, reusablePath)
            }
        }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmbers(progress: Float) {
    val random = Random(42)
    repeat(30) { i ->
        val phase = (progress + i / 30f) % 1f
        val speedFactor = 0.7f + (random.nextFloat() * 0.6f)
        val actualPhase = (phase * speedFactor) % 1f
        
        val x = (random.nextFloat() * size.width) + sin(actualPhase * 8f * PI.toFloat()) * 12f
        val y = size.height * (1f - actualPhase)
        val alpha = sin(actualPhase * PI.toFloat())
        val sizePx = (1f + random.nextFloat() * 3f).dp.toPx()

        val emberColor = when(i % 4) {
            0 -> Color(0xFFFF3D00)
            1 -> Color(0xFFFF9100)
            2 -> Color(0xFFFFC400)
            else -> Color(0xFFFFEA00)
        }
        
        drawCircle(color = emberColor.copy(alpha = alpha * 0.9f), radius = sizePx, center = Offset(x, y), blendMode = BlendMode.Screen)
        if (alpha > 0.4f) {
            drawCircle(color = emberColor.copy(alpha = alpha * 0.25f), radius = sizePx * 5f, center = Offset(x, y), blendMode = BlendMode.Screen)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForestSpirit(progress: Float, path: Path) {
    repeat(3) { i ->
        val phase = (progress + i / 3f) % 1f
        val xOffset = sin(phase * 2f * PI.toFloat()) * 50.dp.toPx()
        val yOffset = cos(phase * 2f * PI.toFloat()) * 30.dp.toPx()
        val radius = (size.width * 0.8f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color(0xFFC8E6C9).copy(alpha = 0.15f), Color.Transparent), center = Offset(size.width / 2 + xOffset, size.height / 4 + yOffset), radius = radius),
            radius = radius, center = Offset(size.width / 2 + xOffset, size.height / 4 + yOffset)
        )
    }
    val sporeRandom = Random(43)
    repeat(25) { i ->
        val speed = 0.3f + sporeRandom.nextFloat() * 0.4f
        val particleProgress = (progress * speed + (i.toFloat() / 25f)) % 1f
        val startX = sporeRandom.nextFloat() * size.width
        val x = startX + sin(particleProgress * 4f * PI.toFloat()) * 30.dp.toPx()
        val y = size.height * (1.1f - particleProgress * 1.2f)
        val alpha = sin(particleProgress * PI.toFloat())
        drawCircle(color = Color(0xFFE8F5E9).copy(alpha = alpha * 0.5f), radius = (1f + sporeRandom.nextFloat() * 2f).dp.toPx(), center = Offset(x, y), blendMode = BlendMode.Plus)
    }
    repeat(4) { i ->
        val petalRandom = Random(100L + i)
        val petalProgress = (progress * 0.15f + (i.toFloat() / 4f)) % 1f
        val startX = petalRandom.nextFloat() * size.width
        val x = startX + sin(petalProgress * 2f * PI.toFloat()) * 60.dp.toPx()
        val y = -50.dp.toPx() + (size.height + 100.dp.toPx()) * petalProgress
        val alpha = sin(petalProgress * PI.toFloat())
        val rotation = petalProgress * 360f + (i * 90f)
        withTransform({
            translate(x, y)
            rotate(rotation)
            scale(abs(sin(petalProgress * 10f)).coerceAtLeast(0.3f), 1f)
        }) {
            val pw = 10.dp.toPx()
            val ph = 14.dp.toPx()
            path.reset()
            path.moveTo(0f, -ph / 2)
            path.quadraticTo(pw / 2, 0f, 0f, ph / 2)
            path.quadraticTo(-pw / 2, 0f, 0f, -ph / 2)
            path.close()
            drawPath(path, color = Color(0xFF81C784).copy(alpha = alpha * 0.4f))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBubbles(progress: Float) {
    val random = Random(44)
    repeat(20) { i ->
        val phase = (progress + i / 20f) % 1f
        val x = random.nextFloat() * size.width
        val y = size.height * (1f - phase)
        val alpha = sin(phase * PI.toFloat())
        val radius = (4f + random.nextFloat() * 8f).dp.toPx()
        drawCircle(color = Color.White.copy(alpha = alpha * 0.3f), radius = radius, center = Offset(x, y), style = Stroke(width = 1.dp.toPx()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightningStorm(progress: Float, path: Path) {
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
            drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFFE1F5FE).copy(alpha = flashAlpha), Color.Transparent), startY = 0f, endY = topBound), blendMode = BlendMode.Screen)
            path.reset()
            path.moveTo(startX, 0f)
            var currX = startX
            var currY = 0f
            while (currY < topBound * 0.8f) {
                currX += (pulseRandom.nextFloat() - 0.5f) * 160.dp.toPx()
                currY += pulseRandom.nextFloat() * 80.dp.toPx()
                path.lineTo(currX, currY)
                if (pulseRandom.nextFloat() > 0.65f) {
                    val bx = currX + (pulseRandom.nextFloat() - 0.5f) * 90.dp.toPx()
                    val by = currY + pulseRandom.nextFloat() * 45.dp.toPx()
                    path.moveTo(currX, currY)
                    path.lineTo(bx, by)
                    path.moveTo(currX, currY)
                }
            }
            drawPath(path = path, color = Color.White.copy(alpha = flashAlpha * 3f), style = Stroke(width = 1.3.dp.toPx()), blendMode = BlendMode.Screen)
            drawPath(path = path, color = Color(0xFF03A9F4).copy(alpha = flashAlpha), style = Stroke(width = 5.dp.toPx()), blendMode = BlendMode.Screen)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawImpactShockwave(progress: Float) {
    val eventIndex = (progress * 8).toInt()
    val random = Random(eventIndex)
    if (random.nextFloat() > 0.7f) {
        val eventProgress = (progress * 8 % 1f)
        val alpha = (1f - eventProgress).pow(2)
        val centerX = random.nextFloat() * size.width
        val centerY = random.nextFloat() * size.height * 0.6f
        val radius = eventProgress * size.width * 0.4f
        drawCircle(color = Color.White.copy(alpha = alpha * 0.2f), radius = radius, center = Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
        repeat(5) { j ->
            val pRandom = Random(eventIndex + j)
            val angle = pRandom.nextFloat() * 2f * PI.toFloat()
            val dist = radius + pRandom.nextFloat() * 20.dp.toPx()
            drawCircle(color = Color(0xFFD7CCC8).copy(alpha = alpha * 0.4f), radius = 2.dp.toPx(), center = Offset(centerX + cos(angle) * dist, centerY + sin(angle) * dist))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDarkAura(progress: Float) {
    repeat(4) { i ->
        val auraProgress = (progress + i / 4f) % 1f
        val alpha = sin(auraProgress * PI.toFloat()) * 0.3f
        val radius = (0.5f + auraProgress * 0.5f) * size.width
        if (radius > 0f) {
            drawCircle(brush = Brush.radialGradient(colors = listOf(Color.Black.copy(alpha = alpha), Color.Transparent), center = Offset(size.width / 2, size.height * 0.3f), radius = radius), radius = radius, center = Offset(size.width / 2, size.height * 0.3f), blendMode = BlendMode.Multiply)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTelekinesisRipples(progress: Float) {
    repeat(3) { i ->
        val rp = (progress + i / 3f) % 1f
        val r = rp * size.width
        val a = (1f - rp) * 0.5f
        drawCircle(color = Color(0xFFE1BEE7).copy(alpha = a), radius = r, center = Offset(size.width / 2, size.height / 2), style = Stroke(width = 2.dp.toPx()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnchantedGlitter(progress: Float) {
    val random = Random(49)
    repeat(20) { i ->
        val pp = (progress + i / 20f) % 1f
        val a = sin(pp * PI.toFloat())
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        drawCircle(color = Color(0xFFF48FB1).copy(alpha = a * 0.6f), radius = 3.dp.toPx(), center = Offset(x, y))
        if (a > 0.7f) {
            val cs = 6.dp.toPx() * a
            drawLine(Color.White.copy(alpha = a), Offset(x - cs, y), Offset(x + cs, y), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = a), Offset(x, y - cs), Offset(x, y + cs), strokeWidth = 1.dp.toPx())
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDragonBreath(progress: Float) {
    val wave = progress * 2f * PI.toFloat()
    drawRect(brush = Brush.verticalGradient(0.0f to Color(0xFF673AB7).copy(alpha = 0.1f + sin(wave) * 0.1f), 0.5f to Color(0xFF311B92).copy(alpha = 0.2f + cos(wave) * 0.1f), 1.0f to Color.Transparent))
    val random = Random(50)
    repeat(5) { i ->
        val wp = (progress * 0.6f + i / 5f) % 1f
        val x = (random.nextFloat() * size.width) + sin(wp * 4f * PI.toFloat()) * 20.dp.toPx()
        val y = size.height * (1f - wp)
        val radius = 60.dp.toPx()
        if (radius > 0f) {
            drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF7E57C2).copy(alpha = 0.2f * sin(wp * PI.toFloat())), Color.Transparent), center = Offset(x, y), radius = radius), radius = radius, center = Offset(x, y))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGustsOfWind(progress: Float, path: Path) {
    val random = Random(47)
    repeat(5) { i ->
        val phase = (progress + i / 5f) % 1f
        val x = phase * size.width * 2.5f - size.width * 0.75f
        val y = random.nextFloat() * size.height
        val alpha = (1f - abs(phase - 0.5f) * 2).pow(2) * 0.2f
        path.reset()
        path.moveTo(x, y)
        path.cubicTo(x + size.width * 0.2f, y - 40.dp.toPx(), x + size.width * 0.4f, y + 40.dp.toPx(), x + size.width * 0.6f, y)
        drawPath(path = path, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)), start = Offset(x, y), end = Offset(x + size.width * 0.6f, y)), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
        path.reset()
        path.moveTo(x - 30.dp.toPx(), y + 15.dp.toPx())
        path.cubicTo(x + size.width * 0.15f, y - 25.dp.toPx(), x + size.width * 0.35f, y + 55.dp.toPx(), x + size.width * 0.55f, y + 15.dp.toPx())
        drawPath(path = path, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha * 0.5f), Color.White.copy(alpha = 0f)), start = Offset(x - 30.dp.toPx(), y + 15.dp.toPx()), end = Offset(x + size.width * 0.55f, y + 15.dp.toPx())), style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun MicroCaptureFanfare(
    center: Offset,
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val random = Random(System.currentTimeMillis())
        List(30) {
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 400f + 100f
            Offset(cos(angle) * speed, sin(angle) * speed)
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onAnimationFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val t = progress.value

                // 1. Concentric Expanding Rings
                val ringCount = 2
                repeat(ringCount) { i ->
                    val ringT = (t * 1.5f - (i * 0.2f)).coerceIn(0f, 1f)
                    if (ringT > 0f) {
                        val radius = ringT * 80.dp.toPx()
                        val alpha = (1f - ringT).pow(2.5f)
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.8f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // 2. Radiating Multicolor Sparkles
                particles.forEachIndexed { index, velocity ->
                    val particleT = (t * 1.2f).coerceIn(0f, 1f)
                    if (particleT > 0f) {
                        val pos = center + velocity * particleT
                        val alpha = (1f - particleT).pow(2)
                        val particleSize = (1f - particleT) * 4.dp.toPx()

                        val color = when (index % 5) {
                            0 -> Color(0xFF00E676) // Green
                            1 -> Color(0xFFFFD600) // Gold
                            2 -> Color(0xFF00B0FF) // Cyan
                            3 -> Color(0xFFFF4081) // Pink
                            else -> Color.White
                        }

                        drawCircle(
                            color = color.copy(alpha = alpha),
                            radius = particleSize,
                            center = pos
                        )
                    }
                }
            }
    ) {
        val t = progress.value

        val ballScale = when {
            t < 0.2f -> (t / 0.2f) * 1.05f
            t < 0.4f -> 1.05f + sin((t - 0.2f) * 30f) * 0.02f
            else -> 1.05f
        }

        val ballAlpha = when {
            t < 0.1f -> t / 0.1f
            t < 0.7f -> 1f
            else -> 1f - ((t - 0.7f) / 0.3f)
        }

        if (ballAlpha > 0f) {
            Icon(
                Icons.Rounded.CatchingPokemon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        translationX = center.x - size.width / 2
                        translationY = center.y - size.height / 2
                        scaleX = ballScale
                        scaleY = ballScale
                        alpha = ballAlpha
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, name = "Micro Capture Fanfare Preview")
@Composable
private fun MicroCaptureFanfarePreview() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MicroCaptureFanfare(
            center = Offset(500f, 500f)
        )
    }
}

@Preview(showBackground = true, name = "Holo Effect Preview")
@Composable
private fun HoloEffectPreview() {
    Box(
        modifier = Modifier
            .padding(32.dp)
            .size(width = 200.dp, height = 280.dp)
            .holoEffect(finish = PricingUtils.FINISH_HOLOFOIL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Holo Card")
            Text("(Drag to tilt)", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true, name = "Energy Effect Preview")
@Composable
private fun EnergyEffectPreview() {
    Box(
        modifier = Modifier
            .padding(32.dp)
            .size(width = 200.dp, height = 280.dp)
            .energyEffect(type = "fire"),
        contentAlignment = Alignment.Center
    ) {
        Text("Fire Energy")
    }
}
