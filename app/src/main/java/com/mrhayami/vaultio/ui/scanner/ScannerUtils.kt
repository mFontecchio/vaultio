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

    fun enhanceImage(src: Bitmap): Bitmap {
        val contrast = 1.4f
        val brightness = 10f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val ret = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return ret
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
