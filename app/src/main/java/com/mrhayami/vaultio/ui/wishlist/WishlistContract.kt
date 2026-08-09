package com.mrhayami.vaultio.ui.wishlist

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.WishlistCardWithDetails
import com.mrhayami.vaultio.data.remote.TcgDexCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class WishlistItemUiModel(
    val details: WishlistCardWithDetails,
    val price: Double
)

@Immutable
data class WishlistUiState(
    val isLoading: Boolean = true,
    val wishlistItems: ImmutableList<WishlistItemUiModel> = persistentListOf(),
    val isSearching: Boolean = false,
    val searchResults: ImmutableList<TcgDexCard> = persistentListOf(),
    val totalWishlistValue: Double = 0.0
)

sealed class WishlistEvent {
    data class SearchRemoteCards(val query: String) : WishlistEvent()
    data class AddCardToWishlist(
        val card: TcgDexCard,
        val quantity: Int,
        val condition: String,
        val printing: String,
        val finish: String
    ) : WishlistEvent()

    data class RemoveFromWishlist(val id: Long) : WishlistEvent()
    data class MoveToCollection(val id: Long) : WishlistEvent()
}

sealed class WishlistEffect {
    data class ShowToast(val message: String) : WishlistEffect()
}
