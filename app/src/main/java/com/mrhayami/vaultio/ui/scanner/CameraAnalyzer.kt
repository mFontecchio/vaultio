package com.mrhayami.vaultio.ui.scanner

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class DetectedLine(
    val text: String,
    val boundingBox: Rect?,
    val imageWidth: Int,
    val imageHeight: Int
)

class CameraAnalyzer(
    private val onLinesDetected: (List<DetectedLine>) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastScanTime = 0L
    // Reduced from 500ms → 250ms: feeds the consensus buffer faster and improves
    // time-to-match while the zero-allocation fromMediaImage path keeps CPU load low.
    private val scanIntervalMs = 250L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanIntervalMs) {
            imageProxy.close()
            return
        }

        // Null-guard: some device/OS combos return a null mediaImage on certain frames.
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // fromMediaImage lets ML Kit operate directly on the native YUV_420_888 buffer —
        // higher OCR fidelity than the lossy YUV→RGB bitmap conversion, and zero heap
        // allocations (no toBitmap / rotation matrix / crop bitmaps).
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Compute the logical (post-rotation) dimensions so that spatial filtering in the
        // ViewModel can still reason about top/bottom percentages correctly.
        val (logicalWidth, logicalHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
            mediaImage.height to mediaImage.width
        } else {
            mediaImage.width to mediaImage.height
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                lastScanTime = System.currentTimeMillis()
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        DetectedLine(
                            text = line.text,
                            boundingBox = line.boundingBox,
                            imageWidth = logicalWidth,
                            imageHeight = logicalHeight
                        )
                    }
                }
                onLinesDetected(lines)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
