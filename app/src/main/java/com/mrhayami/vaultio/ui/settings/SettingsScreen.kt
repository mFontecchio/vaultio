package com.mrhayami.vaultio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrhayami.vaultio.BuildConfig
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.theme.*

@Composable
fun SettingsScreen(
    repository: VaultioRepository,
    userPreferencesRepository: UserPreferencesRepository,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository, userPreferencesRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onThemeBrandChange = viewModel::setThemeBrand,
        onDarkThemeConfigChange = viewModel::setDarkThemeConfig,
        onPreferSetLogoChange = viewModel::setPreferSetLogo,
        onShowEnergyAnimationsChange = viewModel::setShowEnergyAnimations,
        onShowFinishAnimationsChange = viewModel::setShowFinishAnimations,
        onJustTcgApiKeyChange = viewModel::setJustTcgApiKey,
        onRefreshApiUsage = viewModel::refreshApiUsage,
        onClearImageCache = viewModel::clearImageCache,
        onResetSettings = viewModel::resetSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onThemeBrandChange: (ThemeBrand) -> Unit,
    onDarkThemeConfigChange: (DarkThemeConfig) -> Unit,
    onPreferSetLogoChange: (Boolean) -> Unit,
    onShowEnergyAnimationsChange: (Boolean) -> Unit,
    onShowFinishAnimationsChange: (Boolean) -> Unit,
    onJustTcgApiKeyChange: (String) -> Unit,
    onRefreshApiUsage: () -> Unit,
    onClearImageCache: () -> Unit,
    onResetSettings: () -> Unit
) {
    var showThemeBrandDialog by remember { mutableStateOf(false) }
    var showDarkConfigDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var animationsExpanded by remember { mutableStateOf(false) }

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
                headlineContent = { Text("Prefer Set Logos") },
                supportingContent = { Text("Show large set logos instead of small icons where possible") },
                leadingContent = { Icon(Icons.Rounded.Image, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.preferSetLogo,
                        onCheckedChange = { onPreferSetLogoChange(it) }
                    )
                },
                modifier = Modifier.clickable { onPreferSetLogoChange(!uiState.preferSetLogo) }
            )

            ListItem(
                headlineContent = { Text("Animations") },
                supportingContent = { Text("Visual effects for cards") },
                leadingContent = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                trailingContent = {
                    Icon(
                        if (animationsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { animationsExpanded = !animationsExpanded }
            )

            if (animationsExpanded) {
                ListItem(
                    headlineContent = { Text("Energy Animations") },
                    supportingContent = { Text("Special background effects for card types") },
                    trailingContent = {
                        Switch(
                            checked = uiState.showEnergyAnimations,
                            onCheckedChange = { onShowEnergyAnimationsChange(it) }
                        )
                    },
                    modifier = Modifier.padding(start = 32.dp)
                )

                ListItem(
                    headlineContent = { Text("Card Finish Animations") },
                    supportingContent = { Text("Holo sparkle and Gold shimmer effects") },
                    trailingContent = {
                        Switch(
                            checked = uiState.showFinishAnimations,
                            onCheckedChange = { onShowFinishAnimationsChange(it) }
                        )
                    },
                    modifier = Modifier.padding(start = 32.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Market Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("JustTCG API Key") },
                supportingContent = { 
                    Text(if (uiState.justTcgApiKey.isEmpty()) "Not set (required for some pricing)" else "••••••••••••••••") 
                },
                leadingContent = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
                modifier = Modifier.clickable { showApiKeyDialog = true }
            )

            ListItem(
                headlineContent = { Text("JustTCG API Usage") },
                supportingContent = {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Plan: ${uiState.planName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Synced: ${formatLastSynced(uiState.lastSyncedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Daily Usage", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { if (uiState.dailyLimit > 0) (uiState.dailyUsed.toFloat() / uiState.dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = if (uiState.dailyRemaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${uiState.dailyUsed} / ${uiState.dailyLimit} used", style = MaterialTheme.typography.bodySmall)
                            Text("${uiState.dailyRemaining} remaining", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Plan Usage", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { if (uiState.planLimit > 0) (uiState.planUsed.toFloat() / uiState.planLimit.toFloat()).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = if (uiState.planRemaining <= 50) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${uiState.planUsed} / ${uiState.planLimit} used", style = MaterialTheme.typography.bodySmall)
                            Text("${uiState.planRemaining} remaining", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                leadingContent = { Icon(Icons.Rounded.Api, contentDescription = null) },
                trailingContent = {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { onRefreshApiUsage() },
                            enabled = uiState.justTcgApiKey.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Refresh API usage",
                                tint = if (uiState.justTcgApiKey.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Clear Image Cache") },
                supportingContent = { Text("Free up space on your device") },
                leadingContent = { Icon(Icons.Rounded.Storage, contentDescription = null) },
                modifier = Modifier.clickable { onClearImageCache() }
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
                onClick = { onResetSettings() },
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
                        onThemeBrandChange(ThemeBrand.DEFAULT)
                        showThemeBrandDialog = false
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Energy Themes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                    
                    ThemeBrandOption("Grass", uiState.themeBrand == ThemeBrand.GRASS, EnergyGrass) {
                        onThemeBrandChange(ThemeBrand.GRASS)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Fire", uiState.themeBrand == ThemeBrand.FIRE, EnergyFire) {
                        onThemeBrandChange(ThemeBrand.FIRE)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Water", uiState.themeBrand == ThemeBrand.WATER, EnergyWater) {
                        onThemeBrandChange(ThemeBrand.WATER)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Lightning", uiState.themeBrand == ThemeBrand.ELECTRIC, EnergyLightning) {
                        onThemeBrandChange(ThemeBrand.ELECTRIC)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Psychic", uiState.themeBrand == ThemeBrand.PSYCHIC, EnergyPsychic) {
                        onThemeBrandChange(ThemeBrand.PSYCHIC)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Fighting", uiState.themeBrand == ThemeBrand.FIGHTING, EnergyFighting) {
                        onThemeBrandChange(ThemeBrand.FIGHTING)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Darkness", uiState.themeBrand == ThemeBrand.DARKNESS, EnergyDarkness) {
                        onThemeBrandChange(ThemeBrand.DARKNESS)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Metal", uiState.themeBrand == ThemeBrand.STEEL, EnergyMetal) {
                        onThemeBrandChange(ThemeBrand.STEEL)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Fairy", uiState.themeBrand == ThemeBrand.FAIRY, EnergyFairy) {
                        onThemeBrandChange(ThemeBrand.FAIRY)
                        showThemeBrandDialog = false
                    }
                    ThemeBrandOption("Dragon", uiState.themeBrand == ThemeBrand.DRAGON, EnergyDragon) {
                        onThemeBrandChange(ThemeBrand.DRAGON)
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
                        onDarkThemeConfigChange(DarkThemeConfig.FOLLOW_SYSTEM)
                        showDarkConfigDialog = false
                    }
                    DarkConfigOption("Light Mode", uiState.darkThemeConfig == DarkThemeConfig.LIGHT) {
                        onDarkThemeConfigChange(DarkThemeConfig.LIGHT)
                        showDarkConfigDialog = false
                    }
                    DarkConfigOption("Dark Mode", uiState.darkThemeConfig == DarkThemeConfig.DARK) {
                        onDarkThemeConfigChange(DarkThemeConfig.DARK)
                        showDarkConfigDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkConfigDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(uiState.justTcgApiKey) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("JustTCG API Key") },
            text = {
                Column {
                    Text(
                        "Enter your JustTCG API key to enable vintage pricing and fallback market data.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    onJustTcgApiKeyChange(tempKey)
                    showApiKeyDialog = false 
                }) { 
                    Text("Save") 
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel") }
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

/** Returns a human-readable "X ago" string for the given epoch-millis timestamp. */
private fun formatLastSynced(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L      -> "Just now"
        diff < 3_600_000L   -> "${diff / 60_000L}m ago"
        diff < 86_400_000L  -> "${diff / 3_600_000L}h ago"
        else                -> "${diff / 86_400_000L}d ago"
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    VaultioTheme {
        Surface {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    themeBrand = ThemeBrand.DEFAULT,
                    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                    justTcgApiKey = "mock_api_key",
                    dailyUsed = 42,
                    dailyLimit = 100,
                    dailyRemaining = 58,
                    planUsed = 150,
                    planLimit = 1000,
                    planRemaining = 850,
                    planName = "Basic",
                    offlineSetsCount = 5,
                    isRefreshing = false
                ),
                onThemeBrandChange = {},
                onDarkThemeConfigChange = {},
                onPreferSetLogoChange = {},
                onShowEnergyAnimationsChange = {},
                onShowFinishAnimationsChange = {},
                onJustTcgApiKeyChange = {},
                onRefreshApiUsage = {},
                onClearImageCache = {},
                onResetSettings = {}
            )
        }
    }
}
