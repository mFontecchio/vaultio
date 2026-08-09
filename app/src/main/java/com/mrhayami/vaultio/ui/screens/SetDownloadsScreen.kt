package com.mrhayami.vaultio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.ui.components.ConfirmDestructiveDialog
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDownloadsScreen(
    viewModel: SetDownloadsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.sideEffects) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SetDownloadsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Set Downloads")
                        Text(
                            String.format(Locale.getDefault(), "~%.2f MB used", state.estimatedSizeMB),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (state.isRefreshing || state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.onEvent(SetDownloadsEvent.RefreshSets) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Sets")
                        }
                        if (state.downloadedSets.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllConfirm = true }) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    contentDescription = "Delete all downloaded sets",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Button(
                onClick = { viewModel.onEvent(SetDownloadsEvent.DownloadAll) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = state.remainingSets.isNotEmpty() && !state.isLoading
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download All")
            }

            // Storage Visualization
            LinearProgressIndicator(
                progress = { state.downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(8.dp)
                    .clip(CircleShape),
            )
            Text(
                text = "${state.downloadedSets.size} of ${state.sets.size} sets available offline",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            if (state.sets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = { viewModel.onEvent(SetDownloadsEvent.RefreshSets) }) {
                            Text("Fetch Sets")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.navigationBarsPadding()) {
                    items(state.sets, key = { it.id }) { set ->
                        SetItem(
                            set = set,
                            onDownload = { viewModel.onEvent(SetDownloadsEvent.DownloadSet(set.id)) },
                            onDelete = { viewModel.onEvent(SetDownloadsEvent.DeleteSet(set.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllConfirm) {
        ConfirmDestructiveDialog(
            title = "Delete all downloaded sets?",
            message = "This removes offline set data from this device. You can download sets again later.",
            confirmLabel = "Delete all",
            onConfirm = { viewModel.onEvent(SetDownloadsEvent.DeleteAll) },
            onDismiss = { showDeleteAllConfirm = false }
        )
    }
}

@Composable
fun SetItem(
    set: SetEntity,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    // TCGDex predictable asset URL fallback - Always prefer logo for Set Downloads view
    val imageUrl = remember(set.logo, set.symbol, set.id) {
        set.logo ?: set.symbol ?: "https://assets.tcgdex.net/en/sets/${set.id}/logo.png"
    }

    ListItem(
        headlineContent = { 
            Text(set.name, fontWeight = FontWeight.SemiBold) 
        },
        supportingContent = {
            Column {
                Text("${set.series ?: "Standard"} • ${set.totalCards} cards")
                set.releaseDate?.let {
                    Text("Released: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${set.name} logo",
                    modifier = Modifier.padding(4.dp),
                    error = rememberVectorPainter(Icons.Rounded.ImageNotSupported)
                )
            }
        },
        trailingContent = {
            if (set.isDownloaded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle, 
                        contentDescription = "Downloaded", 
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.DeleteOutline, 
                            contentDescription = "Delete from device",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Rounded.CloudDownload, 
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

@Composable
fun rememberVectorPainter(image: androidx.compose.ui.graphics.vector.ImageVector) = 
    androidx.compose.ui.graphics.vector.rememberVectorPainter(image)

@VaultioPreviews
@Composable
private fun SetItemNotDownloadedPreview() {
    VaultioPreview {
        SetItem(
            set = SetEntity(
                id = "swsh4",
                name = "Vivid Voltage",
                series = "Sword & Shield",
                logo = null,
                symbol = null,
                totalCards = 185,
                releaseDate = "2020/11/13",
                isDownloaded = false,
            ),
            onDownload = {},
            onDelete = {},
        )
    }
}

@VaultioPreviews
@Composable
private fun SetItemDownloadedPreview() {
    VaultioPreview {
        SetItem(
            set = SetEntity(
                id = "swsh4",
                name = "Vivid Voltage",
                series = "Sword & Shield",
                logo = null,
                symbol = null,
                totalCards = 185,
                releaseDate = "2020/11/13",
                isDownloaded = true,
            ),
            onDownload = {},
            onDelete = {},
        )
    }
}
