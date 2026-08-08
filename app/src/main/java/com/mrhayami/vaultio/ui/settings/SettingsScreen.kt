package com.mrhayami.vaultio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrhayami.vaultio.BuildConfig
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.repository.AppUpdateRepository
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.theme.EnergyDarkness
import com.mrhayami.vaultio.ui.theme.EnergyDragon
import com.mrhayami.vaultio.ui.theme.EnergyFairy
import com.mrhayami.vaultio.ui.theme.EnergyFighting
import com.mrhayami.vaultio.ui.theme.EnergyFire
import com.mrhayami.vaultio.ui.theme.EnergyGrass
import com.mrhayami.vaultio.ui.theme.EnergyLightning
import com.mrhayami.vaultio.ui.theme.EnergyMetal
import com.mrhayami.vaultio.ui.theme.EnergyPsychic
import com.mrhayami.vaultio.ui.theme.EnergyWater
import com.mrhayami.vaultio.ui.theme.VaultioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: VaultioRepository,
    userPreferencesRepository: UserPreferencesRepository,
    appUpdateRepository: AppUpdateRepository,
    onNavigateToDownloads: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as VaultioApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            repository,
            userPreferencesRepository,
            appUpdateRepository,
            application
        ),
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional; install path works without notifications */ }
    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(SettingsEvent.ResumeInstallAfterUnknownSources)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                SettingsEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                SettingsEffect.OpenUnknownSourcesSettings -> {
                    unknownSourcesLauncher.launch(appUpdateRepository.unknownSourcesSettingsIntent())
                }
                is SettingsEffect.LaunchInstall -> {
                    context.startActivity(effect.intent)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        SettingsScreenContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onNavigateToDownloads = onNavigateToDownloads,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeBrandDialog by remember { mutableStateOf(value = false) }
    var showDarkConfigDialog by remember { mutableStateOf(value = false) }
    var showApiKeyDialog by remember { mutableStateOf(value = false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AppearanceSection(
            uiState = uiState,
            onEvent = onEvent,
            onShowThemeDialog = { showThemeBrandDialog = true }
        ) { showDarkConfigDialog = true }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        MarketDataSection(
            uiState = uiState,
            onEvent = onEvent
        ) { showApiKeyDialog = true }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        StorageSection(
            offlineSetsCount = uiState.offlineSetsCount,
            onClearCache = { onEvent(SettingsEvent.ClearImageCache) },
            onNavigateToDownloads = onNavigateToDownloads
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        AboutSection(
            uiState = uiState,
            onEvent = onEvent
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        DeveloperSection(
            onResetSettings = { onEvent(SettingsEvent.ResetSettings) }
        )
    }

    if (showThemeBrandDialog) {
        ThemeBrandDialog(
            currentBrand = uiState.themeBrand,
            onBrandSelected = { onEvent(SettingsEvent.SetThemeBrand(it)) },
            onDismiss = { showThemeBrandDialog = false }
        )
    }

    if (showDarkConfigDialog) {
        DarkThemeConfigDialog(
            currentConfig = uiState.darkThemeConfig,
            onConfigSelected = { onEvent(SettingsEvent.SetDarkThemeConfig(it)) },
            onDismiss = { showDarkConfigDialog = false }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = uiState.justTcgApiKey,
            onSaveKey = { onEvent(SettingsEvent.SetJustTcgApiKey(it)) },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AppearanceSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowDarkConfigDialog: () -> Unit
) {
    var animationsExpanded by remember { mutableStateOf(value = false) }

    SettingsSection(title = "Appearance") {
        ListItem(
            headlineContent = { Text("Theme Color") },
            supportingContent = {
                Text(
                    when (uiState.themeBrand) {
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
                    }
                )
            },
            leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onShowThemeDialog)
        )

        ListItem(
            headlineContent = { Text("Dark Mode") },
            supportingContent = {
                Text(
                    when (uiState.darkThemeConfig) {
                        DarkThemeConfig.FOLLOW_SYSTEM -> "Follow System"
                        DarkThemeConfig.LIGHT -> "Light"
                        DarkThemeConfig.DARK -> "Dark"
                    }
                )
            },
            leadingContent = {
                Icon(
                    if (uiState.darkThemeConfig == DarkThemeConfig.DARK) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable(onClick = onShowDarkConfigDialog)
        )

        ListItem(
            headlineContent = { Text("Prefer Set Logos") },
            supportingContent = { Text("Show large set logos instead of small icons where possible") },
            leadingContent = { Icon(Icons.Rounded.Image, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = uiState.preferSetLogo,
                    onCheckedChange = { onEvent(SettingsEvent.SetPreferSetLogo(it)) }
                )
            },
            modifier = Modifier.clickable { onEvent(SettingsEvent.SetPreferSetLogo(!uiState.preferSetLogo)) }
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
                        onCheckedChange = { onEvent(SettingsEvent.SetShowEnergyAnimations(it)) }
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
                        onCheckedChange = { onEvent(SettingsEvent.SetShowFinishAnimations(it)) }
                    )
                },
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

@Composable
private fun MarketDataSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onShowApiKeyDialog: () -> Unit
) {
    SettingsSection(title = "Market Data") {
        ListItem(
            headlineContent = { Text("JustTCG API Key") },
            supportingContent = {
                Text(if (uiState.justTcgApiKey.isEmpty()) "Not set (required for some pricing)" else "••••••••••••••••")
            },
            leadingContent = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onShowApiKeyDialog)
        )

        ListItem(
            headlineContent = { Text("JustTCG API Usage") },
            supportingContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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
                        onClick = { onEvent(SettingsEvent.RefreshApiUsage) },
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
    }
}

@Composable
private fun StorageSection(
    offlineSetsCount: Int,
    onClearCache: () -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    SettingsSection(title = "Storage") {
        ListItem(
            headlineContent = { Text("Clear Image Cache") },
            supportingContent = { Text("Free up space on your device") },
            leadingContent = { Icon(Icons.Rounded.Storage, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onClearCache)
        )

        ListItem(
            headlineContent = { Text("$offlineSetsCount sets downloaded") },
            supportingContent = { Text("Manage offline data and high-res images") },
            leadingContent = { Icon(Icons.Rounded.Download, contentDescription = null) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onNavigateToDownloads)
        )
    }
}

@Composable
private fun AboutSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit
) {
    SettingsSection(title = "About") {
        ListItem(
            headlineContent = { Text("App Version") },
            supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})") },
            leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) }
        )

        when {
            uiState.isPlayInstall -> {
                ListItem(
                    headlineContent = { Text("Updates") },
                    supportingContent = {
                        Text("Installed from Google Play. Updates are delivered by Play Store.")
                    },
                    leadingContent = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) }
                )
            }
            !uiState.updaterSupported -> {
                ListItem(
                    headlineContent = { Text("Updates") },
                    supportingContent = {
                        Text("In-app updates are available for release and nightly builds.")
                    },
                    leadingContent = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) }
                )
            }
            else -> {
                ListItem(
                    headlineContent = { Text("Automatically check & download updates") },
                    supportingContent = {
                        Text("Uses GitHub Releases. Android still asks you to confirm install.")
                    },
                    leadingContent = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.autoUpdateEnabled,
                            onCheckedChange = { onEvent(SettingsEvent.SetAutoUpdateEnabled(it)) }
                        )
                    }
                )

                val busy = uiState.updateCheckState is UpdateCheckUiState.Checking ||
                    uiState.updateCheckState is UpdateCheckUiState.Downloading

                TextButton(
                    onClick = { onEvent(SettingsEvent.CheckForUpdates) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check for updates")
                }

                UpdateStatusRow(uiState = uiState)

                if (uiState.updateCheckState is UpdateCheckUiState.ReadyToInstall ||
                    uiState.updateCheckState is UpdateCheckUiState.Available
                ) {
                    Button(
                        onClick = { onEvent(SettingsEvent.InstallUpdate) },
                        enabled = uiState.updateCheckState is UpdateCheckUiState.ReadyToInstall,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Install update")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusRow(uiState: SettingsUiState) {
    val text = when (val state = uiState.updateCheckState) {
        UpdateCheckUiState.Idle -> "Tap check to look for a newer GitHub build."
        UpdateCheckUiState.Checking -> "Checking for updates…"
        UpdateCheckUiState.UpToDate -> "You're up to date."
        is UpdateCheckUiState.Available -> "Update available: ${state.tagName}"
        is UpdateCheckUiState.Downloading -> {
            val pct = state.progress?.let { "${(it * 100).toInt()}%" } ?: "…"
            "Downloading $pct"
        }
        is UpdateCheckUiState.ReadyToInstall -> "Ready to install ${state.tagName}"
        is UpdateCheckUiState.Error -> state.message
    }

    ListItem(
        headlineContent = { Text("Update status") },
        supportingContent = { Text(text) }
    )

    val downloading = uiState.updateCheckState as? UpdateCheckUiState.Downloading
    if (downloading != null) {
        LinearProgressIndicator(
            progress = { downloading.progress ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DeveloperSection(
    onResetSettings: () -> Unit
) {
    SettingsSection(title = "Developer") {
        TextButton(
            onClick = onResetSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Reset All Settings")
        }
    }
}

@Composable
private fun ThemeBrandDialog(
    currentBrand: ThemeBrand,
    onBrandSelected: (ThemeBrand) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color Theme") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Standard",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                ThemeBrandOption(
                    "Default (Material You)",
                    currentBrand == ThemeBrand.DEFAULT,
                    null
                ) {
                    onBrandSelected(ThemeBrand.DEFAULT)
                    onDismiss()
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Energy Themes",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                ThemeBrandOption("Grass", currentBrand == ThemeBrand.GRASS, EnergyGrass) {
                    onBrandSelected(ThemeBrand.GRASS)
                    onDismiss()
                }
                ThemeBrandOption("Fire", currentBrand == ThemeBrand.FIRE, EnergyFire) {
                    onBrandSelected(ThemeBrand.FIRE)
                    onDismiss()
                }
                ThemeBrandOption("Water", currentBrand == ThemeBrand.WATER, EnergyWater) {
                    onBrandSelected(ThemeBrand.WATER)
                    onDismiss()
                }
                ThemeBrandOption(
                    "Lightning",
                    currentBrand == ThemeBrand.ELECTRIC,
                    EnergyLightning
                ) {
                    onBrandSelected(ThemeBrand.ELECTRIC)
                    onDismiss()
                }
                ThemeBrandOption("Psychic", currentBrand == ThemeBrand.PSYCHIC, EnergyPsychic) {
                    onBrandSelected(ThemeBrand.PSYCHIC)
                    onDismiss()
                }
                ThemeBrandOption("Fighting", currentBrand == ThemeBrand.FIGHTING, EnergyFighting) {
                    onBrandSelected(ThemeBrand.FIGHTING)
                    onDismiss()
                }
                ThemeBrandOption("Darkness", currentBrand == ThemeBrand.DARKNESS, EnergyDarkness) {
                    onBrandSelected(ThemeBrand.DARKNESS)
                    onDismiss()
                }
                ThemeBrandOption("Metal", currentBrand == ThemeBrand.STEEL, EnergyMetal) {
                    onBrandSelected(ThemeBrand.STEEL)
                    onDismiss()
                }
                ThemeBrandOption("Fairy", currentBrand == ThemeBrand.FAIRY, EnergyFairy) {
                    onBrandSelected(ThemeBrand.FAIRY)
                    onDismiss()
                }
                ThemeBrandOption("Dragon", currentBrand == ThemeBrand.DRAGON, EnergyDragon) {
                    onBrandSelected(ThemeBrand.DRAGON)
                    onDismiss()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DarkThemeConfigDialog(
    currentConfig: DarkThemeConfig,
    onConfigSelected: (DarkThemeConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dark Mode Settings") },
        text = {
            Column {
                DarkConfigOption("Follow System", currentConfig == DarkThemeConfig.FOLLOW_SYSTEM) {
                    onConfigSelected(DarkThemeConfig.FOLLOW_SYSTEM)
                    onDismiss()
                }
                DarkConfigOption("Light Mode", currentConfig == DarkThemeConfig.LIGHT) {
                    onConfigSelected(DarkThemeConfig.LIGHT)
                    onDismiss()
                }
                DarkConfigOption("Dark Mode", currentConfig == DarkThemeConfig.DARK) {
                    onConfigSelected(DarkThemeConfig.DARK)
                    onDismiss()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempKey by remember { mutableStateOf(currentKey) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                onSaveKey(tempKey)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        CircleShape
                    )
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
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
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
                onEvent = {},
                onNavigateToDownloads = {}
            )
        }
    }
}
