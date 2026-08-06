package com.mrhayami.vaultio.ui.scanner

import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard

enum class ScannerMode {
    IDLE,
    SEARCH,
    BULK,
    PAGE,
    GRADING,
    PRICE_CHECK
}

data class PriceCheckInfo(
    val card: TcgDexCard,
    val prices: List<PriceEntity> = emptyList(),
    val vintagePrices: List<VintagePriceEntity> = emptyList(),
    val isFetching: Boolean = false,
)
