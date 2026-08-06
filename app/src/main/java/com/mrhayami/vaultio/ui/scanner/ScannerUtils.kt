package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mrhayami.vaultio.data.PHash
import kotlinx.coroutines.tasks.await

object ScannerUtils {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun enhanceImage(src: Bitmap, dest: Bitmap? = null): Bitmap {
        val contrast = 1.4f
        val brightness = 10f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        val ret =
            if (dest != null && dest.width == src.width && dest.height == src.height && !dest.isRecycled) {
                dest
            } else {
                Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
            }

        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return ret
    }

    /**
     * Detects if there is excessive glare in the bitmap.
     * Looks for clusters of pixels with brightness near 255.
     */
    fun detectGlare(bitmap: Bitmap): Boolean {
        // Performance: Check a scaled-down version
        val scaledWidth = 64
        val scaledHeight = (bitmap.height * (scaledWidth.toFloat() / bitmap.width)).toInt()
        val small = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        val pixels = IntArray(scaledWidth * scaledHeight)
        small.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

        var glareCount = 0
        val threshold = 245 // Brightness threshold for glare

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Simple brightness: average
            val brightness = (r + g + b) / 3
            if (brightness > threshold) {
                glareCount++
            }
        }

        small.recycle()

        // If more than 8% of the ROI is "blown out", it's likely glare
        return glareCount > (scaledWidth * scaledHeight * 0.08)
    }

    suspend fun processCell(bitmap: Bitmap): Pair<List<String>, Long?> {
        val pHash = try {
            PHash.computeHash(bitmap)
        } catch (e: Exception) {
            null
        }

        val enhanced = enhanceImage(bitmap)
        val image = InputImage.fromBitmap(enhanced, 0)

        return try {
            val visionText = recognizer.process(image).await()
            val lines = visionText.textBlocks.flatMap { block ->
                block.lines.map { it.text }
            }
            lines to pHash
        } catch (e: Exception) {
            emptyList<String>() to pHash
        }
    }
}
