package com.mrhayami.vaultio.ui.scanner

import android.Manifest
import android.graphics.Bitmap
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.components.DropdownSelector
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    repository: VaultioRepository,
    onNavigateBack: () -> Unit,
    viewModel: ScannerViewModel = viewModel(factory = ScannerViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.onEvent(ScannerEvent.PermissionResult(cameraPermissionState.status.isGranted))
    }

    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            Toast.makeText(context, "Card added to collection", Toast.LENGTH_SHORT).show()
            viewModel.onEvent(ScannerEvent.ConsumeSaveSuccess)
        }
    }

    var showBulkSettings by remember { mutableStateOf(false) }
    var showSkippedReview by remember { mutableStateOf(false) }
    
    val onEventStable = remember(viewModel) { { event: ScannerEvent -> viewModel.onEvent(event) } }
    
    ScannerContent(
        uiState = uiState,
        onEvent = onEventStable,
        onNavigateBack = onNavigateBack,
        onShowBulkSettings = { showBulkSettings = true },
        onShowSkippedReview = { showSkippedReview = true }
    )

    if (showBulkSettings) {
        ModalBottomSheet(
            onDismissRequest = { showBulkSettings = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BulkSettingsSheet(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onDismiss = { showBulkSettings = false }
            )
        }
    }

    if (showSkippedReview) {
        ModalBottomSheet(
            onDismissRequest = { showSkippedReview = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            SkippedReviewSheet(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onDismiss = { showSkippedReview = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerContent(
    uiState: ScannerUiState,
    onEvent: (ScannerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onShowBulkSettings: () -> Unit,
    onShowSkippedReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onEventStable = remember(onEvent) { onEvent }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.hasCameraPermission) {
            if (uiState.pageScanMode == PageScanMode.REVIEWING) {
                PageScanReviewContent(
                    uiState = uiState,
                    onEvent = onEventStable
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    val onLinesDetected = remember(onEventStable) {
                        { lines: List<DetectedLine>, pHash: Long? ->
                            onEventStable(ScannerEvent.LinesDetected(lines, pHash))
                        }
                    }
                    val onPhotoCaptured = remember(onEventStable) {
                        { bitmap: Bitmap ->
                            onEventStable(ScannerEvent.CapturePagePhoto(bitmap))
                        }
                    }
                    CameraPreview(
                        isPageScanMode = uiState.isPageScanMode,
                        isTorchEnabled = uiState.isTorchEnabled,
                        onLinesDetected = onLinesDetected,
                        onPhotoCaptured = onPhotoCaptured
                    )
                    
                    if (uiState.isPageScanMode) {
                        PageScanOverlay(isProcessing = uiState.pageScanMode == PageScanMode.PROCESSING)
                    } else {
                        ScannerOverlay(isSearching = uiState.isSearching)
                    }
                }
            }

            // Header
            AnimatedVisibility(
                visible = uiState.pageScanMode != PageScanMode.REVIEWING,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // Mode Selector
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            ModeButton(
                                selected = !uiState.isBulkMode && !uiState.isPageScanMode,
                                icon = Icons.Rounded.Search,
                                onClick = { 
                                    if (uiState.isBulkMode) onEvent(ScannerEvent.ToggleBulkMode)
                                    if (uiState.isPageScanMode) onEvent(ScannerEvent.TogglePageScanMode)
                                }
                            )
                            ModeButton(
                                selected = uiState.isBulkMode,
                                icon = Icons.Rounded.Layers,
                                onClick = { onEvent(ScannerEvent.ToggleBulkMode) }
                            )
                            ModeButton(
                                selected = uiState.isPageScanMode,
                                icon = Icons.Rounded.GridView,
                                onClick = { onEvent(ScannerEvent.TogglePageScanMode) }
                            )
                        }
                    }

                    IconButton(
                        onClick = { onEvent(ScannerEvent.ToggleTorch) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.isTorchEnabled) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (uiState.isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = if (uiState.isTorchEnabled) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                    }

                    IconButton(
                        onClick = onShowBulkSettings,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Bulk Settings", tint = Color.White)
                    }
                }
            }

            // HUD / Status Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 125.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = (uiState.detectedNumber != null || uiState.isSearching) && !uiState.isPageScanMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isSearching) "Searching..." else "Detected: ${uiState.detectedNumber ?: ""} ${uiState.detectedName ?: ""}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // High Confidence Match
            AnimatedVisibility(
                visible = uiState.autoSelectedCard != null && !uiState.isBulkMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.autoSelectedCard?.let { card ->
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 4.dp
                            ) {
                                AsyncImage(
                                    model = "${card.image}/low.webp",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp, 112.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = card.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                    FilledTonalIconButton(
                                        onClick = { onEvent(ScannerEvent.ResumeScanning) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Reject")
                                    }
                                }
                                Text(
                                    text = card.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                card.rarity?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onEvent(ScannerEvent.CardSelected(card)) },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Add Details", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bulk Mode HUD
            AnimatedVisibility(
                visible = uiState.isBulkMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BulkModeHUD(
                    uiState = uiState,
                    onEvent = onEvent,
                    onShowSkippedReview = onShowSkippedReview
                )
            }

            // Candidates List
            if (uiState.candidates.isNotEmpty() && uiState.autoSelectedCard == null && !uiState.isBulkMode) {
                ModalBottomSheet(
                    onDismissRequest = { onEvent(ScannerEvent.ClearDetectedNumber) },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Text(
                            "Select Match", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold, 
                            modifier = Modifier.padding(24.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = uiState.candidates,
                                key = { it.id }
                            ) { card ->
                                CandidateItem(card = card, onClick = { onEvent(ScannerEvent.CardSelected(card)) })
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission is required to scan cards.")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { onEvent(ScannerEvent.PermissionResult(true)) }) { // Mock granting for preview
                        Text("Grant Permission")
                    }
                }
            }
        }

        if (uiState.selectedCard != null) {
            ModalBottomSheet(
                onDismissRequest = { onEvent(ScannerEvent.CardSelected(null)) },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                MetadataModal(
                    card = uiState.selectedCard,
                    folders = uiState.folders,
                    onConfirm = { q, c, p, f, folderIds ->
                        onEvent(ScannerEvent.SaveScannedCard(uiState.selectedCard, q, c, p, f, folderIds))
                    },
                    onBack = { onEvent(ScannerEvent.CardSelected(null)) }
                )
            }
        }
    }
}

@Composable
fun CandidateItem(
    card: TcgDexCard, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(card.name, fontWeight = FontWeight.Bold) },
        supportingContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.id, style = MaterialTheme.typography.bodySmall)
                card.rarity?.let {
                    Text(" • $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp, 68.dp)
            ) {
                AsyncImage(
                    model = "${card.image}/low.webp",
                    contentDescription = null,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        },
        modifier = modifier
            .padding(vertical = 2.dp, horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkSettingsSheet(
    uiState: ScannerUiState,
    onEvent: (ScannerEvent) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Bulk Scanning Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val conditions = listOf(
                    com.mrhayami.vaultio.data.PricingUtils.CONDITION_NM,
                    com.mrhayami.vaultio.data.PricingUtils.CONDITION_LP,
                    com.mrhayami.vaultio.data.PricingUtils.CONDITION_MP,
                    com.mrhayami.vaultio.data.PricingUtils.CONDITION_HP,
                    com.mrhayami.vaultio.data.PricingUtils.CONDITION_DMG
                )
                DropdownSelector(
                    "Default Condition",
                    uiState.bulkDefaults.condition,
                    conditions
                ) {
                    onEvent(ScannerEvent.SetBulkDefaults(uiState.bulkDefaults.copy(condition = it)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                val printings = listOf(
                    com.mrhayami.vaultio.data.PricingUtils.PRINTING_UNLIMITED,
                    com.mrhayami.vaultio.data.PricingUtils.PRINTING_SHADOWLESS,
                    com.mrhayami.vaultio.data.PricingUtils.PRINTING_PROMO,
                    com.mrhayami.vaultio.data.PricingUtils.PRINTING_1ST_EDITION
                )
                DropdownSelector(
                    "Default Printing",
                    uiState.bulkDefaults.printing,
                    printings
                ) {
                    onEvent(ScannerEvent.SetBulkDefaults(uiState.bulkDefaults.copy(printing = it)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                val finishes = listOf(
                    com.mrhayami.vaultio.data.PricingUtils.FINISH_NORMAL,
                    com.mrhayami.vaultio.data.PricingUtils.FINISH_HOLOFOIL,
                    com.mrhayami.vaultio.data.PricingUtils.FINISH_REVERSE_HOLO,
                    com.mrhayami.vaultio.data.PricingUtils.FINISH_TEXTURED,
                    com.mrhayami.vaultio.data.PricingUtils.FINISH_GOLD
                )
                DropdownSelector(
                    "Default Finish",
                    uiState.bulkDefaults.finish,
                    finishes
                ) {
                    onEvent(ScannerEvent.SetBulkDefaults(uiState.bulkDefaults.copy(finish = it)))
                }

                if (uiState.folders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Add to Folders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.folders.forEach { folder ->
                            val isSelected = uiState.bulkDefaults.folderIds.contains(folder.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newIds = if (isSelected) {
                                        uiState.bulkDefaults.folderIds.filter { it != folder.id }
                                    } else {
                                        uiState.bulkDefaults.folderIds + folder.id
                                    }
                                    onEvent(ScannerEvent.SetBulkDefaults(uiState.bulkDefaults.copy(folderIds = newIds)))
                                },
                                label = { Text(folder.name) },
                                shape = CircleShape
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BulkModeHUD(
    uiState: ScannerUiState,
    onEvent: (ScannerEvent) -> Unit,
    onShowSkippedReview: () -> Unit
) {
    val lastSaved = uiState.bulkSessionLog.lastOrNull { it.status != BulkScanStatus.SKIPPED_AMBIGUOUS }
    val skippedCount = uiState.skippedCards.distinctBy { it.id }.size
    val totalScanned = uiState.bulkSessionLog.filter { it.status != BulkScanStatus.SKIPPED_AMBIGUOUS }.sumOf { it.quantity }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Text(
                    text = "$totalScanned cards scanned",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            if (skippedCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                    modifier = Modifier.clickable { onShowSkippedReview() }
                ) {
                    Text(
                        text = "Review $skippedCount skipped",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastSaved != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp, 68.dp)
                    ) {
                        AsyncImage(
                            model = "${lastSaved.card.image}/low.webp",
                            contentDescription = null,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lastSaved.card.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lastSaved.status == BulkScanStatus.DUPLICATE_INCREMENTED) "Added (x${lastSaved.quantity})" else "Added",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    IconButton(onClick = { onEvent(ScannerEvent.UndoLastBulkScan) }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ready to scan bulk cards...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkippedReviewSheet(
    uiState: ScannerUiState,
    onEvent: (ScannerEvent) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCardForReview by remember { mutableStateOf<TcgDexCard?>(null) }
    val skippedCards = uiState.skippedCards.distinctBy { it.id }

    if (selectedCardForReview != null) {
        MetadataModal(
            card = selectedCardForReview!!,
            folders = uiState.folders,
            onConfirm = { q, c, p, f, folderIds ->
                onEvent(ScannerEvent.ConfirmSkippedCard(selectedCardForReview!!, q, c, p, f, folderIds))
                selectedCardForReview = null
                if (skippedCards.size <= 1) onDismiss()
            },
            onBack = { selectedCardForReview = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Review Skipped Matches",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (skippedCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No skipped cards to review.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = skippedCards,
                        key = { it.id }
                    ) { card ->
                        CandidateItem(
                            card = card,
                            onClick = { selectedCardForReview = card }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(8.dp).size(20.dp)
        )
    }
}

@Composable
fun CameraPreview(
    isPageScanMode: Boolean = false,
    isTorchEnabled: Boolean = false,
    onLinesDetected: (List<DetectedLine>, Long?) -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera Preview",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var showFlash by remember { mutableStateOf(false) }

    LaunchedEffect(isTorchEnabled, cameraControl) {
        cameraControl?.enableTorch(isTorchEnabled)
    }

    LaunchedEffect(showFlash) {
        if (showFlash) {
            delay(80)
            showFlash = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1920, 1080),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }

    LaunchedEffect(isPageScanMode, lifecycleOwner, previewViewRef) {
        val previewView = previewViewRef ?: return@LaunchedEffect
        
        val cameraProvider = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
            cameraProviderFuture.addListener({
                continuation.resume(cameraProviderFuture.get())
            }, ContextCompat.getMainExecutor(context))
        }

        val preview = CameraPreview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                // Determine aspect ratio for analyzer
                val ar = if (previewView.width > 0) {
                    previewView.width.toFloat() / previewView.height.toFloat()
                } else {
                    context.resources.displayMetrics.let { dm ->
                        dm.widthPixels.toFloat() / dm.heightPixels.toFloat()
                    }
                }
                it.setAnalyzer(executor, CameraAnalyzer(ar, onLinesDetected))
            }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
        imageCapture = capture

        try {
            val camera = cameraProvider.unbindAll().let {
                if (isPageScanMode) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }
            }
            cameraControl = camera.cameraControl
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    keepScreenOn = true
                }.also { previewViewRef = it }
            },
            modifier = Modifier.fillMaxSize(),
            update = { /* Camera logic moved to LaunchedEffect */ }
        )

        if (showFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        if (isPageScanMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showFlash = true
                        imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                val rotation = image.imageInfo.rotationDegrees
                                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                
                                // Viewport Alignment: Calculate visible area based on screen aspect ratio
                                val pv = previewViewRef ?: return
                                val cropRect = ScannerGeometry.getCropRect(
                                    bitmapWidth = rotatedBitmap.width,
                                    bitmapHeight = rotatedBitmap.height,
                                    viewportWidth = pv.width.toFloat(),
                                    viewportHeight = pv.height.toFloat(),
                                    isPageScanMode = true
                                )
                                
                                val cropped = try {
                                    Bitmap.createBitmap(
                                        rotatedBitmap,
                                        cropRect.left,
                                        cropRect.top,
                                        cropRect.width(),
                                        cropRect.height()
                                    )
                                } catch (e: Exception) {
                                    rotatedBitmap
                                }
                                
                                onPhotoCaptured(cropped)
                                image.close()
                                
                                // Clean up intermediate bitmaps
                                if (bitmap != cropped) bitmap.recycle()
                                if (rotatedBitmap != bitmap && rotatedBitmap != cropped) rotatedBitmap.recycle()
                            }
                        })
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(4.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun PageScanOverlay(isProcessing: Boolean) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            val rect = ScannerGeometry.getOverlayRect(size, isPageScanMode = true)
            val gridWidth = rect.width
            val gridHeight = rect.height
            val left = rect.left
            val top = rect.top

            // Background mask
            drawRect(color = Color.Black.copy(alpha = 0.5f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = ComposeSize(gridWidth, gridHeight),
                cornerRadius = CornerRadius(12.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Grid lines
            val strokeWidth = 1.dp.toPx()
            val lineColor = Color.White.copy(alpha = 0.5f)

            // Vertical lines
            drawLine(lineColor, Offset(left + gridWidth / 3, top), Offset(left + gridWidth / 3, top + gridHeight), strokeWidth)
            drawLine(lineColor, Offset(left + 2 * gridWidth / 3, top), Offset(left + 2 * gridWidth / 3, top + gridHeight), strokeWidth)

            // Horizontal lines
            drawLine(lineColor, Offset(left, top + gridHeight / 3), Offset(left + gridWidth, top + gridHeight / 3), strokeWidth)
            drawLine(lineColor, Offset(left, top + 2 * gridHeight / 3), Offset(left + gridWidth, top + 2 * gridHeight / 3), strokeWidth)
            
            // Outer border
            drawRoundRect(
                color = if (isProcessing) primaryColor else Color.White,
                topLeft = Offset(left, top),
                size = ComposeSize(gridWidth, gridHeight),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = primaryColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing Page...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Scanning 9 cards in parallel", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You can move your phone now", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            // Hint for the user
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 125.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hold Still • Align Grid",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageScanReviewContent(
    uiState: ScannerUiState,
    onEvent: (ScannerEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Review Page Scan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onEvent(ScannerEvent.RetryPageScan) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp), // Align with 95% width overlay
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = uiState.pageScanCells,
                key = { it.id }
            ) { cell ->
                PageCellReviewItem(cell = cell, onEvent = onEvent)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(ScannerEvent.RetryPageScan) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = { onEvent(ScannerEvent.SaveAllPageResults) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState.pageScanCells.any { it.isConfirmed && it.matchedCard != null }
                ) {
                    val count = uiState.pageScanCells.count { it.isConfirmed && it.matchedCard != null }
                    Text("Save $count Cards")
                }
            }
        }
    }
}

@Composable
fun PageCellReviewItem(
    cell: PageScanCell,
    onEvent: (ScannerEvent) -> Unit
) {
    val isConfirmed = cell.isConfirmed && cell.matchedCard != null
    
    Card(
        modifier = Modifier
            .aspectRatio(0.715f) // Standard card aspect ratio
            .clickable { 
                if (cell.isConfirmed) onEvent(ScannerEvent.RejectPageCell(cell.id))
                else onEvent(ScannerEvent.ConfirmPageCell(cell.id, cell.matchedCard))
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfirmed) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isConfirmed) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cell.matchedCard != null) {
                AsyncImage(
                    model = "${cell.matchedCard.image}/low.webp",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (cell.isConfirmed) 1f else 0.5f
                )
            } else if (cell.bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = cell.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
            }

            // Overlay Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = cell.matchedCard?.name ?: "No Match",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = cell.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when(cell.status) {
                        PageScanCellStatus.MATCHED -> Color.Green
                        PageScanCellStatus.AMBIGUOUS -> Color.Yellow
                        else -> Color.Red
                    }.copy(alpha = 0.8f)
                )
            }

            if (cell.status == PageScanCellStatus.SCANNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                    strokeWidth = 2.dp
                )
            }

            if (isConfirmed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
fun ScannerOverlay(isSearching: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scannerLineAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerLine"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val rect = ScannerGeometry.getOverlayRect(size, isPageScanMode = false)
        val rectWidth = rect.width
        val rectHeight = rect.height
        val left = rect.left
        val top = rect.top
        val cornerRadius = 24.dp.toPx()
        val cornerSize = 40.dp.toPx()

        // Background mask
        drawRect(color = Color.Black.copy(alpha = 0.5f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = ComposeSize(rectWidth, rectHeight),
            cornerRadius = CornerRadius(cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Draw scanning line
        val lineY = top + (rectHeight * scannerLineAnim)
        if (isSearching) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startY = lineY - 20.dp.toPx(),
                    endY = lineY + 20.dp.toPx()
                ),
                topLeft = Offset(left, lineY - 20.dp.toPx()),
                size = ComposeSize(rectWidth, 40.dp.toPx())
            )
        }

        // Draw the corner brackets
        val strokeWidth = 3.dp.toPx()
        val bracketColor = if (isSearching) primaryColor else Color.White.copy(alpha = 0.8f)

        // Top Left
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerSize)
                lineTo(left, top + cornerRadius)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left, top, left + cornerRadius * 2, top + cornerRadius * 2),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerSize, top)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Top Right
        drawPath(
            path = Path().apply {
                moveTo(left + rectWidth - cornerSize, top)
                lineTo(left + rectWidth - cornerRadius, top)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left + rectWidth - cornerRadius * 2, top, left + rectWidth, top + cornerRadius * 2),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + rectWidth, top + cornerSize)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom Left
        drawPath(
            path = Path().apply {
                moveTo(left, top + rectHeight - cornerSize)
                lineTo(left, top + rectHeight - cornerRadius)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left, top + rectHeight - cornerRadius * 2, left + cornerRadius * 2, top + rectHeight),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(left + cornerSize, top + rectHeight)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom Right
        drawPath(
            path = Path().apply {
                moveTo(left + rectWidth, top + rectHeight - cornerSize)
                lineTo(left + rectWidth, top + rectHeight - cornerRadius)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left + rectWidth - cornerRadius * 2, top + rectHeight - cornerRadius * 2, left + rectWidth, top + rectHeight),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + rectWidth - cornerSize, top + rectHeight)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Active scan line
        drawLine(
            color = bracketColor.copy(alpha = 0.5f),
            start = Offset(left, lineY),
            end = Offset(left + rectWidth, lineY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CandidateItemPreview() {
    VaultioTheme {
        CandidateItem(
            card = TcgDexCard(
                id = "swsh1-1",
                localId = "1",
                name = "Bulbasaur",
                image = "https://assets.tcgdex.net/en/swsh/swsh1/1",
                rarity = "Common",
                category = "Pokemon"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ScannerOverlayPreview() {
    VaultioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ScannerOverlay(isSearching = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerContentPermissionPreview() {
    VaultioTheme {
        ScannerContent(
            uiState = ScannerUiState(hasCameraPermission = false),
            onEvent = {},
            onNavigateBack = {},
            onShowBulkSettings = {},
            onShowSkippedReview = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerContentSearchingPreview() {
    VaultioTheme {
        ScannerContent(
            uiState = ScannerUiState(
                hasCameraPermission = true,
                isSearching = true,
                detectedNumber = "123",
                detectedName = "Bulbasaur"
            ),
            onEvent = {},
            onNavigateBack = {},
            onShowBulkSettings = {},
            onShowSkippedReview = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerContentAutoSelectedPreview() {
    VaultioTheme {
        ScannerContent(
            uiState = ScannerUiState(
                hasCameraPermission = true,
                autoSelectedCard = TcgDexCard(
                    id = "swsh1-1",
                    localId = "1",
                    name = "Bulbasaur",
                    image = "https://assets.tcgdex.net/en/swsh/swsh1/1",
                    rarity = "Common",
                    category = "Pokemon"
                )
            ),
            onEvent = {},
            onNavigateBack = {},
            onShowBulkSettings = {},
            onShowSkippedReview = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BulkSettingsPreview() {
    VaultioTheme {
        Surface {
            BulkSettingsSheet(
                uiState = ScannerUiState(
                    bulkDefaults = BulkScanDefaults(
                        condition = "Near Mint",
                        finish = PricingUtils.FINISH_HOLOFOIL
                    )
                ),
                onEvent = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BulkHUDPreview() {
    VaultioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            BulkModeHUD(
                uiState = ScannerUiState(
                    isBulkMode = true,
                    bulkSessionLog = listOf(
                        BulkScanEntry(
                            card = TcgDexCard("1", "1", "Pikachu", null, null, null),
                            status = BulkScanStatus.SAVED,
                            quantity = 1
                        ),
                        BulkScanEntry(
                            card = TcgDexCard("2", "2", "Charizard", null, null, null),
                            status = BulkScanStatus.SAVED,
                            quantity = 1
                        )
                    )
                ),
                onEvent = {},
                onShowSkippedReview = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SkippedReviewPreview() {
    VaultioTheme {
        Surface {
            SkippedReviewSheet(
                uiState = ScannerUiState(
                    skippedCards = listOf(
                        TcgDexCard("1", "1", "Pikachu", null, "Rare", "Pokemon"),
                        TcgDexCard("2", "2", "Bulbasaur", null, "Common", "Pokemon")
                    )
                ),
                onEvent = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PageScanReviewPreview() {
    VaultioTheme {
        Surface {
            PageScanReviewContent(
                uiState = ScannerUiState(
                    isPageScanMode = true,
                    pageScanMode = PageScanMode.REVIEWING,
                    pageScanCells = List(9) { i ->
                        PageScanCell(
                            id = i,
                            row = i / 3,
                            col = i % 3,
                            matchedCard = if (i % 2 == 0) TcgDexCard(
                                id = "$i",
                                localId = "$i",
                                name = "Card $i",
                                image = null,
                                rarity = "Common",
                                category = "Pokemon"
                            ) else null,
                            status = if (i % 2 == 0) PageScanCellStatus.MATCHED else PageScanCellStatus.NOT_FOUND
                        )
                    }
                ),
                onEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PageScanOverlayIdlePreview() {
    VaultioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            PageScanOverlay(isProcessing = false)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PageScanOverlayProcessingPreview() {
    VaultioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            PageScanOverlay(isProcessing = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerContentBulkModePreview() {
    VaultioTheme {
        ScannerContent(
            uiState = ScannerUiState(
                hasCameraPermission = true,
                isBulkMode = true,
                bulkSessionLog = listOf(
                    BulkScanEntry(
                        card = TcgDexCard("1", "1", "Pikachu", null, null, null),
                        status = BulkScanStatus.SAVED,
                        quantity = 1
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {},
            onShowBulkSettings = {},
            onShowSkippedReview = {}
        )
    }
}
