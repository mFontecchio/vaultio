package com.mrhayami.vaultio.ui.card_detail

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/** Snapshot of what the UI should render. */
@Immutable
data class CardDetailUiState(
    val cardWithDetails: CardWithDetails? = null,
    val folders: ImmutableList<FolderEntity> = persistentListOf(),
    val cardFolderIds: ImmutableSet<Long> = persistentSetOf(),
    val prices: ImmutableList<PriceEntity> = persistentListOf(),
    val vintagePrices: ImmutableList<VintagePriceEntity> = persistentListOf(),
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
    data class GradeCard(val image: android.graphics.Bitmap? = null) : CardDetailEvent
}

/** One-time effects that the Screen reacts to. */
sealed interface CardDetailEffect {
    sealed interface Navigation : CardDetailEffect {
        data object Back : Navigation
        data class ToCard(val userCardId: Long) : Navigation
        data class ToGrading(val userCardId: Long, val image: android.graphics.Bitmap? = null) :
            Navigation
    }
}
