package com.mrhayami.vaultio.ui.scanner

import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mrhayami.vaultio.data.PHash

data class DetectedLine(
    val text: String,
    val boundingBox: Rect?,
    val imageWidth: Int,
    val imageHeight: Int
)

class CameraAnalyzer(
    private val viewportAspectRatio: Float,
    private val onLinesDetected: (List<DetectedLine>, Long?) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastScanTime = 0L
    private val scanIntervalMs = 120L 

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanIntervalMs) {
            imageProxy.close()
            return
        }

        val bitmap = try {
            imageProxy.toBitmap()
        } catch (e: Exception) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        // Viewport Alignment: Calculate visible area of the bitmap based on screen aspect ratio
        val screenAR = viewportAspectRatio
        val bitmapAR = rotated.width.toFloat() / rotated.height.toFloat()
        
        val (visibleW, _) = if (bitmapAR > screenAR) {
            // Bitmap is fatter than screen, sides are cropped in FILL_CENTER
            (rotated.height * screenAR) to rotated.height.toFloat()
        } else {
            // Bitmap is taller than screen, top/bottom are cropped
            rotated.width.toFloat() to (rotated.width / screenAR)
        }
        
        val rectW = visibleW * 0.85f
        val rectH = rectW * 1.397f
        val left = (rotated.width - rectW) / 2
        val top = (rotated.height - rectH) / 2
        
        val cropped = try {
            Bitmap.createBitmap(
                rotated,
                left.toInt().coerceAtLeast(0),
                top.toInt().coerceAtLeast(0),
                rectW.toInt().coerceAtMost(rotated.width - left.toInt().coerceAtLeast(0)),
                rectH.toInt().coerceAtMost(rotated.height - top.toInt().coerceAtLeast(0))
            )
        } catch (e: Exception) {
            rotated
        }

        // Compute perceptual hash for disambiguation
        val pHash = try {
            PHash.computeHash(cropped)
        } catch (e: Exception) {
            null
        }

        val enhanced = ScannerUtils.enhanceImage(cropped)
        val image = InputImage.fromBitmap(enhanced, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                lastScanTime = System.currentTimeMillis()
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        DetectedLine(
                            text = line.text,
                            boundingBox = line.boundingBox,
                            imageWidth = enhanced.width,
                            imageHeight = enhanced.height
                        )
                    }
                }
                onLinesDetected(lines, pHash)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
            .addOnCompleteListener {
                if (rotated != bitmap) rotated.recycle()
                if (cropped != rotated && cropped != enhanced) cropped.recycle()
                if (enhanced != cropped) enhanced.recycle()
                bitmap.recycle()
                imageProxy.close()
            }
    }
}
