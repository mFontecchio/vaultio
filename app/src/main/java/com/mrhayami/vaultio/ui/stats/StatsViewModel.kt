package com.mrhayami.vaultio.ui.stats

import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.VintageSets
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StatsViewModel(
    private val repository: VaultioRepository
) : MviViewModel<StatsViewState, StatsEvent, StatsEffect>(StatsViewState()) {

    override fun onEvent(event: StatsEvent) {
        when (event) {
            StatsEvent.OnScreenOpened -> loadStats()
            StatsEvent.OnRefreshClicked -> loadStats()
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            val userCardsFlow = repository.allUserCards
            val snapshotsFlow = repository.getAllSnapshots()
            val allPricesFlow = repository.allPrices
            val allVintagePricesFlow = repository.allVintagePrices
            val allSetsFlow = repository.allSets

            combine(
                userCardsFlow,
                snapshotsFlow,
                allPricesFlow,
                allVintagePricesFlow,
                allSetsFlow
            ) { userCards, snapshots, allPrices, allVintagePrices, allSets ->

                val cardWithValueList = userCards.map { details ->
                    val value = calculateCardValue(details, allPrices, allVintagePrices)
                    CardWithValue(details, value)
                }

                val totalValue = cardWithValueList.sumOf { it.value * it.details.userCard.quantity }
                val totalCount = userCards.sumOf { it.userCard.quantity }

                val mostValuable = cardWithValueList
                    .sortedByDescending { it.value * it.details.userCard.quantity }
                    .take(5)

                val rarityDist = userCards.groupBy { it.card.rarity ?: "Unknown" }
                    .mapValues { it.value.sumOf { c -> c.userCard.quantity } }

                val typeDist = userCards.flatMap { details ->
                    val types = details.card.types?.split(",") ?: listOf("Unknown")
                    types.map { it.trim() to details.userCard.quantity }
                }.groupBy({ it.first }, { it.second })
                    .mapValues { it.value.sum() }

                val setCompletion = allSets.map { set ->
                    val cardsInSet = userCards.filter { it.card.setId == set.id }
                    val uniqueCollected = cardsInSet.distinctBy { it.card.id }.size
                    val totalInSet = set.officialCards.takeIf { it > 0 } ?: set.totalCards

                    SetCompletionInfo(
                        setId = set.id,
                        setName = set.name,
                        logo = set.logo,
                        collectedCount = uniqueCollected,
                        totalCount = totalInSet,
                        completionPercentage = if (totalInSet > 0) (uniqueCollected.toFloat() / totalInSet) * 100f else 0f
                    )
                }.filter { it.collectedCount > 0 }
                    .sortedByDescending { it.completionPercentage }

                StatsViewState(
                    isLoading = false,
                    totalValue = totalValue,
                    cardCount = totalCount,
                    snapshots = snapshots,
                    mostValuableCards = mostValuable,
                    distributionByRarity = rarityDist,
                    distributionByType = typeDist,
                    setCompletion = setCompletion
                )
            }.collect { newState ->
                updateState { newState }
            }
        }
    }

    private fun calculateCardValue(
        details: CardWithDetails,
        allPrices: List<PriceEntity>,
        allVintagePrices: List<VintagePriceEntity>
    ): Double {
        val userCard = details.userCard
        val card = details.card

        if (userCard.manualPrice != null) return userCard.manualPrice

        return if (VintageSets.isVintageSet(card.setId)) {
            allVintagePrices.find {
                it.cardId == userCard.cardId &&
                        it.finish == userCard.finish &&
                        it.printing == userCard.printing &&
                        it.condition == userCard.condition
            }?.marketPrice ?: 0.0
        } else {
            allPrices.find {
                it.cardId == userCard.cardId &&
                        it.finish == userCard.finish &&
                        it.condition == userCard.condition
            }?.marketPrice ?: 0.0
        }
    }
}
