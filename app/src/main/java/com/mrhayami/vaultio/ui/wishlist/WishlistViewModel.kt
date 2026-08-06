package com.mrhayami.vaultio.ui.wishlist

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.WishlistCardEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val repository: VaultioRepository
) : MviViewModel<WishlistUiState, WishlistEvent, WishlistEffect>(
    initialState = WishlistUiState()
) {

    init {
        combine(
            repository.allWishlistCards,
            repository.allPrices,
            repository.allVintagePrices
        ) { wishlist, prices, vintagePrices ->
            val priceMap = prices.associateBy { "${it.cardId}_${it.finish}_${it.condition}" }
            val vintagePriceMap =
                vintagePrices.associateBy { "${it.cardId}_${it.finish}_${it.condition}_${it.printing}" }

            val uiItems = wishlist.map { item ->
                val cardId = item.wishlistCard.cardId
                val finish = item.wishlistCard.finish
                val condition = item.wishlistCard.condition
                val printing = item.wishlistCard.printing

                val price = priceMap["${cardId}_${finish}_${condition}"]?.marketPrice
                    ?: vintagePriceMap["${cardId}_${finish}_${condition}_${printing}"]?.marketPrice
                    ?: 0.0

                WishlistItemUiModel(details = item, price = price)
            }

            val totalValue = uiItems.sumOf { it.price * it.details.wishlistCard.quantity }

            updateState {
                copy(
                    wishlistItems = uiItems,
                    totalWishlistValue = totalValue,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: WishlistEvent) {
        when (event) {
            is WishlistEvent.SearchRemoteCards -> searchRemoteCards(event.query)
            is WishlistEvent.AddCardToWishlist -> addCardToWishlist(
                event.card,
                event.quantity,
                event.condition,
                event.printing,
                event.finish
            )

            is WishlistEvent.RemoveFromWishlist -> removeFromWishlist(event.id)
            is WishlistEvent.MoveToCollection -> moveToCollection(event.id, event.position)
        }
    }

    private fun searchRemoteCards(query: String) {
        viewModelScope.launch {
            updateState { copy(isSearching = true) }
            val results = repository.searchTcgDex(query)
            updateState { copy(searchResults = results, isSearching = false) }
        }
    }

    private fun addCardToWishlist(
        card: TcgDexCard,
        quantity: Int,
        condition: String,
        printing: String,
        finish: String
    ) {
        viewModelScope.launch {
            repository.addCardToWishlist(
                card,
                WishlistCardEntity(
                    cardId = card.id,
                    quantity = quantity,
                    condition = condition,
                    printing = printing,
                    finish = finish
                )
            )
            emitEffect(WishlistEffect.ShowToast("Added to wishlist"))
        }
    }

    private fun removeFromWishlist(id: Long) {
        viewModelScope.launch {
            repository.removeCardFromWishlist(id)
            emitEffect(WishlistEffect.ShowToast("Removed from wishlist"))
        }
    }

    private fun moveToCollection(id: Long, position: Offset) {
        viewModelScope.launch {
            repository.moveWishlistCardToCollection(id)
            emitEffect(WishlistEffect.ShowFanfare(position))
            emitEffect(WishlistEffect.ShowToast("Moved to collection"))
        }
    }
}

class WishlistViewModelFactory(
    private val repository: VaultioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WishlistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WishlistViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
