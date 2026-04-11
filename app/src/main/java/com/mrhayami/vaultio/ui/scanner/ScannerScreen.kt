package com.mrhayami.vaultio.ui.scanner

import android.Manifest
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
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

    ScannerContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerContent(
    uiState: ScannerUiState,
    onEvent: (ScannerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.hasCameraPermission) {
            CameraPreview(
                onLinesDetected = { lines, pHash ->
                    onEvent(ScannerEvent.LinesDetected(lines, pHash))
                }
            )
            
            ScannerOverlay(isSearching = uiState.isSearching)

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        "Card Scanner", 
                        color = Color.White, 
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            // HUD / Status Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 115.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = uiState.detectedNumber != null || uiState.isSearching,
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
                visible = uiState.autoSelectedCard != null,
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
                                        modifier = modifier.align(Alignment.CenterVertically)
                                    )
                                    FilledTonalIconButton(
                                        onClick = { onEvent(ScannerEvent.ResumeScanning) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Reject")
                                    }
                                }
//                                Text(
//                                    text = card.name,
//                                    style = MaterialTheme.typography.titleMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    maxLines = 1
//                                )
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
//                                    FilledTonalIconButton(
//                                        onClick = { onEvent(ScannerEvent.ResumeScanning) },
//                                        modifier = Modifier.size(40.dp)
//                                    ) {
//                                        Icon(Icons.Rounded.Close, contentDescription = "Reject")
//                                    }
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

            // Candidates List
            if (uiState.candidates.isNotEmpty() && uiState.autoSelectedCard == null) {
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

@Preview(showBackground = true)
@Composable
private fun CandidateItemPreview() {
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
private fun ScannerOverlayPreview() {
    VaultioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ScannerOverlay(isSearching = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerContentPermissionPreview() {
    VaultioTheme {
        ScannerContent(
            uiState = ScannerUiState(hasCameraPermission = false),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerContentSearchingPreview() {
    VaultioTheme {
        ScannerContent(
            uiState = ScannerUiState(
                hasCameraPermission = true,
                isSearching = true,
                detectedNumber = "123",
                detectedName = "Bulbasaur"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerContentAutoSelectedPreview() {
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
            onNavigateBack = {}
        )
    }
}

@Composable
fun CameraPreview(onLinesDetected: (List<DetectedLine>, Long?) -> Unit) {
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val resolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(1920, 1080),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                keepScreenOn = true
            }
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
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
                        it.setAnalyzer(executor, CameraAnalyzer(onLinesDetected))
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
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
        val canvasWidth = size.width
        val canvasHeight = size.height
        val rectWidth = canvasWidth * 0.85f
        val rectHeight = rectWidth * 1.397f
        val left = (canvasWidth - rectWidth) / 2
        val top = (canvasHeight - rectHeight) / 2
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
