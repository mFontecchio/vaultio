---
name: mvi-editor
description: Guide for implementing the MVI (Model-View-Intent) pattern in Android with Jetpack Compose. Use when creating new screens, adding state management, or refactoring existing ViewModels to follow unidirectional data flow. Fully self-contained — no external MVI library required.
---

# MVI Editor

## Overview

MVI (Model-View-Intent) is a unidirectional data flow pattern for Android UIs:

```
User Action → Event → ViewModel → State / SideEffect → UI
```

This skill is **fully self-contained**. Everything you need is defined here — no external MVI
library required. Copy the foundation once and reuse it across all features.

---

## Data Flow

```
┌──────────────────────────────────────────────────┐
│                    UI Layer                      │
│                                                  │
│   ┌──────────┐  events   ┌─────────────────┐    │
│   │  Screen  │ ────────► │   ViewModel     │    │
│   │          │           │                 │    │
│   │          │ ◄──────── │  _state         │    │
│   │          │  state    │  _sideEffects   │    │
│   └──────────┘           └─────────────────┘    │
│         │                        │               │
│   side effects            domain calls           │
│         ▼                        ▼               │
│   Navigation /            Use Cases /            │
│   Toast / Dialog          Repositories           │
└──────────────────────────────────────────────────┘
```

---

## Step 1 — Foundation (copy once per project)

Create this base class in a shared module. It encapsulates the boilerplate that every ViewModel
would otherwise duplicate:

```kotlin
// shared/mvi/MviViewModel.kt

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
```

> **No base class preferred?** Skip `MviViewModel` and copy the `_state` + `_sideEffects`
> boilerplate directly into each ViewModel — see the "Without base class" example below.

---

## Step 2 — Contract (one file per feature)

Three plain Kotlin types — no external interfaces to extend:

```kotlin
// feature/orders/OrderContract.kt

/** Snapshot of what the UI should render. All fields have defaults. */
data class OrderViewState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null,
)

/** User actions and UI triggers. */
sealed interface OrderEvent {
    data object OnScreenOpened   : OrderEvent
    data object OnRefreshClicked : OrderEvent
    data class  OnOrderClicked(val orderId: String) : OrderEvent
}

/** One-time effects that the Screen reacts to. */
sealed interface OrderEffect {
    data class ShowError(val message: String) : OrderEffect

    sealed interface Navigation : OrderEffect {
        data class  GoToDetail(val orderId: String) : Navigation
        data object GoBack : Navigation
    }
}
```

### Contract rules

| Type       | Kotlin type        | Rule                                                            |
|------------|--------------------|-----------------------------------------------------------------|
| State      | `data class`       | All fields `val`, all have defaults                             |
| Event      | `sealed interface` | `data object` for parameterless, `data class` for carrying data |
| SideEffect | `sealed interface` | Nest `Navigation` as sub-sealed for type-safe nav wiring        |

---

## Step 3 — ViewModel

### With `MviViewModel` base class

```kotlin
class OrderViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
) : MviViewModel<OrderViewState, OrderEvent, OrderEffect>(
    initialState = OrderViewState(),
) {

    override fun onEvent(event: OrderEvent) {
        when (event) {
            OrderEvent.OnScreenOpened,
            OrderEvent.OnRefreshClicked -> loadOrders()
            is OrderEvent.OnOrderClicked -> navigateToDetail(event.orderId)
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            getOrdersUseCase()
                .onSuccess { orders ->
                    updateState { copy(isLoading = false, orders = orders) }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false) }
                    emitEffect(OrderEffect.ShowError(error.message ?: "Unknown error"))
                }
        }
    }

    private fun navigateToDetail(orderId: String) {
        emitEffect(OrderEffect.Navigation.GoToDetail(orderId))
    }
}
```

### Without base class (flat)

```kotlin
class OrderViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OrderViewState())
    val state: StateFlow<OrderViewState> = _state.asStateFlow()

    private val _sideEffects = Channel<OrderEffect>(Channel.BUFFERED)
    val sideEffects: Flow<OrderEffect> = _sideEffects.receiveAsFlow()

    fun onEvent(event: OrderEvent) {
        when (event) {
            OrderEvent.OnScreenOpened,
            OrderEvent.OnRefreshClicked -> loadOrders()
            is OrderEvent.OnOrderClicked -> navigateToDetail(event.orderId)
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getOrdersUseCase()
                .onSuccess { orders ->
                    _state.update { it.copy(isLoading = false, orders = orders) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _sideEffects.send(OrderEffect.ShowError(error.message ?: "Unknown error"))
                }
        }
    }

    private fun navigateToDetail(orderId: String) {
        viewModelScope.launch { _sideEffects.send(OrderEffect.Navigation.GoToDetail(orderId)) }
    }
}
```

---

## Step 4 — Screen

