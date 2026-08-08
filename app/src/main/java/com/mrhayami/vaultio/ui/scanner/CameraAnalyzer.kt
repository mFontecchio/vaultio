package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.mrhayami.vaultio.data.PHash

data class DetectedLine(
    val text: String,
    val boundingBox: Rect?,
    val imageWidth: Int,
    val imageHeight: Int
)

class CameraAnalyzer(
    private val viewportAspectRatio: Float,
    private val recognizer: TextRecognizer?,
    private val onLinesDetected: (List<DetectedLine>, Long?, Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastScanTime = 0L
    private val scanIntervalMs = 120L

    private var reusableCropped: Bitmap? = null
    private var reusableEnhanced: Bitmap? = null

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val client = recognizer
        if (client == null) {
            imageProxy.close()
            return
        }

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

        if (rotated != bitmap) bitmap.recycle()

        val cropRect = ScannerGeometry.getCropRect(
            bitmapWidth = rotated.width,
            bitmapHeight = rotated.height,
            viewportWidth = viewportAspectRatio,
            viewportHeight = 1f,
            isPageScanMode = false
        )

        val cropped = if (reusableCropped != null &&
            reusableCropped!!.width == cropRect.width() &&
            reusableCropped!!.height == cropRect.height()
        ) {
            val canvas = android.graphics.Canvas(reusableCropped!!)
            canvas.drawBitmap(
                rotated,
                cropRect,
                Rect(0, 0, cropRect.width(), cropRect.height()),
                null
            )
            reusableCropped!!
        } else {
            reusableCropped?.recycle()
            val newCropped = try {
                Bitmap.createBitmap(
                    rotated,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height()
                )
            } catch (e: Exception) {
                rotated
            }
            reusableCropped = if (newCropped != rotated) newCropped else null
            newCropped
        }

        val pHash = try {
            PHash.computeHash(cropped)
        } catch (e: Exception) {
            null
        }

        val isGlareDetected = ScannerUtils.detectGlare(cropped)

        val enhanced = ScannerUtils.enhanceImage(cropped, reusableEnhanced)
        reusableEnhanced = enhanced

        val image = InputImage.fromBitmap(enhanced, 0)

        client.process(image)
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
                onLinesDetected(lines, pHash, isGlareDetected)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
            .addOnCompleteListener {
                if (rotated != cropped && !rotated.isRecycled) rotated.recycle()
                imageProxy.close()
            }
    }
}
