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
    private val scanIntervalMs = 250L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanIntervalMs) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Compute the logical (post-rotation) dimensions.
        // These are used by the ViewModel to perform spatial filtering (top/bottom zones).
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
