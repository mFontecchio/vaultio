package com.mrhayami.vaultio.ui.scanner

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.remote.TcgDexCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class BulkScanDefaults(
    val condition: String = PricingUtils.CONDITION_NM,
    val printing: String = PricingUtils.PRINTING_UNLIMITED,
    val finish: String = PricingUtils.FINISH_NORMAL,
    val folderIds: ImmutableList<Long> = persistentListOf()
)

enum class BulkScanStatus {
    SAVED,
    DUPLICATE_INCREMENTED,
    SKIPPED_AMBIGUOUS
}

@Immutable
data class BulkScanEntry(
    val card: TcgDexCard,
    val timestamp: Long = System.currentTimeMillis(),
    val status: BulkScanStatus,
    val quantity: Int = 1
)
