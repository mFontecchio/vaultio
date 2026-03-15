package com.mrhayami.vaultio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrhayami.vaultio.BuildConfig
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: VaultioRepository,
    userPreferencesRepository: UserPreferencesRepository,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository, userPreferencesRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeBrandDialog by remember { mutableStateOf(false) }
    var showDarkConfigDialog by remember { mutableStateOf(false) }

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
                headlineContent = { Text("Theme Color") },
                supportingContent = { 
                    Text(when(uiState.themeBrand) {
                        ThemeBrand.DEFAULT -> "System Default (Material You)"
                        ThemeBrand.GRASS -> "Grass Energy"
                        ThemeBrand.FIRE -> "Fire Energy"
                        ThemeBrand.WATER -> "Water Energy"
                        ThemeBrand.ELECTRIC -> "Lightning Energy"
                        ThemeBrand.PSYCHIC -> "Psychic Energy"
                        ThemeBrand.FIGHTING -> "Fighting Energy"
                        ThemeBrand.DARKNESS -> "Darkness Energy"
                        ThemeBrand.STEEL -> "Metal Energy"
                        ThemeBrand.FAIRY -> "Fairy Energy"
                        ThemeBrand.DRAGON -> "Dragon Energy"
                    }) 
                },
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                modifier = Modifier.clickable { showThemeBrandDialog = true }
            )

            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = {
                    Text(when(uiState.darkThemeConfig) {
                        DarkThemeConfig.FOLLOW_SYSTEM -> "Follow System"
                        DarkThemeConfig.LIGHT -> "Light"
                        DarkThemeConfig.DARK -> "Dark"
                    })
                },
                leadingContent = { 
                    Icon(
                        if (uiState.darkThemeConfig == DarkThemeConfig.DARK) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, 
                        contentDescription = null
                    ) 
                },
                modifier = Modifier.clickable { showDarkConfigDialog = true }
            )

            ListItem(
                headlineContent = { Text("Energy Animations") },
                supportingContent = { Text("Special background effects for card types") },
                leadingContent = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.showEnergyAnimations,
                        onCheckedChange = { viewModel.setShowEnergyAnimations(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Card Finish Animations") },
                supportingContent = { Text("Holo sparkle and Gold shimmer effects") },
                leadingContent = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.showFinishAnimations,
                        onCheckedChange = { viewModel.setShowFinishAnimations(it) }
                    )
                }
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
                supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})") },
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

    if (showThemeBrandDialog) {
        AlertDialog(
            onDismissRequest = { showThemeBrandDialog = false },
            title = { Text("Select Color Theme") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Standard", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                    ThemeBrandOption("Default (Material You)", uiState.themeBrand == ThemeBrand.DEFAULT, null) {
                        viewModel.setThemeBrand(ThemeBrand.DEFAULT)
                        showThemeBrandDialog = false
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Energy Themes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                    
                    ThemeBrandOption("Grass", uiState.themeBrand == ThemeBrand.GRASS, EnergyGrass) {
                        viewModel.setThemeBrand(ThemeBrand.GRASS)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Fire", uiState.themeBrand == ThemeBrand.FIRE, EnergyFire) {
                        viewModel.setThemeBrand(ThemeBrand.FIRE)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Water", uiState.themeBrand == ThemeBrand.WATER, EnergyWater) {
                        viewModel.setThemeBrand(ThemeBrand.WATER)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Lightning", uiState.themeBrand == ThemeBrand.ELECTRIC, EnergyLightning) {
                        viewModel.setThemeBrand(ThemeBrand.ELECTRIC)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Psychic", uiState.themeBrand == ThemeBrand.PSYCHIC, EnergyPsychic) {
                        viewModel.setThemeBrand(ThemeBrand.PSYCHIC)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Fighting", uiState.themeBrand == ThemeBrand.FIGHTING, EnergyFighting) {
                        viewModel.setThemeBrand(ThemeBrand.FIGHTING)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Darkness", uiState.themeBrand == ThemeBrand.DARKNESS, EnergyDarkness) {
                        viewModel.setThemeBrand(ThemeBrand.DARKNESS)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Metal", uiState.themeBrand == ThemeBrand.STEEL, EnergyMetal) {
                        viewModel.setThemeBrand(ThemeBrand.STEEL)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Fairy", uiState.themeBrand == ThemeBrand.FAIRY, EnergyFairy) {
                        viewModel.setThemeBrand(ThemeBrand.FAIRY)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Dragon", uiState.themeBrand == ThemeBrand.DRAGON, EnergyDragon) {
                        viewModel.setThemeBrand(ThemeBrand.DRAGON)
                        showThemeBrandDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeBrandDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDarkConfigDialog) {
        AlertDialog(
            onDismissRequest = { showDarkConfigDialog = false },
            title = { Text("Dark Mode Settings") },
            text = {
                Column {
                    DarkConfigOption("Follow System", uiState.darkThemeConfig == DarkThemeConfig.FOLLOW_SYSTEM) {
                        viewModel.setDarkThemeConfig(DarkThemeConfig.FOLLOW_SYSTEM)
                        showDarkConfigDialog = false
                    }
                    DarkConfigOption("Light Mode", uiState.darkThemeConfig == DarkThemeConfig.LIGHT) {
                        viewModel.setDarkThemeConfig(DarkThemeConfig.LIGHT)
                        showDarkConfigDialog = false
                    }
                    DarkConfigOption("Dark Mode", uiState.darkThemeConfig == DarkThemeConfig.DARK) {
                        viewModel.setDarkThemeConfig(DarkThemeConfig.DARK)
                        showDarkConfigDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkConfigDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ThemeBrandOption(label: String, selected: Boolean, color: Color?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        if (color != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(label)
    }
}

@Composable
fun DarkConfigOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
