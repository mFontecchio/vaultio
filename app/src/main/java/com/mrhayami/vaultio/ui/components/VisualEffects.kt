package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
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
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = colors,
                start = Offset(translateAnim, translateAnim),
                end = Offset(translateAnim + 500f, translateAnim + 500f)
            ),
            blendMode = BlendMode.SrcOver
        )
    }
}

/**
 * A highly optimized sparkle effect that uses a single animation state
 * and quantized cycles to ensure a perfectly seamless loop without stuttering.
 */
fun Modifier.sparkleEffect(
    show: Boolean = true,
    sparkleCount: Int = 70 // Reduced count for better performance
): Modifier = composed {
    if (!show) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    
    // 12-second loop for a natural rhythm
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
                // Fewer cycles over 12 seconds makes it slower and more natural
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
            // Sinusoidal alpha with seamless landing
            val angle = progress * pi2 * prop.blinkCycles + prop.phase
            val alpha = (sin(angle) * 0.5f + 0.5f).pow(3) // Slightly lower power for softer fades
            
            if (alpha > 0.05f) {
                val x = prop.pos.x * width
                val y = prop.pos.y * height
                val sizePx = prop.baseSize.dp.toPx() * (0.6f + alpha * 0.4f)
                
                // Draw point
                drawCircle(
                    color = prop.color.copy(alpha = alpha * 0.8f),
                    radius = sizePx,
                    center = Offset(x, y)
                )
                
                // Draw glow
                drawCircle(
                    color = prop.color.copy(alpha = alpha * 0.15f),
                    radius = sizePx * 4.5f,
                    center = Offset(x, y)
                )

                // Diamond Highlight
                if (prop.hasHighlight && alpha > 0.65f) {
                    val flareAlpha = (alpha - 0.65f) / 0.35f
                    val flareSize = sizePx * 5.5f
                    
                    withTransform({
                        translate(x, y)
                        // Rotation is also seamless
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
