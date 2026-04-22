---
name: kotlin-coroutines
description: Guide for writing correct, performant, and idiomatic Kotlin Coroutines code. Use when implementing async operations, reactive streams with Flow/StateFlow/SharedFlow, or reviewing coroutine usage for correctness and performance issues.
---

# Kotlin Coroutines

## Overview

Best practices for Kotlin Coroutines — covering structured concurrency, Flow operators, StateFlow,
SharedFlow, error handling, testing, and common pitfalls.

---

## Structured Concurrency

### Always Use a Scoped Coroutine

```kotlin
// ❌ GlobalScope — leaks, no lifecycle, unstructured
GlobalScope.launch { fetchData() }

// ✅ viewModelScope — cancelled when ViewModel cleared
class MyViewModel : ViewModel() {
    fun load() { viewModelScope.launch { fetchData() } }
}

// ✅ lifecycleScope — cancelled when lifecycle destroyed
class MyFragment : Fragment() {
    fun load() { viewLifecycleOwner.lifecycleScope.launch { fetchData() } }
}

// ✅ Custom scope — you control lifecycle
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
fun destroy() { scope.cancel() }
```

### Dispatchers — Inject, Never Hardcode

```kotlin
// ❌ Hardcoded dispatcher — impossible to override in tests
suspend fun fetchFromDb() = withContext(Dispatchers.IO) { dao.getAll() }

// ✅ Injected dispatcher
class MyRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun fetchFromDb() = withContext(ioDispatcher) { dao.getAll() }
}

// Test override
val repo = MyRepository(ioDispatcher = UnconfinedTestDispatcher())
```

### `SupervisorJob` for Independent Children

```kotlin
// Without SupervisorJob — one child failure cancels all siblings
val scope = CoroutineScope(Job())

// With SupervisorJob — children fail independently
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

### `async` / `await` for Parallel Work

```kotlin
// Sequential — total time = A + B
val userResult = fetchUser()
val settingsResult = fetchSettings()

// Parallel — total time = max(A, B)
coroutineScope {
    val user = async { fetchUser() }
    val settings = async { fetchSettings() }
    combine(user.await(), settings.await())
}
```

---

## Flow

### Cold vs Hot

| Type               | When emits    | Subscribers            |
|--------------------|---------------|------------------------|
| `Flow` (cold)      | Per collector | Each gets fresh stream |
| `StateFlow` (hot)  | Always alive  | Latest value replayed  |
| `SharedFlow` (hot) | Always alive  | Configurable replay    |

### Flow Operators — Common Patterns

```kotlin
// Transform
flow.map { transform(it) }
flow.flatMapLatest { id -> fetchDetails(id) }  // cancels previous on new emission
flow.flatMapMerge { id -> fetchDetails(id) }   // concurrent, unordered

// Filter
flow.filter { it.isValid }
flow.filterNotNull()
flow.distinctUntilChanged()                     // skip duplicate emissions
flow.distinctUntilChangedBy { it.id }          // skip if key is same

// Combine
combine(flowA, flowB) { a, b -> Pair(a, b) }  // emits when either updates
zip(flowA, flowB) { a, b -> Pair(a, b) }       // emits when both emit once

// Error handling
flow.catch { e -> emit(defaultValue) }
flow.retry(3) { e -> e is IOException }
flow.retryWhen { e, attempt -> attempt < 3 && e is IOException }

// Timing
flow.debounce(300)       // wait 300ms after last emission (search input)
flow.throttleFirst(500)  // emit at most once per 500ms
flow.sample(1_000)       // emit latest value every 1s

// Terminal
flow.first()              // suspend until first emission
flow.firstOrNull()
flow.toList()             // collect all into a list
flow.single()             // collect exactly one item
```

### `flatMapLatest` vs `flatMapMerge` vs `flatMapConcat`

| Operator        | Behavior                                    | Use Case                     |
|-----------------|---------------------------------------------|------------------------------|
| `flatMapLatest` | Cancels previous inner flow on new emission | Search-as-you-type           |
| `flatMapMerge`  | Concurrent, emissions interleaved           | Parallel independent fetches |
| `flatMapConcat` | Sequential, waits for each inner flow       | Order-dependent operations   |

---

## StateFlow

```kotlin
// In ViewModel
private val _state = MutableStateFlow(UiState())
val state: StateFlow<UiState> = _state.asStateFlow()

// Update atomically
_state.update { current -> current.copy(isLoading = true) }

// Expose a transformed view
val isLoading: StateFlow<Boolean> = _state
    .map { it.isLoading }
    .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = false)
```

### `stateIn` — `SharingStarted` Options

| Strategy                 | Behavior                                                     | Use                              |
|--------------------------|--------------------------------------------------------------|----------------------------------|
| `Eagerly`                | Starts immediately, never stops                              | Always-active state              |
| `Lazily`                 | Starts on first subscriber, never stops                      | ViewModel state                  |
| `WhileSubscribed(5_000)` | Starts on first subscriber, stops 5s after last unsubscribes | Memory-efficient ViewModel state |

> **Best practice for ViewModels**: `WhileSubscribed(5_000)` — survives brief background/rotation
> gaps.

---

## SharedFlow

```kotlin
// One-shot events (side effects)
private val _events = MutableSharedFlow<UiEvent>(
    replay = 0,              // no replay — events are consumed
    extraBufferCapacity = 64, // buffer during slow collectors
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)
val events: SharedFlow<UiEvent> = _events.asSharedFlow()

