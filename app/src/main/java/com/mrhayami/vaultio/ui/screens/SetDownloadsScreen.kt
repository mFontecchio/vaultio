package com.mrhayami.vaultio.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDownloadsScreen(repository: VaultioRepository) {
    val sets by repository.allSets.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    val downloadedSets = sets.filter { it.isDownloaded }
    val remainingSets = sets.filter { !it.isDownloaded }
    
    // Estimate storage (very rough estimate: ~1.5KB per card entity)
    val totalCardsDownloaded = downloadedSets.sumOf { it.totalCards }
    val estimatedSizeMB = (totalCardsDownloaded * 1.5 / 1024.0)

    // Auto-refresh if we have sets but they are missing logos (stale data fix)
    LaunchedEffect(sets) {
        if (sets.isNotEmpty() && sets.take(10).all { it.logo == null || !it.logo.contains("http") }) {
            repository.refreshSets()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Set Downloads")
                        Text(
                            String.format(Locale.getDefault(), "~%.2f MB used", estimatedSizeMB),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                refreshing = true
                                repository.refreshSets()
                                refreshing = false
                            }
                        }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Sets")
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
            // Bulk Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            remainingSets.forEach { repository.downloadSet(it.id) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = remainingSets.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Download All", fontSize = 12.sp)
                }
                
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            downloadedSets.forEach { repository.deleteDownloadedSet(it.id) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = downloadedSets.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete All", fontSize = 12.sp)
                }
            }

            // Storage Visualization
            LinearProgressIndicator(
                progress = { (downloadedSets.size.toFloat() / sets.size.coerceAtLeast(1).toFloat()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(8.dp)
                    .clip(CircleShape),
            )
            Text(
                text = "${downloadedSets.size} of ${sets.size} sets available offline",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            if (sets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (refreshing) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = {
                            scope.launch {
                                refreshing = true
                                repository.refreshSets()
                                refreshing = false
                            }
                        }) {
                            Text("Fetch Sets")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.navigationBarsPadding()) {
                    items(sets, key = { it.id }) { set ->
                        SetItem(
                            set = set,
                            onDownload = {
                                scope.launch {
                                    repository.downloadSet(set.id)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    repository.deleteDownloadedSet(set.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetItem(
    set: SetEntity,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    // TCGDex predictable asset URL fallback
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
                    contentDescription = null,
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
                        tint = Color(0xFF4CAF50),
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
