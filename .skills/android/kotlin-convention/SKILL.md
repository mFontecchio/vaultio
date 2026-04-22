---
name: kotlin-convention
description: Guide for writing idiomatic, clean, and maintainable Kotlin code. Use when reviewing Kotlin code style, applying language best practices, or onboarding engineers to Kotlin conventions for Android projects.
---

# Kotlin Convention

## Overview

This skill defines idiomatic Kotlin conventions for Android projects: naming, null safety, data
modeling, functional patterns, and language features to embrace or avoid.

---

## Naming

| Element                              | Convention               | Example                                        |
|--------------------------------------|--------------------------|------------------------------------------------|
| Class / Interface / Object           | PascalCase               | `OrderRepository`, `UserMapper`                |
| Function / Variable                  | camelCase                | `fetchUser()`, `currentState`                  |
| Constant (`const val`, `object val`) | SCREAMING_SNAKE_CASE     | `MAX_RETRY_COUNT`                              |
| Package                              | lowercase, dot-separated | `com.example.feature.order`                    |
| Extension function                   | camelCase, descriptive   | `String.isValidEmail()`                        |
| Test function                        | backtick sentence        | `` `SHOULD return error WHEN network fails` `` |

### Naming Rules

- Be **explicit** over short: `userIdentifier` not `uid`
- Boolean properties: prefix with `is`, `has`, `can` → `isLoading`, `hasError`
- Functions returning `Boolean`: prefix with `is`, `can`, `should` → `isValid()`, `canSubmit()`
- Avoid `data`, `info`, `manager` as suffixes unless unavoidable

---

## Null Safety

### Avoid `!!`

```kotlin
// ❌ Crashes at runtime if null
val name = user!!.name

// ✅ Safe call + Elvis
val name = user?.name ?: "Unknown"

// ✅ Early return pattern
fun process(user: User?) {
    val u = user ?: return
    doWork(u)
}
```

### Prefer `let`, `run`, `apply`, `also`, `with` Appropriately

| Function | Receiver | Returns       | Best For                      |
|----------|----------|---------------|-------------------------------|
| `let`    | `it`     | lambda result | Null checks, transformation   |
| `run`    | `this`   | lambda result | Object configuration + result |
| `apply`  | `this`   | receiver      | Builder-style setup           |
| `also`   | `it`     | receiver      | Side effects (logging)        |
| `with`   | `this`   | lambda result | Multiple calls on same object |

```kotlin
// let for null-safe call
user?.let { sendEmail(it.email) }

// apply for object construction
val config = Config().apply {
    host = "api.example.com"
    port = 443
    timeout = 30
}

// also for logging side effect
return fetchUser(id).also { log("Fetched user: $it") }
```

---

## Data Classes

```kotlin
// ✅ Immutable domain model
data class Order(
    val id: String,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val createdAt: Instant,
)

// ✅ Update with copy
val updated = order.copy(status = OrderStatus.SHIPPED)

// ❌ Avoid mutable data classes for domain models
data class Order(
    var id: String,       // mutable = unpredictable
    var status: String,
)
```

---

## Sealed Classes / Interfaces

Use for **restricted hierarchies** — states, results, events:

```kotlin
// Result type
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

// Events
sealed interface UiEvent {
    data object OnScreenOpened : UiEvent
    data class OnItemClicked(val id: String) : UiEvent
}

// Exhaustive when — compiler enforces all branches
when (result) {
    is Result.Success -> show(result.value)
    is Result.Error -> showError(result.exception)
    Result.Loading -> showLoading()
}
```

> Prefer `sealed interface` over `sealed class` — more flexible (multiple inheritance).

---

## Functions

### Single Expression Functions

```kotlin
// ✅ Concise for simple transformations
fun Double.toPercentage(): String = "${(this * 100).toInt()}%"

// ❌ Redundant block for single expression
fun Double.toPercentage(): String {
    return "${(this * 100).toInt()}%"
}
```

### Extension Functions

```kotlin
// ✅ Domain-specific extensions
fun Instant.isOlderThan(duration: Duration): Boolean = this < Clock.System.now() - duration

fun String.isValidEmail(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

// ❌ Extension on types you don't own for unrelated functionality
fun String.doOrderThing(): Order { ... }  // Not clearly related to String
```

### Default Parameters vs Overloads

```kotlin
// ✅ Default parameters (idiomatic Kotlin)
fun fetchOrders(userId: String, pageSize: Int = 20, page: Int = 0): List<Order>

// ❌ Multiple overloads (Java-style)
fun fetchOrders(userId: String): List<Order>
fun fetchOrders(userId: String, pageSize: Int): List<Order>
fun fetchOrders(userId: String, pageSize: Int, page: Int): List<Order>
```

---

## Collections

