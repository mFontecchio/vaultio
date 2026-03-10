package com.mrhayami.vaultio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDownloadsScreen(repository: VaultioRepository) {
    val sets by repository.allSets.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Downloads") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            refreshing = true
                            repository.refreshSets()
                            refreshing = false
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Sets")
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
                    items(sets) { set ->
                        SetItem(set = set, onDownload = {
                            scope.launch {
                                repository.downloadSet(set.id)
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SetItem(set: SetEntity, onDownload: () -> Unit) {
    ListItem(
        headlineContent = { Text(set.name) },
        supportingContent = { Text("${set.series ?: ""} • ${set.totalCards} cards") },
        leadingContent = {
            AsyncImage(
                model = set.logo,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        trailingContent = {
            if (set.isDownloaded) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
            } else {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Rounded.Download, contentDescription = "Download")
                }
            }
        }
    )
}
