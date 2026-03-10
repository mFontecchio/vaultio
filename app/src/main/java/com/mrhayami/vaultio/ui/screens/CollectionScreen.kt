package com.mrhayami.vaultio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mrhayami.vaultio.data.repository.VaultioRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(repository: VaultioRepository) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Collection") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Collection Screen (Coming Soon)")
        }
    }
}
