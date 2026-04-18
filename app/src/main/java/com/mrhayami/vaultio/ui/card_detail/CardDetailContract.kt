package com.mrhayami.vaultio.ui.card_detail

import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity

/** Snapshot of what the UI should render. */
data class CardDetailUiState(
    val cardWithDetails: CardWithDetails? = null,
    val folders: List<FolderEntity> = emptyList(),
    val cardFolderIds: Set<Long> = emptySet(),
    val prices: List<PriceEntity> = emptyList(),
    val vintagePrices: List<VintagePriceEntity> = emptyList(),
    val showEnergyAnimations: Boolean = true,
    val showFinishAnimations: Boolean = true,
    val preferSetLogo: Boolean = true,
    val isLoading: Boolean = true,
    val isRefreshingPrice: Boolean = false,
    val showSaveSuccess: Boolean = false,
    val errorMessage: String? = null
)

/** User actions and UI triggers. */
sealed interface CardDetailEvent {
    data class SaveChanges(
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String
    ) : CardDetailEvent
    data class SplitCard(
        val condition: String,
        val printing: String,
        val finish: String
    ) : CardDetailEvent
    data object DeleteCard : CardDetailEvent
    data object RefreshPrice : CardDetailEvent
    data class AddCardToFolder(val folderId: Long) : CardDetailEvent
    data class RemoveCardFromFolder(val folderId: Long) : CardDetailEvent
    data object ConsumeSaveSuccess : CardDetailEvent
}

/** One-time effects that the Screen reacts to. */
sealed interface CardDetailEffect {
    sealed interface Navigation : CardDetailEffect {
        data object Back : Navigation
        data class ToCard(val userCardId: Long) : Navigation
    }
}
