package com.mrhayami.vaultio.ui.scanner

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class ScannerMode {
    IDLE,
    SEARCH,
    BULK,
    PAGE,
    GRADING,
    PRICE_CHECK
}

@Immutable
data class PriceCheckInfo(
    val card: TcgDexCard,
    val prices: ImmutableList<PriceEntity> = persistentListOf(),
    val vintagePrices: ImmutableList<VintagePriceEntity> = persistentListOf(),
    val isFetching: Boolean = false,
)
