package com.mrhayami.vaultio.ui.screens

import androidx.compose.runtime.Immutable
import com.mrhayami.vaultio.data.local.SetEntity

@Immutable
data class SetDownloadsState(
    val sets: List<SetEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val downloadedSets = sets.filter { it.isDownloaded }
    val remainingSets = sets.filter { !it.isDownloaded }
    
    // Estimate storage (very rough estimate: ~1.5KB per card entity)
    val totalCardsDownloaded = downloadedSets.sumOf { it.totalCards }
    val estimatedSizeMB = (totalCardsDownloaded * 1.5 / 1024.0)
    
    val downloadProgress: Float = if (sets.isNotEmpty()) {
        downloadedSets.size.toFloat() / sets.size.toFloat()
    } else 0f
}

sealed interface SetDownloadsEvent {
    data object RefreshSets : SetDownloadsEvent
    data object DownloadAll : SetDownloadsEvent
    data object DeleteAll : SetDownloadsEvent
    data class DownloadSet(val setId: String) : SetDownloadsEvent
    data class DeleteSet(val setId: String) : SetDownloadsEvent
    data object DismissError : SetDownloadsEvent
}

sealed interface SetDownloadsEffect {
    data class ShowError(val message: String) : SetDownloadsEffect
}
