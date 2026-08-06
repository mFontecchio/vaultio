package com.mrhayami.vaultio.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for the MVI pattern.
 *
 * S — State:      immutable data class representing what the UI renders
 * E — Event:      sealed interface for user actions / UI triggers
 * F — SideEffect: sealed interface for one-time effects (navigation, toasts…)
 */
abstract class MviViewModel<S : Any, E : Any, F : Any>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _sideEffects = Channel<F>(Channel.BUFFERED)
    val sideEffects: Flow<F> = _sideEffects.receiveAsFlow()

    /** Single entry point for all UI events. */
    abstract fun onEvent(event: E)

    /** Update state using a reducer. Thread-safe via MutableStateFlow.update. */
    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    /** Emit a one-time side effect. */
    protected fun emitEffect(effect: F) {
        viewModelScope.launch { _sideEffects.send(effect) }
    }
}
