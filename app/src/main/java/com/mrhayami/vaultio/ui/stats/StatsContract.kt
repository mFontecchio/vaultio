package com.mrhayami.vaultio.ui.stats

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.CollectionSnapshotEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Immutable
data class StatsViewState(
    val isLoading: Boolean = false,
    val totalValue: Double = 0.0,
    val cardCount: Int = 0,
    val snapshots: ImmutableList<CollectionSnapshotEntity> = persistentListOf(),
    val mostValuableCards: ImmutableList<CardWithValue> = persistentListOf(),
    val distributionByRarity: ImmutableMap<String, Int> = persistentMapOf(),
    val distributionByType: ImmutableMap<String, Int> = persistentMapOf(),
    val setCompletion: ImmutableList<SetCompletionInfo> = persistentListOf()
)

@Immutable
data class CardWithValue(
    val details: CardWithDetails,
    val value: Double
)

@Immutable
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
