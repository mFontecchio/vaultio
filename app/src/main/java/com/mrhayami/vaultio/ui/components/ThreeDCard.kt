package com.mrhayami.vaultio.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ThreeDCard(
    modifier: Modifier = Modifier,
    content: @Composable (rotationX: Float, rotationY: Float) -> Unit
) {
    val context = LocalContext.current
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Gyroscope/Rotation Vector Sensor Logic
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    // orientation[1] is pitch (rotation around X axis)
                    // orientation[2] is roll (rotation around Y axis)
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    
                    // Constrain to reasonable viewing angles
                    rotationX = -pitch.coerceIn(-30f, 30f)
                    rotationY = roll.coerceIn(-30f, 30f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
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
                    if (zoom != 1f) {
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                    }
                    
                    if (scale > 1.1f) {
                        offsetX += pan.x * scale
                        offsetY += pan.y * scale
                    } else {
                        // Manual touch can still influence rotation if needed, but gyro is primary
                        rotationY += pan.x / 8f
                        rotationX -= pan.y / 8f
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
                cameraDistance = 15f * density
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(0.718f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 32.dp,
            color = Color.Transparent
        ) {
            content(animatedRotationX, animatedRotationY)
        }
    }
}
