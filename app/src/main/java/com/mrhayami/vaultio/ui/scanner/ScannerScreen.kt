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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.components.CardAttributeBadges
import com.mrhayami.vaultio.ui.components.DropdownSelector
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import androidx.camera.core.Preview as CameraPreview
import androidx.compose.ui.geometry.Size as ComposeSize

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    repository: VaultioRepository,
    targetUserCardId: Long = -1L,
    onNavigateBack: () -> Unit,
    onNavigateToGrading: (Long, Bitmap, com.mrhayami.vaultio.data.remote.TcgDexCard?) -> Unit,
    viewModel: ScannerViewModel = viewModel(factory = ScannerViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    LaunchedEffect(targetUserCardId) {
        viewModel.onEvent(ScannerEvent.SetTargetUserCard(targetUserCardId))
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScannerEffect.NavigateToGrading -> {
                    effect.capturedImage?.let { bmp ->
                        onNavigateToGrading(effect.userCardId, bmp, effect.pendingCard)
                    } ?: run {
                        // If no image was captured (e.g. they clicked "Grade" HUD button instead of camera button)
                        // Make a fallback or block it? Let's just create a dummy so it doesn't crash since parameter changed
                        onNavigateToGrading(
                            effect.userCardId,
                            Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888),
                            effect.pendingCard
                        )
                    }
                }
            }
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

    val fabOffset by animateDpAsState(
        targetValue = when {
            uiState.isPriceCheckMode && uiState.priceCheckInfo != null -> 460.dp
            uiState.isBulkMode -> 120.dp
            uiState.autoSelectedCard != null && !uiState.isGradingMode -> 180.dp
            else -> 0.dp
        },
        label = "fabOffset"
    )

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
                            onEventStable(ScannerEvent.CapturePhoto(bitmap))
                        }
                    }
                    CameraPreview(
                        isPageScanMode = uiState.isPageScanMode,
                        isGradingMode = uiState.isGradingMode,
                        isTorchEnabled = uiState.isTorchEnabled,
                        autoCaptureTrigger = uiState.autoCaptureTrigger,
                        onLinesDetected = onLinesDetected,
                        onPhotoCaptured = onPhotoCaptured
                    )
                    
                    if (uiState.isPageScanMode) {
                        PageScanOverlay(isProcessing = uiState.pageScanMode == PageScanMode.PROCESSING)
                    } else {
                        ScannerOverlay(
                            isSearching = uiState.isSearching,
                            isGradingMode = uiState.isGradingMode,
                            isPriceCheckMode = uiState.isPriceCheckMode,
                            isTargetDetected = uiState.autoSelectedCard != null || uiState.priceCheckInfo != null
                        )
                    }
                }
            }

            // Header
            AnimatedVisibility(
                visible = uiState.pageScanMode != PageScanMode.REVIEWING,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
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
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
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
                visible = uiState.autoSelectedCard != null && !uiState.isBulkMode && !uiState.isGradingMode && !uiState.isPriceCheckMode,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
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
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 4.dp
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data("${card.image}/low.webp")
                                        .crossfade(true)
                                        .build(),
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

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isGradingMode) {
                                        Button(
                                            onClick = {
                                                onEvent(
                                                    ScannerEvent.SaveAndGrade(
                                                        card = card,
                                                        quantity = 1,
                                                        condition = PricingUtils.CONDITION_NM,
                                                        printing = PricingUtils.PRINTING_UNLIMITED,
                                                        finish = PricingUtils.FINISH_NORMAL,
                                                        folderIds = emptyList()
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF00E676),
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Icon(
                                                Icons.Rounded.AutoFixHigh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Grade",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onEvent(ScannerEvent.ResumeScanning) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                "Cancel",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { onEvent(ScannerEvent.CardSelected(card)) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                "Add Details",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Grading Mode HUD
            AnimatedVisibility(
                visible = uiState.isGradingMode && uiState.autoSelectedCard != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 180.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Found ${uiState.autoSelectedCard?.name}. Press capture to grade.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bulk Mode HUD
            AnimatedVisibility(
                visible = uiState.isBulkMode,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
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

        // Price Check HUD
        AnimatedVisibility(
            visible = uiState.isPriceCheckMode && uiState.priceCheckInfo != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.priceCheckInfo?.let { info ->
                PriceCheckHUD(info = info, onReset = { onEvent(ScannerEvent.ResumeScanning) })
            }
        }

        // Scanner Mode Selector (Expandable FAB)
        if (uiState.hasCameraPermission && uiState.pageScanMode != PageScanMode.REVIEWING) {
            ScannerModeFab(
                activeMode = uiState.activeMode,
                onModeSelected = { onEvent(ScannerEvent.SelectMode(it)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = fabOffset)
                    .navigationBarsPadding()
            )
        }

        // Idle State Overlay
        if (uiState.hasCameraPermission && uiState.activeMode == ScannerMode.IDLE && uiState.pageScanMode != PageScanMode.REVIEWING) {
            ScannerIdleOverlay(onModeSelected = { onEvent(ScannerEvent.SelectMode(it)) })
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
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.name, fontWeight = FontWeight.Bold)
                if (card.rarity?.contains("Promo", ignoreCase = true) == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CardAttributeBadges(
                        finish = PricingUtils.FINISH_NORMAL,
                        printing = PricingUtils.PRINTING_PROMO
                    )
                }
            }
        },
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
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data("${card.image}/low.webp")
                        .crossfade(true)
                        .build(),
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
                val conditions = remember {
                    listOf(
                        com.mrhayami.vaultio.data.PricingUtils.CONDITION_NM,
                        com.mrhayami.vaultio.data.PricingUtils.CONDITION_LP,
                        com.mrhayami.vaultio.data.PricingUtils.CONDITION_MP,
                        com.mrhayami.vaultio.data.PricingUtils.CONDITION_HP,
                        com.mrhayami.vaultio.data.PricingUtils.CONDITION_DMG
                    )
                }
                DropdownSelector(
                    "Default Condition",
                    uiState.bulkDefaults.condition,
                    conditions
                ) {
                    onEvent(ScannerEvent.SetBulkDefaults(uiState.bulkDefaults.copy(condition = it)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                val printings = remember {
                    listOf(
                        com.mrhayami.vaultio.data.PricingUtils.PRINTING_UNLIMITED,
                        com.mrhayami.vaultio.data.PricingUtils.PRINTING_SHADOWLESS,
                        com.mrhayami.vaultio.data.PricingUtils.PRINTING_PROMO,
                        com.mrhayami.vaultio.data.PricingUtils.PRINTING_1ST_EDITION
                    )
                }
                DropdownSelector(
                    "Default Printing",
                    uiState.bulkDefaults.printing,
                    printings
                ) {
                    onEvent(ScannerEvent.SetBulkDefaults(uiState.bulkDefaults.copy(printing = it)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                val finishes = remember {
                    listOf(
                        com.mrhayami.vaultio.data.PricingUtils.FINISH_NORMAL,
                        com.mrhayami.vaultio.data.PricingUtils.FINISH_HOLOFOIL,
                        com.mrhayami.vaultio.data.PricingUtils.FINISH_REVERSE_HOLO,
                        com.mrhayami.vaultio.data.PricingUtils.FINISH_TEXTURED,
                        com.mrhayami.vaultio.data.PricingUtils.FINISH_GOLD
                    )
                }
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
    val lastSaved = remember(uiState.bulkSessionLog) {
        uiState.bulkSessionLog.lastOrNull { it.status != BulkScanStatus.SKIPPED_AMBIGUOUS }
    }
    val skippedCount = remember(uiState.skippedCards) {
        uiState.skippedCards.distinctBy { it.id }.size
    }
    val totalScanned = remember(uiState.bulkSessionLog) {
        uiState.bulkSessionLog.filter { it.status != BulkScanStatus.SKIPPED_AMBIGUOUS }
            .sumOf { it.quantity }
    }

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
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data("${lastSaved.card.image}/low.webp")
                                .crossfade(true)
                                .build(),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
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
    val skippedCards = remember(uiState.skippedCards) {
        uiState.skippedCards.distinctBy { it.id }
    }

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
            modifier = Modifier
                .padding(8.dp)
                .size(20.dp)
        )
    }
}


@Composable
fun CameraPreview(
    isPageScanMode: Boolean = false,
    isGradingMode: Boolean = false,
    isTorchEnabled: Boolean = false,
    autoCaptureTrigger: Long = 0L,
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

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var showFlash by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(isTorchEnabled, cameraControl) {
        cameraControl?.enableTorch(isTorchEnabled)
    }

    LaunchedEffect(autoCaptureTrigger) {
        if (autoCaptureTrigger > 0L && imageCapture != null) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            showFlash = true
            imageCapture?.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.toBitmap()
                        val rotation = image.imageInfo.rotationDegrees
                        val matrix =
                            android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrix,
                            true
                        )

                        val pv = previewViewRef ?: return
                        val cropRect = ScannerGeometry.getCropRect(
                            bitmapWidth = rotatedBitmap.width,
                            bitmapHeight = rotatedBitmap.height,
                            viewportWidth = pv.width.toFloat(),
                            viewportHeight = pv.height.toFloat(),
                            isPageScanMode = isPageScanMode
                        )

                        val cropped = try {
                            val sub = Bitmap.createBitmap(
                                rotatedBitmap,
                                cropRect.left,
                                cropRect.top,
                                cropRect.width(),
                                cropRect.height()
                            )
                            // Create a deep copy to ensure independence from CameraX buffers
                            sub.copy(sub.config ?: Bitmap.Config.ARGB_8888, false)
                        } catch (e: Exception) {
                            rotatedBitmap.copy(
                                rotatedBitmap.config ?: Bitmap.Config.ARGB_8888,
                                false
                            )
                        }

                        showFlash = false
                        onPhotoCaptured(cropped)
                        image.close()
                    }

                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                        showFlash = false
                        Toast.makeText(
                            context,
                            "Failed to capture: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
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

    LaunchedEffect(isPageScanMode, isGradingMode, lifecycleOwner, previewViewRef) {
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
                if (isPageScanMode || isGradingMode) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                        imageAnalysis // allow scanning while showing button
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

        if (isPageScanMode || isGradingMode) {
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
                                
                                val pv = previewViewRef ?: return
                                val cropRect = ScannerGeometry.getCropRect(
                                    bitmapWidth = rotatedBitmap.width,
                                    bitmapHeight = rotatedBitmap.height,
                                    viewportWidth = pv.width.toFloat(),
                                    viewportHeight = pv.height.toFloat(),
                                    isPageScanMode = isPageScanMode
                                )
                                
                                val cropped = try {
                                    val sub = Bitmap.createBitmap(
                                        rotatedBitmap,
                                        cropRect.left,
                                        cropRect.top,
                                        cropRect.width(),
                                        cropRect.height()
                                    )
                                    // Deep copy for UI safety
                                    sub.copy(sub.config ?: Bitmap.Config.ARGB_8888, false)
                                } catch (e: Exception) {
                                    rotatedBitmap.copy(
                                        rotatedBitmap.config ?: Bitmap.Config.ARGB_8888, false
                                    )
                                }
                                
                                onPhotoCaptured(cropped)
                                image.close()
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
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data("${cell.matchedCard.image}/low.webp")
                        .crossfade(true)
                        .build(),
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
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
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
fun ScannerOverlay(
    isSearching: Boolean,
    isGradingMode: Boolean = false,
    isPriceCheckMode: Boolean = false,
    isTargetDetected: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scannerLineAnimState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerLine"
    )

    val primaryColor by animateColorAsState(
        targetValue = when {
            isGradingMode && isTargetDetected -> Color(0xFF00E676)
            isGradingMode -> Color(0xFFFFC107)
            isPriceCheckMode && isTargetDetected -> Color(0xFF00E676)
            isPriceCheckMode -> Color(0xFF00B0FF)
            isSearching -> MaterialTheme.colorScheme.primary
            else -> Color.White.copy(alpha = 0.8f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "primaryColor"
    )

    val secondaryColor =
        if (isGradingMode) Color(0xFF00B0FF) else MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val scannerLineAnim = scannerLineAnimState.value
        val rect = ScannerGeometry.getOverlayRect(size, isPageScanMode = false)
        val rectWidth = rect.width
        val rectHeight = rect.height
        val left = rect.left
        val top = rect.top
        val cornerRadius = 24.dp.toPx()
        val cornerSize = 40.dp.toPx()

        // Background mask
        drawRect(color = Color.Black.copy(alpha = 0.6f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = ComposeSize(rectWidth, rectHeight),
            cornerRadius = CornerRadius(cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Phase 2: AI Safe Zone AR Overlay
        if (isGradingMode) {
            // Draw "Safe Zone" inner guide
            val inset = 20.dp.toPx()
            drawRoundRect(
                color = secondaryColor.copy(alpha = 0.2f),
                topLeft = Offset(left + inset, top + inset),
                size = ComposeSize(rectWidth - (inset * 2), rectHeight - (inset * 2)),
                cornerRadius = CornerRadius(cornerRadius / 2),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Centering Crosshair
            val crossSize = 15.dp.toPx()
            drawLine(
                color = secondaryColor.copy(alpha = 0.4f),
                start = Offset(left + (rectWidth / 2) - crossSize, top + (rectHeight / 2)),
                end = Offset(left + (rectWidth / 2) + crossSize, top + (rectHeight / 2)),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = secondaryColor.copy(alpha = 0.4f),
                start = Offset(left + (rectWidth / 2), top + (rectHeight / 2) - crossSize),
                end = Offset(left + (rectWidth / 2), top + (rectHeight / 2) + crossSize),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Draw scanning line
        val lineY = top + (rectHeight * scannerLineAnim)
        if (isSearching || isGradingMode) {
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
        val bracketColor = primaryColor

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
            ScannerOverlay(isSearching = true, isPriceCheckMode = false)
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
                    activeMode = ScannerMode.BULK,
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
                    activeMode = ScannerMode.PAGE,
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
                activeMode = ScannerMode.BULK,
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


@Composable
fun ScannerModeFab(
    activeMode: ScannerMode,
    onModeSelected: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val modes = listOf(
        ScannerMode.SEARCH to ("Search" to Icons.Rounded.Search),
        ScannerMode.PRICE_CHECK to ("Price Check" to Icons.Rounded.Payments),
        ScannerMode.BULK to ("Bulk Scan" to Icons.Rounded.Layers),
        ScannerMode.PAGE to ("Page Scan" to Icons.Rounded.GridView),
        ScannerMode.GRADING to ("AI Grading" to Icons.Rounded.AutoFixHigh)
    )

    val currentModeInfo = modes.find { it.first == activeMode }
    val currentIcon = currentModeInfo?.second?.second ?: Icons.Rounded.PhotoCamera
    val currentLabel = currentModeInfo?.second?.first ?: "Select Mode"

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                modes.forEach { (mode, info) ->
                    if (mode != activeMode) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                onModeSelected(mode)
                                expanded = false
                            },
                            icon = { Icon(info.second, contentDescription = null) },
                            text = { Text(info.first) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { expanded = !expanded },
            icon = {
                Icon(
                    if (expanded) Icons.Rounded.Close else currentIcon,
                    contentDescription = null
                )
            },
            text = { Text(if (expanded) "Close" else currentLabel) },
            containerColor = if (activeMode == ScannerMode.IDLE) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            contentColor = if (activeMode == ScannerMode.IDLE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            expanded = expanded || activeMode != ScannerMode.IDLE
        )
    }
}


@Composable
fun ScannerIdleOverlay(
    onModeSelected: (ScannerMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Select a scanning mode to begin",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Pause active scanning to save battery and focus on what you need.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onModeSelected(ScannerMode.SEARCH) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Search")
                }
                Button(
                    onClick = { onModeSelected(ScannerMode.PRICE_CHECK) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Price Check")
                }
            }
        }
    }
}


@Composable
fun PriceCheckHUD(
    info: PriceCheckInfo,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .heightIn(max = 450.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data("${info.card.image}/low.webp")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp, 84.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = info.card.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(
                    onClick = onReset,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            if (info.isFetching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else if (info.prices.isEmpty() && info.vintagePrices.isEmpty()) {
                Text(
                    "No pricing data available for this card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    // Standard Prices
                    info.prices
                        .sortedWith(compareBy({ it.finish }, { it.condition }))
                        .forEach { price ->
                            PriceRow(
                                label = "${price.finish.replaceFirstChar { it.uppercase() }} • ${price.condition}",
                                marketPrice = price.marketPrice,
                                lowPrice = price.lowPrice,
                                highPrice = price.highPrice
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.3f
                                )
                            )
                        }

                    // Vintage Prices
                    info.vintagePrices
                        .sortedWith(compareBy({ it.printing }, { it.finish }, { it.condition }))
                        .forEach { price ->
                            PriceRow(
                                label = "${price.printing.replaceFirstChar { it.uppercase() }} ${price.finish.replaceFirstChar { it.uppercase() }} • ${price.condition}",
                                marketPrice = price.marketPrice,
                                lowPrice = price.lowPrice,
                                highPrice = price.highPrice
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.3f
                                )
                            )
                        }
                }
            }
        }
    }
}


@Composable
fun PriceRow(
    label: String,
    marketPrice: Double?,
    lowPrice: Double?,
    highPrice: Double?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (lowPrice != null && highPrice != null) {
                Text(
                    text = "Range: $${String.format(Locale.US, "%.2f", lowPrice)} - $${
                        String.format(
                            Locale.US,
                            "%.2f",
                            highPrice
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = marketPrice?.let { "$${String.format(Locale.US, "%.2f", it)}" } ?: "N/A",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PriceRowPreview() {
    VaultioTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                PriceRow(
                    label = "Holo • Near Mint",
                    marketPrice = 12.50,
                    lowPrice = 10.00,
                    highPrice = 15.00
                )
                PriceRow(
                    label = "Normal • Lightly Played",
                    marketPrice = 5.25,
                    lowPrice = 4.50,
                    highPrice = 6.00
                )
                PriceRow(
                    label = "Secret Rare • Mint",
                    marketPrice = null,
                    lowPrice = null,
                    highPrice = null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerModeFabPreview() {
    VaultioTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            ScannerModeFab(
                activeMode = ScannerMode.SEARCH,
                onModeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PriceCheckHUDPreview() {
    VaultioTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            PriceCheckHUD(
                info = PriceCheckInfo(
                    card = TcgDexCard(
                        id = "swsh1-1",
                        localId = "1",
                        name = "Bulbasaur",
                        image = "https://assets.tcgdex.net/en/swsh/swsh1/1",
                        rarity = "Common",
                        category = "Pokemon"
                    ),
                    prices = listOf(
                        PriceEntity(
                            cardId = "swsh1-1",
                            finish = "Holo",
                            condition = "Near Mint",
                            marketPrice = 15.0,
                            lowPrice = 12.0,
                            midPrice = 14.0,
                            highPrice = 18.0,
                            source = "tcgdex"
                        ),
                        PriceEntity(
                            cardId = "swsh1-1",
                            finish = "Normal",
                            condition = "Lightly Played",
                            marketPrice = 5.0,
                            lowPrice = 4.0,
                            midPrice = 4.5,
                            highPrice = 6.0,
                            source = "tcgdex"
                        )
                    ),
                    isFetching = false
                ),
                onReset = {}
            )
        }
    }
}






