package com.mrhayami.vaultio.ui.scanner

import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.remote.TcgDexCard

data class BulkScanDefaults(
    val condition: String = PricingUtils.CONDITION_NM,
    val printing: String = PricingUtils.PRINTING_UNLIMITED,
    val finish: String = PricingUtils.FINISH_NORMAL,
    val folderIds: List<Long> = emptyList()
)

enum class BulkScanStatus {
    SAVED,
    DUPLICATE_INCREMENTED,
    SKIPPED_AMBIGUOUS
}

data class BulkScanEntry(
    val card: TcgDexCard,
    val timestamp: Long = System.currentTimeMillis(),
    val status: BulkScanStatus,
    val quantity: Int = 1
)
