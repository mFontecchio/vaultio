package com.mrhayami.vaultio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    primaryLabel: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (primaryLabel != null && onPrimaryClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onPrimaryClick) {
                Text(primaryLabel)
            }
        }
        if (secondaryLabel != null && onSecondaryClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondaryClick) {
                Text(secondaryLabel)
            }
        }
    }
}

@VaultioPreviews
@Composable
private fun EmptyStateWithActionsPreview() {
    VaultioPreview {
        EmptyState(
            title = "Your collection is empty",
            message = "Scan cards or add them manually to start building your collection.",
            icon = Icons.Rounded.QrCodeScanner,
            primaryLabel = "Scan cards",
            onPrimaryClick = {},
            secondaryLabel = "Add manually",
            onSecondaryClick = {},
        )
    }
}

@VaultioPreviews
@Composable
private fun EmptyStateMessageOnlyPreview() {
    VaultioPreview {
        EmptyState(
            title = "Nothing here yet",
            message = "Check back later for updates.",
            icon = Icons.Rounded.Info,
        )
    }
}
