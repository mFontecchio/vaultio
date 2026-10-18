package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

/**
 * A modifier that applies a staggered entrance animation (fade and slide/scale).
 * Centralized to ensure consistent performance and feel across List, Grid, and Pokedex views.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    initialDelay: Long = 0,
    staggerDelay: Long = 25,
    indexModulo: Int = 16,
    type: EntranceType = EntranceType.SlideUp,
    enabled: Boolean = true
): Modifier = composed {
    val isInspectionMode = LocalInspectionMode.current
    if (isInspectionMode || !enabled) return@composed this

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(enabled) {
        if (enabled) {
            val delayTime = initialDelay + (index % indexModulo) * staggerDelay
            kotlinx.coroutines.delay(delayTime)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
        }
    }

    this.graphicsLayer {
        alpha = animatable.value
        when (type) {
            EntranceType.SlideUp -> {
                translationY = (1f - animatable.value) * 30.dp.toPx()
            }

            EntranceType.SlideIn -> {
                translationX = (1f - animatable.value) * -30.dp.toPx()
            }

            EntranceType.ScaleUp -> {
                val scale = 0.9f + (animatable.value * 0.1f)
                scaleX = scale
                scaleY = scale
            }
        }
    }
}

fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "bouncyClickScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

enum class EntranceType {
    SlideUp,
    SlideIn,
    ScaleUp
}

@Composable
fun AnimatedCounter(
    value: Double,
    modifier: Modifier = Modifier,
    prefix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = Color.Unspecified,
    formatter: (Double) -> String = { NumberFormat.getCurrencyInstance(Locale.US).format(it) }
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = prefix + formatter(animated.value.toDouble()),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
fun AnimatedIntCounter(
    value: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = "${animated.value.toInt()}$suffix",
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
fun CardFlipContainer(
    flipped: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "cardFlipRotation"
    )
    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 16f * density
        }
    ) {
        if (rotation <= 90f) {
            front()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                back()
            }
        }
    }
}

@Composable
fun PokemonCardBack(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFC62828), Color(0xFF8E0000))
                )
            )
            .border(6.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD54F)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1565C0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "V",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            }
        }
    }
}

@Composable
fun TargetLockReticle(
    locked: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00E676)
) {
    val progress by animateFloatAsState(
        targetValue = if (locked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lockReticle"
    )
    val pulse = rememberInfiniteTransition(label = "lockPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lockPulseAlpha"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        if (progress <= 0.01f) return@Canvas
        val inset = 28.dp.toPx() * (1.4f - 0.4f * progress)
        val arm = 28.dp.toPx() * progress
        val stroke = 3.dp.toPx()
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        val c = color.copy(alpha = if (locked) pulseAlpha else progress)
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(c, Offset(x, y), Offset(x + dx, y), stroke, StrokeCap.Round)
            drawLine(c, Offset(x, y), Offset(x, y + dy), stroke, StrokeCap.Round)
        }
        corner(left, top, arm, arm)
        corner(right, top, -arm, arm)
        corner(left, bottom, arm, -arm)
        corner(right, bottom, -arm, -arm)
    }
}

@Composable
fun PriceSparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 28.dp
) {
    if (values.size < 2) return
    val min = values.minOrNull() ?: return
    val max = values.maxOrNull() ?: return
    val range = (max - min).takeIf { it > 0.0001 } ?: 1.0
    Canvas(modifier = modifier.height(height)) {
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.lastIndex).coerceAtLeast(1)
            val y = size.height - ((value - min) / range).toFloat() * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun GradingRadarOverlay(
    active: Boolean,
    modifier: Modifier = Modifier,
    statusText: String = "Analyzing surface…"
) {
    if (!active) return
    val transition = rememberInfiniteTransition(label = "gradingRadar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweep"
    )
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * sweep
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00E676).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = y - 24.dp.toPx(),
                    endY = y + 24.dp.toPx()
                )
            )
            val arm = min(size.width, size.height) * 0.08f
            val c = Color(0xFFFFD54F)
            drawLine(c, Offset(12.dp.toPx(), 12.dp.toPx()), Offset(12.dp.toPx() + arm, 12.dp.toPx()), 3.dp.toPx())
            drawLine(c, Offset(12.dp.toPx(), 12.dp.toPx()), Offset(12.dp.toPx(), 12.dp.toPx() + arm), 3.dp.toPx())
            drawLine(
                c,
                Offset(size.width - 12.dp.toPx(), 12.dp.toPx()),
                Offset(size.width - 12.dp.toPx() - arm, 12.dp.toPx()),
                3.dp.toPx()
            )
            drawLine(
                c,
                Offset(size.width - 12.dp.toPx(), 12.dp.toPx()),
                Offset(size.width - 12.dp.toPx(), 12.dp.toPx() + arm),
                3.dp.toPx()
            )
        }
        Text(
            statusText,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .then(Modifier)
        )
    }
}

@Composable
fun FloatingPokeBall(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pokeballFloat")
    val offset by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pokeballY"
    )
    Icon(
        Icons.Rounded.CatchingPokemon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = modifier
            .size(72.dp)
            .graphicsLayer { translationY = offset }
    )
}

@Composable
fun SlabHologramOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "slabHolo")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slabShift"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val x = size.width * shift
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFD700).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                start = Offset(x - 40.dp.toPx(), 0f),
                end = Offset(x + 40.dp.toPx(), size.height)
            )
        )
    }
}

@VaultioPreviews
@Composable
private fun AnimatedCounterPreview() {
    VaultioPreview {
        Column {
            AnimatedCounter(value = 1240.55)
            Spacer(Modifier.height(8.dp))
            PokemonCardBack(Modifier.size(120.dp).aspectRatio(0.718f))
            FloatingPokeBall()
        }
    }
}
