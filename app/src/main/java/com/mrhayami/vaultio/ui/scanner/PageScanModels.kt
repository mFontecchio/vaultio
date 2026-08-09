package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import com.mrhayami.vaultio.data.remote.TcgDexCard

enum class PageScanMode {
    IDLE,
    CAPTURING,
    PROCESSING,
    REVIEWING
}

enum class PageScanCellStatus {
    IDLE,
    SCANNING,
    MATCHED,
    AMBIGUOUS,
    NOT_FOUND,
    ERROR
}

data class PageScanCell(
    val id: Int, // 0-8 for a 3x3 grid
    val row: Int,
    val col: Int,
    val bitmap: Bitmap? = null,
    val ocrResult: String? = null,
    val matchedCard: TcgDexCard? = null,
    val candidates: List<TcgDexCard> = emptyList(),
    val status: PageScanCellStatus = PageScanCellStatus.IDLE,
    /** Only MATCHED cells start confirmed; ambiguous/missing require explicit user confirm. */
    val isConfirmed: Boolean = false
)

data class PageScanResult(
    val cells: List<PageScanCell> = emptyList(),
    val status: PageScanMode = PageScanMode.IDLE
)