```kotlin
// Prefer immutable by default
val items: List<Item> = listOf()       // immutable reference
val mutable: MutableList<Item> = mutableListOf()  // only when mutation needed

// Functional operators over loops
val activeUsers = users
    .filter { it.isActive }
    .sortedByDescending { it.lastLogin }
    .take(10)

// Sequences for large collections with multiple chained operations
val result = largeList
    .asSequence()
    .filter { it.isValid }
    .map { transform(it) }
    .firstOrNull()

// groupBy
val byCategory = products.groupBy { it.category }

// associateBy for quick lookup map
val userById: Map<String, User> = users.associateBy { it.id }

// partition splits into (matching, notMatching)
val (active, inactive) = users.partition { it.isActive }
```

---

## Value Classes (Inline)

Prevent primitive type confusion at zero runtime cost:

```kotlin
@JvmInline value class UserId(val value: String)
@JvmInline value class ProductId(val value: String)
@JvmInline value class Price(val cents: Long) {
    val euros: Double get() = cents / 100.0
}

// ✅ Type-safe — won't accidentally mix UserId and ProductId
fun getUser(id: UserId): User
fun getProduct(id: ProductId): Product
```

---

## Error Handling

```kotlin
// ✅ Use Result<T> for expected/recoverable errors
suspend fun fetchData(): Result<Data> = runCatching { api.getData() }

// ✅ Throw exceptions only for programmer errors (illegal state)
require(count >= 0) { "Count must be non-negative, got $count" }
check(isInitialized) { "Must call init() before use" }
error("Unexpected branch in ${::processState.name}")

// ❌ Don't use exceptions for flow control
try {
    val user = fetchUser(id)
} catch (e: UserNotFoundException) {  // ❌ use Result / nullable return
    // ...
}
```

---

## Coroutines Style

```kotlin
// ✅ suspend function signature — always document blocking behavior
/** Fetches user from network. IO-bound. */
suspend fun fetchUser(id: String): User

// ✅ Name coroutine builders clearly
viewModelScope.launch(CoroutineName("load-orders")) { ... }

// ❌ runBlocking in production code
fun getUser(): User = runBlocking { fetchUser("1") }  // Blocks thread
```

---

## When Expressions

```kotlin
// ✅ Exhaustive when as expression (sealed types)
val label = when (status) {
    Status.PENDING -> "Pending"
    Status.ACTIVE -> "Active"
    Status.CLOSED -> "Closed"
}

// ✅ When as statement with complex conditions
when {
    age < 0 -> error("Invalid age")
    age < 18 -> showMinorWarning()
    age >= 65 -> offerSeniorDiscount()
    else -> processStandard()
}
```

---

## Object / Companion Object

```kotlin
// ✅ Companion object for factory methods and constants
class Config private constructor(val host: String, val port: Int) {
    companion object {
        val DEFAULT = Config("localhost", 8080)
        fun fromEnv() = Config(
            host = System.getenv("HOST") ?: "localhost",
            port = System.getenv("PORT")?.toInt() ?: 8080,
        )
    }
}

// ✅ object for singletons with no state
object DateFormatter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    fun format(date: LocalDate): String = formatter.format(date)
}
```

---

## Anti-Patterns

| ❌ Avoid                           | ✅ Instead                                                                                                                                                   |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `!!` operator                     | Safe call + Elvis / early return                                                                                                                            |
| `var` in data classes             | `val` + `copy()`                                                                                                                                            |
| Checked exceptions                | `Result<T>` / sealed error types                                                                                                                            |
| Java-style overloads              | Default parameter values                                                                                                                                    |
| Nested `if` chains                | `when` expression / guard returns                                                                                                                           |
| `Any` as parameter type           | Specific sealed interface / generics                                                                                                                        |
| Magic numbers                     | Named constants / value classes                                                                                                                             |
| Mutable `List` exposed publicly   | `List` (immutable) in public API                                                                                                                            |
| `lateinit var` for nullable types | Nullable with `?` or `by lazy { }` — `lateinit` is only valid for non-null, and crashes with `UninitializedPropertyAccessException` if accessed before init |

---

## Checklist: Code Review

- [ ] Naming follows conventions (PascalCase, camelCase, SCREAMING_SNAKE)
- [ ] No `!!` operator (except truly null-impossible cases with comment)
- [ ] `data class` fields are `val`
- [ ] Sealed hierarchies use `sealed interface`
- [ ] No unchecked `Exception` for flow control
- [ ] Large collection chains use `.asSequence()`
- [ ] Coroutines use injected dispatchers
- [ ] `companion object` used for factories/constants
- [ ] Extension functions are placed in appropriate scope

---

## References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Effective Kotlin (book)](https://kt.academy/book/effectivekotlin)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
