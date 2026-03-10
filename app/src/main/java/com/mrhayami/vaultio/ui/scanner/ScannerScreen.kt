package com.mrhayami.vaultio.ui.scanner

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.collection.MetadataModal
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
                onTextRecognized = viewModel::onTextDetected
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
                Text("Scan Card Number", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(48.dp)) // For balance
            }

            // Debug/Detected Info
            if (uiState.detectedNumber != null || uiState.isSearching) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 150.dp)
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Searching for ${uiState.detectedNumber}...", modifier = Modifier.padding(top = 8.dp))
                        } else {
                            Text("Detected: ${uiState.detectedNumber}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            }

            // Candidates
            if (uiState.candidates.isNotEmpty()) {
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
                                        model = card.image,
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
                    // Reusing logic from CollectionViewModel or calling repository directly
                    // For brevity, we'll just print for now or assume a shared VM
                    selectedCard = null
                    onNavigateBack()
                },
                onBack = { selectedCard = null }
            )
        }
    }
}

@Composable
fun CameraPreview(onTextRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, CameraAnalyzer(onTextRecognized))
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
        val rectWidth = canvasWidth * 0.8f
        val rectHeight = rectWidth * 0.718f // Card aspect ratio
        val left = (canvasWidth - rectWidth) / 2
        val top = (canvasHeight - rectHeight) / 2

        // Dark background with cutout
        drawRect(color = Color.Black.copy(alpha = 0.5f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(12.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Frame border
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
