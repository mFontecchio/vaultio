package com.mrhayami.vaultio.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews

private const val FabAnimMs = 220
private const val StaggerMs = 60

/**
 * Screen-level speed dial for manual add and scanner entry.
 * Used on Collection and Wishlist (not on the root Scaffold).
 */
@Composable
fun AddScanFabMenu(
    onAddCard: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
    addLabel: String = "Add",
) {
    var expanded by remember { mutableStateOf(false) }
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = FabAnimMs),
        label = "fabIconRotation"
    )

    BackHandler(enabled = expanded) {
        expanded = false
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Farthest from FAB: enters last, exits first
        SpeedDialAction(
            visible = expanded,
            enterDelayMs = StaggerMs,
            exitDelayMs = 0,
            onClick = {
                expanded = false
                onAddCard()
            },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            label = addLabel,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        // Closest to FAB: enters first, exits last
        SpeedDialAction(
            visible = expanded,
            enterDelayMs = 0,
            exitDelayMs = StaggerMs,
            onClick = {
                expanded = false
                onScan()
            },
            icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
            label = "Scan",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (expanded) "Close menu" else "Add or scan",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(iconRotation)
            )
        }
    }
}

@Composable
private fun SpeedDialAction(
    visible: Boolean,
    enterDelayMs: Int,
    exitDelayMs: Int,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(FabAnimMs, delayMillis = enterDelayMs)) +
            scaleIn(
                animationSpec = tween(FabAnimMs, delayMillis = enterDelayMs),
                initialScale = 0.85f
            ) +
            slideInVertically(
                animationSpec = tween(FabAnimMs, delayMillis = enterDelayMs)
            ) { fullHeight -> fullHeight / 2 },
        exit = fadeOut(tween(FabAnimMs - 40, delayMillis = exitDelayMs)) +
            scaleOut(
                animationSpec = tween(FabAnimMs - 40, delayMillis = exitDelayMs),
                targetScale = 0.85f
            ) +
            slideOutVertically(
                animationSpec = tween(FabAnimMs - 40, delayMillis = exitDelayMs)
            ) { fullHeight -> fullHeight / 2 }
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = icon,
            text = { Text(label) },
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        )
    }
}

@VaultioPreviews
@Composable
private fun AddScanFabMenuPreview() {
    VaultioPreview {
        AddScanFabMenu(onAddCard = {}, onScan = {})
    }
}