fun emitEvent(event: UiEvent) {
    viewModelScope.launch { _events.emit(event) }
}
```

> **Use `Channel` instead** when you need exactly-once delivery with backpressure:

```kotlin
private val _channel = Channel<UiEvent>(Channel.BUFFERED)
val events: Flow<UiEvent> = _channel.receiveAsFlow()
```

---

## Error Handling

### Use `Result<T>` for Expected Failures

```kotlin
suspend fun fetchUser(id: String): Result<User> = runCatching {
    api.getUser(id)
}

// Consumer
fetchUser("123")
    .onSuccess { user -> updateUi(user) }
    .onFailure { error -> showError(error.message) }
```

### `try/catch` — What to Catch

```kotlin
// ✅ Catch specific exceptions
try {
    api.call()
} catch (e: IOException) {
    // network error
} catch (e: HttpException) {
    // server error
}

// ❌ Never swallow CancellationException
try {
    delay(1000)
} catch (e: Exception) {  // ❌ Catches CancellationException too
    log(e)
}

// ✅ Always rethrow CancellationException
try {
    delay(1000)
} catch (e: CancellationException) {
    throw e  // required for structured concurrency
} catch (e: Exception) {
    log(e)
}
```

### Flow Error Handling

```kotlin
// In the producer — don't let uncaught exceptions crash collectors
val safeFlow = upstream
    .catch { e -> emit(Result.failure(e)) }

// In the consumer
viewModelScope.launch {
    safeFlow.collect { result ->
        result.onSuccess { /* ... */ }.onFailure { /* ... */ }
    }
}
```

---

## Lifecycle-Safe Collection in UI

```kotlin
// ❌ Collects in background — leaks when app goes to background
lifecycleScope.launch {
    viewModel.state.collect { render(it) }
}

// ✅ Pauses when STOPPED, resumes on START
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { render(it) }
    }
}

// ✅ In Compose — collectAsStateWithLifecycle
val state by viewModel.state.collectAsStateWithLifecycle()
```

---

## Common Pitfalls

| Pitfall                                       | Symptom                                                      | Fix                         |
|-----------------------------------------------|--------------------------------------------------------------|-----------------------------|
| `GlobalScope`                                 | Memory leaks, crashes after process death                    | Use structured scopes       |
| Hardcoded dispatcher                          | Tests run on real threads, timeouts                          | Inject dispatchers          |
| Catching `CancellationException`              | Coroutine won't cancel                                       | Re-throw it                 |
| `runBlocking` in production                   | ANR / UI freeze                                              | Use `launch`/`async`        |
| Missing `supervisorScope` for parallel jobs   | One failure cancels all                                      | Use `supervisorScope { }`   |
| `collect` without lifecycle guard             | Collects in background                                       | `repeatOnLifecycle`         |
| Multiple `StateFlow` in ViewModel             | State fragmentation, inconsistent UI                         | Single `UiState` data class |
| Hot flow shared with `replay=0` missing event | Events lost before subscriber attaches                       | Use `replay=1` or `Channel` |
| `launchWhenStarted` / `launchWhenResumed`     | Deprecated — does not cancel the coroutine, only suspends it | Use `repeatOnLifecycle`     |

---

## Cooperative Cancellation

Coroutines are only cancelled at **suspension points**. A tight loop without suspension will never
cancel.

```kotlin
// ❌ Non-cooperative — never cancels
suspend fun processLargeList(items: List<Item>) {
    items.forEach { item ->
        processItem(item)  // No suspension point
    }
}

// ✅ Add ensureActive() to check cancellation
suspend fun processLargeList(items: List<Item>) {
    items.forEach { item ->
        ensureActive()  // Throws CancellationException if cancelled
        processItem(item)
    }
}

// ✅ Or yield() — suspends briefly and checks cancellation
suspend fun processLargeList(items: List<Item>) {
    items.forEach { item ->
        yield()
        processItem(item)
    }
}
```

---

## Callback Conversion

Convert callback-based APIs to `Flow` using `callbackFlow`. Always clean up in `awaitClose`.

```kotlin
// ✅ callbackFlow with awaitClose
fun locationUpdates(): Flow<Location> = callbackFlow {
    val listener = LocationListener { location ->
        trySend(location)  // Non-blocking send
    }
    locationManager.requestLocationUpdates(criteria, listener, Looper.getMainLooper())

    awaitClose {
        locationManager.removeUpdates(listener)  // Called on cancellation or close
    }
}

// Usage
locationUpdates()
    .onEach { location -> updateMap(location) }
    .launchIn(viewModelScope)
```

> Use `trySend` (non-suspending) inside the listener. Never use `send` directly in a callback.

---

## Testing Coroutines

```kotlin
@Test
fun `SHOULD emit loading then content`() = runTest {
    // GIVEN
    coEvery { repository.fetch() } returns Result.success(listOf(item))

    // WHEN
    viewModel.load()
    advanceUntilIdle()

    // THEN
    assertEquals(listOf(item), viewModel.state.value.items)
    assertFalse(viewModel.state.value.isLoading)
}

// Test Flow with Turbine
@Test
fun `SHOULD emit events in order`() = runTest {
    viewModel.events.test {
        viewModel.triggerAction()
        assertEquals(UiEvent.ShowSuccess, awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## References

- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Flow reference](https://kotlinlang.org/docs/flow.html)
- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [repeatOnLifecycle](https://developer.android.com/topic/libraries/architecture/coroutines#repeatonlifecycle)
- [Turbine — Flow testing](https://github.com/cashapp/turbine)