```kotlin
@Composable
fun OrderScreen(
    state: OrderViewState,
    onEvent: (OrderEvent) -> Unit,
    sideEffects: Flow<OrderEffect>,
    onNavigation: (OrderEffect.Navigation) -> Unit,
) {
    // Collect side effects — one LaunchedEffect scoped to Unit fires once on entry
    LaunchedEffect(Unit) {
        sideEffects.collect { effect ->
            when (effect) {
                is OrderEffect.Navigation -> onNavigation(effect)
                is OrderEffect.ShowError  -> { /* show snackbar/toast */ }
            }
        }
    }

    // Trigger initial load
    LaunchedEffect(Unit) {
        onEvent(OrderEvent.OnScreenOpened)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.errorMessage != null ->
                ErrorView(
                    message = state.errorMessage,
                    onRetry = { onEvent(OrderEvent.OnRefreshClicked) },
                )
            else ->
                OrderList(
                    orders = state.orders,
                    onOrderClick = { onEvent(OrderEvent.OnOrderClicked(it.id)) },
                )
        }
    }
}
```

### Screen rules

- Receives `state`, `onEvent`, `sideEffects`, `onNavigation` — **never** the ViewModel directly
- **Stateless and pure**: same inputs → same UI output
- No business logic, no coroutines launched manually

---

## Step 5 — Navigation wiring (app layer)

```kotlin
// app/navigation/OrderNavigation.kt

fun NavGraphBuilder.orderNavigation(navController: NavController) {
    composable<OrderRoute> {
        val viewModel: OrderViewModel = viewModel() // or koinViewModel() / hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        OrderScreen(
            state = state,
            onEvent = viewModel::onEvent,
            sideEffects = viewModel.sideEffects,
            onNavigation = { nav ->
                when (nav) {
                    is OrderEffect.Navigation.GoToDetail ->
                        navController.navigate(OrderDetailRoute(nav.orderId))
                    OrderEffect.Navigation.GoBack ->
                        navController.popBackStack()
                }
            },
        )
    }
}
```

---

## Common State Patterns

### Loading / Error / Content

```kotlin
data class ContentViewState(
    val isLoading: Boolean = false,
    val content: ContentData? = null,
    val error: String? = null,
)

// Rendering
when {
    state.isLoading      -> LoadingIndicator()
    state.error != null  -> ErrorView(state.error)
    state.content != null -> ContentView(state.content)
}
```

### Form

```kotlin
data class FormViewState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val emailError: String? = null,
) {
    // Derived — computed from state, not stored separately
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}
```

### Paginated list

```kotlin
data class ListViewState(
    val items: List<Item> = emptyList(),
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = true,
    val currentPage: Int = 0,
)
```

---

## Testing

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getOrdersUseCase: GetOrdersUseCase = mockk()
    private lateinit var sut: OrderViewModel

    @Before
    fun setup() {
        sut = OrderViewModel(getOrdersUseCase)
    }

    @Test
    fun `SHOULD show orders WHEN load succeeds`() = runTest {
        // GIVEN
        val orders = listOf(Order("1", "Pizza"))
        coEvery { getOrdersUseCase() } returns Result.success(orders)

        // WHEN
        sut.onEvent(OrderEvent.OnScreenOpened)
        advanceUntilIdle()

        // THEN
        assertEquals(orders, sut.state.value.orders)
        assertFalse(sut.state.value.isLoading)
        assertNull(sut.state.value.errorMessage)
    }

    @Test
    fun `SHOULD emit ShowError WHEN load fails`() = runTest {
        // GIVEN
        coEvery { getOrdersUseCase() } returns Result.failure(RuntimeException("Net error"))

        val effects = mutableListOf<OrderEffect>()
        val job = launch { sut.sideEffects.toList(effects) }

        // WHEN
        sut.onEvent(OrderEvent.OnScreenOpened)
        advanceUntilIdle()
        job.cancel()

        // THEN
        assertTrue(effects.first() is OrderEffect.ShowError)
    }
}

// ── Utility — copy into your test module ─────────────────────────────────────
class MainDispatcherRule(
    val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description?) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description?) = Dispatchers.resetMain()
}
```

---

## Anti-Patterns

| ❌ Bad                                               | ✅ Good                                           |
|-----------------------------------------------------|--------------------------------------------------|
| ViewModel injected directly into `@Composable`      | Pass `state`, `onEvent`, `sideEffects` as params |
| `MutableStateFlow` or `Channel` exposed as `public` | Expose `StateFlow` / `Flow` (read-only)          |
| Navigation logic inside a composable                | Emit `Navigation` effect, handle in nav layer    |
| `var` fields in the state                           | All fields `val`, use `copy()` to update         |
| Multiple `StateFlow` fields in ViewModel            | Single `state: StateFlow<ViewState>`             |
| Coroutine launched without a scope                  | Always use `viewModelScope.launch`               |
| `GlobalScope`                                       | Never — leaks beyond the lifecycle               |

---

## References

- [Android Architecture — UDF](https://developer.android.com/topic/architecture#udf)
- [ViewModel + StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Compose state hoisting](https://developer.android.com/develop/ui/compose/state#state-hoisting)
- [Channel — fire-and-forget effects](https://kotlinlang.org/docs/channels.html)
