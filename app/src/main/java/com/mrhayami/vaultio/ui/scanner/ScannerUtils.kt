package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.mrhayami.vaultio.data.PHash
import kotlinx.coroutines.tasks.await

object ScannerUtils {

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

    suspend fun processCell(bitmap: Bitmap): Pair<List<DetectedLine>, Long?> {
        val pHash = try {
            PHash.computeHash(bitmap)
        } catch (e: Exception) {
            null
        }

        val client = when (val state = TextRecognizerProvider.getOrCreate()) {
            is TextRecognizerState.Available -> state.client
            TextRecognizerState.Unavailable -> return emptyList<DetectedLine>() to pHash
        }

        val enhanced = enhanceImage(bitmap)
        return try {
            recognizeCard(enhanced, client) to pHash
        } catch (e: Exception) {
            emptyList<DetectedLine>() to pHash
        }
    }

    /**
     * Reads the whole card, then the bottom band again at 2x. Vintage collector
     * numbers are a few pixels tall at camera scale, and the second pass is what
     * recovered them on the research corpus (about 92% of number/total reads).
     * Band boxes are mapped back onto the full card so the zone fractions still apply.
     */
    fun recognizeCard(card: Bitmap, client: TextRecognizer): List<DetectedLine> {
        val full = readLines(client, card, offsetY = 0, scale = 1f, card.width, card.height)
        val bandTop = (card.height * 0.80f).toInt().coerceIn(0, (card.height - 1).coerceAtLeast(0))
        val bandHeight = card.height - bandTop
        if (bandHeight < 8 || card.width < 8) return full
        val band = Bitmap.createBitmap(card, 0, bandTop, card.width, bandHeight)
        val scaled = Bitmap.createScaledBitmap(band, band.width * 2, band.height * 2, true)
        val extra = try {
            readLines(client, scaled, offsetY = bandTop, scale = 2f, card.width, card.height)
        } finally {
            if (scaled != band) scaled.recycle()
            band.recycle()
        }
        return full + extra
    }

    private fun readLines(
        client: TextRecognizer,
        bitmap: Bitmap,
        offsetY: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectedLine> {
        val visionText = Tasks.await(client.process(InputImage.fromBitmap(bitmap, 0)))
        return visionText.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull DetectedLine(
                    text = line.text,
                    boundingBox = null,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                )
                DetectedLine(
                    text = line.text,
                    boundingBox = android.graphics.Rect(
                        (box.left / scale).toInt(),
                        offsetY + (box.top / scale).toInt(),
                        (box.right / scale).toInt(),
                        offsetY + (box.bottom / scale).toInt()
                    ),
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                )
            }
        }
    }
}
