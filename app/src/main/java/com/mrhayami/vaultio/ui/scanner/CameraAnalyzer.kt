package com.mrhayami.vaultio.ui.scanner

import android.graphics.*
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
    private val scanIntervalMs = 500L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanIntervalMs) {
            imageProxy.close()
            return
        }

        // Use built-in toBitmap() from CameraX 1.4.0
        val bitmap = imageProxy.toBitmap()

        // 1. Rotate Bitmap
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        // 2. Center Crop (70% area = 15% margin on all sides)
        val cropMargin = 0.15f
        val width = rotatedBitmap.width
        val height = rotatedBitmap.height
        val left = (width * cropMargin).toInt()
        val top = (height * cropMargin).toInt()
        val right = (width * (1 - cropMargin)).toInt()
        val bottom = (height * (1 - cropMargin)).toInt()
        
        val croppedBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            left,
            top,
            right - left,
            bottom - top
        )

        // 3. Run ML Kit OCR on the cropped bitmap
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                lastScanTime = System.currentTimeMillis()
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        DetectedLine(
                            text = line.text,
                            boundingBox = line.boundingBox,
                            imageWidth = croppedBitmap.width,
                            imageHeight = croppedBitmap.height
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
