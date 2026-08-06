package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
            delay(delayTime)
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

enum class EntranceType {
    SlideUp,
    SlideIn,
    ScaleUp
}
