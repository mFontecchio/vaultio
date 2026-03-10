package com.mrhayami.vaultio.ui.card_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class CardDetailUiState(
    val cardWithDetails: CardWithDetails? = null,
    val folders: List<FolderEntity> = emptyList(),
    val prices: List<PriceEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModel(
    private val repository: VaultioRepository, 
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val userCardId: Long = savedStateHandle.get<Long>("userCardId")!!

    init {
        viewModelScope.launch {
            repository.getUserCardById(userCardId).flatMapLatest { cardWithDetails ->
                combine(
                    repository.allFolders,
                    if (cardWithDetails != null) repository.getPricesForCard(cardWithDetails.card.id) 
                    else flowOf(emptyList<PriceEntity>())
                ) { folders, prices ->
                    CardDetailUiState(
                        cardWithDetails = cardWithDetails,
                        folders = folders,
                        prices = prices,
                        isLoading = false
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun saveChanges(quantity: Int, condition: String, printing: String, finish: String) {
        val current = _uiState.value.cardWithDetails?.userCard ?: return
        viewModelScope.launch {
            repository.updateUserCard(current.copy(
                quantity = quantity, 
                condition = condition,
                printing = printing,
                finish = finish
            ))
        }
    }

    fun deleteUserCard() {
        viewModelScope.launch {
            repository.deleteUserCard(userCardId)
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }

    fun addCardToFolder(folderId: Long) {
        viewModelScope.launch {
            repository.addCardToFolder(userCardId, folderId)
        }
    }

    fun removeCardFromFolder(folderId: Long) {
        viewModelScope.launch {
            repository.removeCardFromFolder(userCardId, folderId)
        }
    }

    fun refreshPrice() {
        // Trigger PriceUpdateWorker or direct repository call
    }
}

class CardDetailViewModelFactory(
    private val repository: VaultioRepository, 
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardDetailViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
