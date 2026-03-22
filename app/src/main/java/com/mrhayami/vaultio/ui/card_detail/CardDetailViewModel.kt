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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val isDeleted: Boolean = false,
    val showSaveSuccess: Boolean = false,
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
                if (cardWithDetails == null) {
                    flowOf(CardDetailUiState(isLoading = false))
                } else {
                    combine(
                        repository.allFolders,
                        repository.allFolderCardCrossRefs,
                        repository.userPreferencesRepository.showEnergyAnimations,
                        repository.userPreferencesRepository.showFinishAnimations,
                        repository.userPreferencesRepository.preferSetLogo,
                        repository.getPricesForCard(cardWithDetails.card.id),
                        repository.getVintagePricesForCard(cardWithDetails.card.id)
                    ) { flows ->
                        val folders = flows[0] as List<FolderEntity>
                        val crossRefs = flows[1] as List<FolderCardCrossRef>
                        val showEnergyAnims = flows[2] as Boolean
                        val showFinishAnims = flows[3] as Boolean
                        val preferLogo = flows[4] as Boolean
                        val prices = flows[5] as List<PriceEntity>
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

    fun saveChanges(quantity: Int, condition: String, printing: String, finish: String) {
        val current = _uiState.value.cardWithDetails?.userCard ?: return
        viewModelScope.launch {
            repository.updateUserCard(current.copy(
                quantity = quantity, 
                condition = condition,
                printing = printing,
                finish = finish
            ))
            _uiState.value = _uiState.value.copy(showSaveSuccess = true)
        }
    }

    fun consumeSaveSuccess() {
        _uiState.value = _uiState.value.copy(showSaveSuccess = false)
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
        val cardId = _uiState.value.cardWithDetails?.card?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingPrice = true)
            try {
                repository.updateCardPrice(cardId)
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshingPrice = false)
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
