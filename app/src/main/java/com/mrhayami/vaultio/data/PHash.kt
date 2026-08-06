package com.mrhayami.vaultio.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * A simple implementation of Perceptual Hashing (specifically dHash - Difference Hash)
 * to allow comparing card images for disambiguation.
 */
object PHash {
    private const val HASH_SIZE = 8

    /**
     * Computes a 64-bit dHash of a bitmap.
     * The process involves:
     * 1. Shrinking to 9x8 (for an 8x8 difference map).
     * 2. Converting to grayscale.
     * 3. Computing differences between adjacent pixels.
     */
    fun computeHash(bitmap: Bitmap): Long {
        // 1. Resize to 9x8
        val resized = Bitmap.createScaledBitmap(bitmap, HASH_SIZE + 1, HASH_SIZE, true)
        
        // 2. Grayscale
        val grayscale = toGrayscale(resized)
        
        // 3. Compute differences
        var hash = 0L
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                val left = grayscale.getPixel(x, y) and 0xFF
                val right = grayscale.getPixel(x + 1, y) and 0xFF
                if (left > right) {
                    hash = hash or (1L shl (y * HASH_SIZE + x))
                }
            }
        }

        // Bitmaps will be garbage collected
        return hash
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dst
    }

    /**
     * Calculates the Hamming distance between two hashes.
     * Lower distance means higher similarity. 
     * Distance < 10 is usually a very strong match.
     */
    fun hammingDistance(h1: Long, h2: Long): Int {
        var x = h1 xor h2
        var dist = 0
        while (x != 0L) {
            dist++
            x = x and (x - 1)
        }
        return dist
    }
}
