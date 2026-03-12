package com.mrhayami.vaultio.ui.scanner

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.components.MetadataModal
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    repository: VaultioRepository,
    onNavigateBack: () -> Unit,
    viewModel: ScannerViewModel = viewModel(factory = ScannerViewModelFactory(repository))
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    var selectedCard by remember { mutableStateOf<com.mrhayami.vaultio.data.remote.TcgDexCard?>(null) }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                onLinesDetected = viewModel::onLinesDetected
            )
            
            ScannerOverlay()

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
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Scan Card", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Debug/Detected Info Overlay (Operational Transparency)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OCR Number: ${uiState.detectedNumber ?: "---"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("OCR Name: ${uiState.detectedName ?: "---"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                if (uiState.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.width(100.dp).padding(top = 4.dp))
                }
            }

            // High Confidence Match / Quick Confirm Overlay
            AnimatedVisibility(
                visible = uiState.autoSelectedCard != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Match Found!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ListItem(
                            headlineContent = { Text(uiState.autoSelectedCard?.name ?: "") },
                            supportingContent = { Text(uiState.autoSelectedCard?.id ?: "") },
                            leadingContent = {
                                AsyncImage(
                                    model = "${uiState.autoSelectedCard?.image}/low.webp",
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = viewModel::resumeScanning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject")
                            }
                            Button(
                                onClick = { selectedCard = uiState.autoSelectedCard },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Details")
                            }
                        }
                    }
                }
            }

            // Candidates List (Manual Selection)
            if (uiState.candidates.isNotEmpty() && uiState.autoSelectedCard == null) {
                ModalBottomSheet(onDismissRequest = { viewModel.clearDetectedNumber() }) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        item {
                            Text("Select Match", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                        }
                        items(uiState.candidates) { card ->
                            ListItem(
                                headlineContent = { Text(card.name) },
                                supportingContent = { Text(card.id) },
                                leadingContent = {
                                    AsyncImage(
                                        model = "${card.image}/low.webp",
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )
                                },
                                modifier = Modifier.clickable { selectedCard = card }
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan cards.")
        }
    }

    if (selectedCard != null) {
        ModalBottomSheet(onDismissRequest = { selectedCard = null }) {
            MetadataModal(
                card = selectedCard!!,
                onConfirm = { q, c, p, f ->
                    viewModel.saveScannedCard(selectedCard!!, q, c, p, f)
                    selectedCard = null
                },
                onBack = { selectedCard = null }
            )
        }
    }
}

@Composable
fun CameraPreview(onLinesDetected: (List<DetectedLine>) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Target Resolution 1080p for accuracy as per deep dive
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
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
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
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val rectWidth = canvasWidth * 0.85f
        val rectHeight = rectWidth * 1.397f
        val left = (canvasWidth - rectWidth) / 2
        val top = (canvasHeight - rectHeight) / 2

        // Dark background with cutout
        drawRect(color = Color.Black.copy(alpha = 0.5f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = ComposeSize(rectWidth, rectHeight),
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Frame border
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = ComposeSize(rectWidth, rectHeight),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
