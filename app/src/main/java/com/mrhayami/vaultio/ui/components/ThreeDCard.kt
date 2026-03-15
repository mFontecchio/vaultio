package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ThreeDCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotationY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Rotate when dragging, but if zoomed in, maybe pan?
                    // Let's use pan for rotation when scale is near 1, and for offset when zoomed?
                    // Or just use pan for rotation and let the user "shift" it via offset.
                    // Actually, the prompt says "shifted and turned".
                    // Let's use two-finger pan for offset and one-finger for rotation? 
                    // detectTransformGestures pan is the change in the centroid.
                    
                    if (zoom != 1f) {
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                    }
                    
                    // Simple logic: dragging turns the card. 
                    // To "shift" (pan), maybe we can use a different gesture or just combine them.
                    // Let's make rotation more sensitive and add translation.
                    rotationY += pan.x / 2f
                    rotationX -= pan.y / 2f
                    
                    if (scale > 1.1f) {
                        offsetX += pan.x * scale
                        offsetY += pan.y * scale
                    }
                }
            }
            .graphicsLayer {
                rotationX = animatedRotationX
                rotationY = animatedRotationY
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = offsetX
                translationY = offsetY
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(0.718f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 24.dp,
            color = Color.Transparent
        ) {
            content()
        }
    }
}
