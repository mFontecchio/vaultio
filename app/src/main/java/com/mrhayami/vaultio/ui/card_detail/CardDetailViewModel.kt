package com.mrhayami.vaultio.ui.card_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.FolderCardCrossRef
import com.mrhayami.vaultio.data.repository.VaultioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModel(
    private val repository: VaultioRepository, 
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffects = Channel<CardDetailEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private val userCardId: Long = checkNotNull(savedStateHandle.get<Long>("userCardId")) { 
        "userCardId is required" 
    }

    init {
        observeCardDetails()
    }

    private fun observeCardDetails() {
        viewModelScope.launch {
            repository.getUserCardById(userCardId).flatMapLatest { cardWithDetails ->
                if (cardWithDetails == null) {
                    flowOf(CardDetailUiState(isLoading = false))
                } else {
                    val preferences = repository.userPreferencesRepository
                    combine(
                        repository.allFolders,
                        repository.allFolderCardCrossRefs,
                        preferences.showEnergyAnimations,
                        preferences.showFinishAnimations,
                        preferences.preferSetLogo,
                        repository.getPricesForCard(cardWithDetails.card.id),
                        repository.getVintagePricesForCard(cardWithDetails.card.id)
                    ) { flows: Array<Any?> ->
                        @Suppress("UNCHECKED_CAST")
                        val folders = flows[0] as List<FolderEntity>
                        @Suppress("UNCHECKED_CAST")
                        val crossRefs = flows[1] as List<FolderCardCrossRef>
                        val showEnergyAnims = flows[2] as Boolean
                        val showFinishAnims = flows[3] as Boolean
                        val preferLogo = flows[4] as Boolean
                        @Suppress("UNCHECKED_CAST")
                        val prices = flows[5] as List<PriceEntity>
                        @Suppress("UNCHECKED_CAST")
                        val vintagePrices = flows[6] as List<VintagePriceEntity>

                        val cardFolderIds = crossRefs
                            .filter { it.userCardId == userCardId }
                            .map { it.folderId }
                            .toSet()

                        CardDetailUiState(
                            cardWithDetails = cardWithDetails,
                            folders = folders,
                            cardFolderIds = cardFolderIds,
                            prices = prices,
                            vintagePrices = vintagePrices,
                            showEnergyAnimations = showEnergyAnims,
                            showFinishAnimations = showFinishAnims,
                            preferSetLogo = preferLogo,
                            isLoading = false
                        )
                    }
                }
            }.collect { _uiState.value = it }
        }
    }

    fun onEvent(event: CardDetailEvent) {
        when (event) {
            is CardDetailEvent.SaveChanges -> saveChanges(event.quantity, event.condition, event.printing, event.finish)
            CardDetailEvent.DeleteCard -> deleteUserCard()
            CardDetailEvent.RefreshPrice -> refreshPrice()
            is CardDetailEvent.AddCardToFolder -> addCardToFolder(event.folderId)
            is CardDetailEvent.RemoveCardFromFolder -> removeCardFromFolder(event.folderId)
            CardDetailEvent.ConsumeSaveSuccess -> consumeSaveSuccess()
        }
    }

    private fun saveChanges(quantity: Int, condition: String, printing: String, finish: String) {
        val current = _uiState.value.cardWithDetails?.userCard ?: return
        viewModelScope.launch {
            try {
                repository.updateUserCard(current.copy(
                    quantity = quantity, 
                    condition = condition,
                    printing = printing,
                    finish = finish
                ))
                _uiState.update { it.copy(showSaveSuccess = true) }
            } catch (_: Exception) {
                // Potential for error side effect here
            }
        }
    }

    private fun consumeSaveSuccess() {
        _uiState.update { it.copy(showSaveSuccess = false) }
    }

    private fun deleteUserCard() {
        viewModelScope.launch {
            try {
                repository.deleteUserCard(userCardId)
                _sideEffects.send(CardDetailEffect.Navigation.Back)
            } catch (_: Exception) {
                // Potential for error side effect
            }
        }
    }

    private fun addCardToFolder(folderId: Long) {
        viewModelScope.launch {
            try {
                repository.addCardToFolder(userCardId, folderId)
            } catch (_: Exception) {
                // Potential for error side effect
            }
        }
    }

    private fun removeCardFromFolder(folderId: Long) {
        viewModelScope.launch {
            try {
                repository.removeCardFromFolder(userCardId, folderId)
            } catch (_: Exception) {
                // Potential for error side effect
            }
        }
    }

    private fun refreshPrice() {
        val cardId = _uiState.value.cardWithDetails?.card?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingPrice = true) }
            try {
                repository.updateCardPrice(cardId)
            } finally {
                _uiState.update { it.copy(isRefreshingPrice = false) }
            }
        }
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
