package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object PageScanProcessor {

    suspend fun processPage(fullBitmap: Bitmap): List<PageScanCell> = coroutineScope {
        val cells = mutableListOf<PageScanCell>()
        val width = fullBitmap.width
        val height = fullBitmap.height

        // The fullBitmap is now exactly the grid area (95% width, 1.4x height)
        val cellWidth = width / 3
        val cellHeight = height / 3
        
        // 5% margin relative to cell size to avoid binder pocket edges
        val marginW = (cellWidth * 0.05f).toInt()
        val marginH = (cellHeight * 0.05f).toInt()

        val jobs = (0 until 9).map { index ->
            val row = index / 3
            val col = index % 3
            
            val left = col * cellWidth + marginW
            val top = row * cellHeight + marginH
            val w = (cellWidth - 2 * marginW).coerceAtMost(fullBitmap.width - left)
            val h = (cellHeight - 2 * marginH).coerceAtMost(fullBitmap.height - top)

            val cellBitmap = Bitmap.createBitmap(fullBitmap, left, top, w, h)
            
            async {
                val (lines, pHash) = ScannerUtils.processCell(cellBitmap)
                val ocrResult = lines.joinToString(" ")
                PageScanCell(
                    id = index,
                    row = row,
                    col = col,
                    bitmap = cellBitmap,
                    ocrResult = ocrResult,
                    status = PageScanCellStatus.IDLE
                )
            }
        }

        jobs.awaitAll()
    }
}
