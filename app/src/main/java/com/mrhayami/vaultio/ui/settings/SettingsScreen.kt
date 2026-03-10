package com.mrhayami.vaultio.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrhayami.vaultio.data.repository.VaultioRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: VaultioRepository,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(uiState.theme) },
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                modifier = Modifier.clickable { /* TODO: Show theme picker */ }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Market Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("JustTCG API Usage") },
                supportingContent = { 
                    Column {
                        LinearProgressIndicator(
                            progress = { uiState.apiUsage / 100f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        Text("${uiState.apiUsage} / 100 requests used today")
                    }
                },
                leadingContent = { Icon(Icons.Rounded.Api, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Clear Image Cache") },
                supportingContent = { Text("Free up space on your device") },
                leadingContent = { Icon(Icons.Rounded.Storage, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.clearImageCache() }
            )

            ListItem(
                headlineContent = { Text("${uiState.offlineSetsCount} sets downloaded") },
                leadingContent = { Icon(Icons.Rounded.Download, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("App Version") },
                supportingContent = { Text("1.0.0 (Debug)") },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) }
            )

            TextButton(
                onClick = { viewModel.resetSettings() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reset All Settings")
            }
        }
    }
}
