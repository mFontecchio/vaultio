package com.mrhayami.vaultio.ui.screens

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mrhayami.vaultio.data.repository.VaultioRepository
import com.mrhayami.vaultio.ui.common.MviViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class SetDownloadsViewModel(
    private val repository: VaultioRepository
) : MviViewModel<SetDownloadsState, SetDownloadsEvent, SetDownloadsEffect>(
    initialState = SetDownloadsState()
) {
    init {
        viewModelScope.launch {
            repository.allSets.collect { sets ->
                updateState { copy(sets = sets, isLoading = false) }
                
                // Auto-refresh if we have sets but they are missing logos (stale data fix)
                if (sets.isNotEmpty() && sets.take(10).all { it.logo == null || !it.logo.contains("http") }) {
                    refreshSets()
                }
            }
        }
    }

    override fun onEvent(event: SetDownloadsEvent) {
        when (event) {
            SetDownloadsEvent.RefreshSets -> refreshSets()
            SetDownloadsEvent.DownloadAll -> downloadAll()
            SetDownloadsEvent.DeleteAll -> deleteAll()
            is SetDownloadsEvent.DownloadSet -> downloadSet(event.setId)
            is SetDownloadsEvent.DeleteSet -> deleteSet(event.setId)
            SetDownloadsEvent.DismissError -> updateState { copy(error = null) }
        }
    }

    private fun refreshSets() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            runCatching {
                repository.refreshSets()
            }.onFailure { e ->
                Log.e("SetDownloadsViewModel", "Failed to refresh sets", e)
                emitEffect(SetDownloadsEffect.ShowError("Failed to refresh sets: ${e.message}"))
            }
            updateState { copy(isRefreshing = false) }
        }
    }

    private fun downloadAll() {
        viewModelScope.launch {
            val remaining = state.value.remainingSets
            if (remaining.isEmpty()) return@launch
            
            updateState { copy(isLoading = true) }
            
            remaining.map { set ->
                async {
                    runCatching {
                        repository.downloadSet(set.id)
                    }.onFailure { e ->
                        Log.e("SetDownloadsViewModel", "Failed to download set ${set.id}", e)
                    }
                }
            }.awaitAll()
            
            updateState { copy(isLoading = false) }
        }
    }

    private fun deleteAll() {
        viewModelScope.launch {
            val downloaded = state.value.downloadedSets
            if (downloaded.isEmpty()) return@launch
            
            updateState { copy(isLoading = true) }
            downloaded.forEach { set ->
                repository.deleteDownloadedSet(set.id)
            }
            updateState { copy(isLoading = false) }
        }
    }

    private fun downloadSet(setId: String) {
        viewModelScope.launch {
            repository.downloadSet(setId)
        }
    }

    private fun deleteSet(setId: String) {
        viewModelScope.launch {
            repository.deleteDownloadedSet(setId)
        }
    }
}
