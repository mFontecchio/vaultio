package com.mrhayami.vaultio.ui.stats

import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.CollectionSnapshotEntity

data class StatsViewState(
    val isLoading: Boolean = false,
    val totalValue: Double = 0.0,
    val cardCount: Int = 0,
    val snapshots: List<CollectionSnapshotEntity> = emptyList(),
    val mostValuableCards: List<CardWithValue> = emptyList(),
    val distributionByRarity: Map<String, Int> = emptyList<Pair<String, Int>>().toMap(),
    val distributionByType: Map<String, Int> = emptyList<Pair<String, Int>>().toMap(),
    val setCompletion: List<SetCompletionInfo> = emptyList()
)

data class CardWithValue(
    val details: CardWithDetails,
    val value: Double
)

data class SetCompletionInfo(
    val setId: String,
    val setName: String,
    val logo: String?,
    val collectedCount: Int,
    val totalCount: Int,
    val completionPercentage: Float
)

sealed interface StatsEvent {
    data object OnScreenOpened : StatsEvent
    data object OnRefreshClicked : StatsEvent
}

sealed interface StatsEffect {
    data class ShowError(val message: String) : StatsEffect

    sealed interface Navigation : StatsEffect {
        data object GoBack : StatsEffect.Navigation
        data class GoToCardDetail(val userCardId: Long) : StatsEffect.Navigation
    }
}
