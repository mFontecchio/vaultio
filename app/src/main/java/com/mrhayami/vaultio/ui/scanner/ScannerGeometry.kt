package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

object ScannerGeometry {
    // Constants for scanning regions
    const val SINGLE_CARD_WIDTH_PCT = 0.85f
    const val SINGLE_CARD_ASPECT_RATIO = 1.397f // Height / Width

    const val BINDER_GRID_WIDTH_PCT = 0.95f
    const val BINDER_GRID_ASPECT_RATIO = 1.4f // Height / Width

    /**
     * Calculates the crop rectangle for a given bitmap based on the viewport aspect ratio.
     * This assumes the bitmap is displayed using FILL_CENTER in the viewport.
     */
    fun getCropRect(
        bitmapWidth: Int,
        bitmapHeight: Int,
        viewportWidth: Float,
        viewportHeight: Float,
        isPageScanMode: Boolean
    ): android.graphics.Rect {
        val screenAR = viewportWidth / viewportHeight
        val bitmapAR = bitmapWidth.toFloat() / bitmapHeight.toFloat()

        val visibleW = if (bitmapAR > screenAR) {
            // Bitmap is fatter than screen, sides are cropped
            bitmapHeight * screenAR
        } else {
            // Bitmap is taller than screen, top/bottom are cropped
            bitmapWidth.toFloat()
        }

        val widthPct = if (isPageScanMode) BINDER_GRID_WIDTH_PCT else SINGLE_CARD_WIDTH_PCT
        val aspectRatio = if (isPageScanMode) BINDER_GRID_ASPECT_RATIO else SINGLE_CARD_ASPECT_RATIO

        val rectW = visibleW * widthPct
        val rectH = rectW * aspectRatio

        val left = (bitmapWidth - rectW) / 2
        val top = (bitmapHeight - rectH) / 2

        return android.graphics.Rect(
            left.toInt().coerceAtLeast(0),
            top.toInt().coerceAtLeast(0),
            (left + rectW).toInt().coerceAtMost(bitmapWidth),
            (top + rectH).toInt().coerceAtMost(bitmapHeight)
        )
    }

    /**
     * Calculates the overlay rectangle for Compose drawing.
     */
    fun getOverlayRect(
        canvasSize: Size,
        isPageScanMode: Boolean
    ): Rect {
        val widthPct = if (isPageScanMode) BINDER_GRID_WIDTH_PCT else SINGLE_CARD_WIDTH_PCT
        val aspectRatio = if (isPageScanMode) BINDER_GRID_ASPECT_RATIO else SINGLE_CARD_ASPECT_RATIO

        val rectWidth = canvasSize.width * widthPct
        val rectHeight = rectWidth * aspectRatio
        val left = (canvasSize.width - rectWidth) / 2
        val top = (canvasSize.height - rectHeight) / 2

        return Rect(left, top, left + rectWidth, top + rectHeight)
    }
}
