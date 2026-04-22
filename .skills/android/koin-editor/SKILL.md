---
name: koin-editor
description: Guide for setting up and using Koin for Dependency Injection in Android (and KMP) projects. Use when configuring DI modules, registering new dependencies, setting up scopes, testing with Koin, or troubleshooting injection failures.
---

# Koin Editor

## Overview

[Koin](https://insert-koin.io/) is a lightweight Kotlin DI framework that uses a DSL for declaring
dependencies. No code generation, no reflection — just Kotlin.

---

## Setup

### Dependencies

```toml
# gradle/libs.versions.toml
[versions]
koin = "3.5.6"

[libraries]
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
koin-test-junit4 = { module = "io.insert-koin:koin-test-junit4", version.ref = "koin" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
}
```

### Initialization

```kotlin
// Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.DEBUG)   // Level.ERROR in production
            androidContext(this@MyApplication)
            modules(
                appModule,
                repositoryModule,
                domainModule,
                viewModelModule,
            )
        }
    }
}
```

---

## Module Definition

### Basic Module

```kotlin
val repositoryModule = module {
    // Singleton — one instance for entire app lifetime
    single<UserRepository> {
        UserRepositoryImpl(
            localDataSource = get(),
            remoteDataSource = get(),
        )
    }

    // Factory — new instance per injection
    factory<UserLocalDataSource> {
        UserLocalDataSourceImpl(database = get())
    }

    // ViewModel
    viewModel {
        ProfileViewModel(
            getUserUseCase = get(),
            updateUserUseCase = get(),
        )
    }
}
```

### Scopes (for Session-Scoped Dependencies)

```kotlin
val sessionModule = module {
    scope<UserSession> {
        scoped { SessionCart() }      // alive as long as UserSession scope is open
    }
}

// Create/close scope
val scope = getKoin().createScope("session-1", named<UserSession>())
val cart: SessionCart = scope.get()
scope.close()
```

---

## Scope Reference

| Function        | Creates                  | Lifetime            |
|-----------------|--------------------------|---------------------|
| `single { }`    | One instance             | App lifetime        |
| `factory { }`   | New instance per `get()` | Per injection       |
| `viewModel { }` | ViewModel                | ViewModel lifecycle |
| `scoped { }`    | One per scope            | Scope lifetime      |

---

## Qualifiers (Named Dependencies)

```kotlin
// Declaration
val appModule = module {
    single<CoroutineDispatcher>(named("IO")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("Default")) { Dispatchers.Default }
    single<CoroutineDispatcher>(named("Main")) { Dispatchers.Main }
}

// Usage in module
factory {
    MyRepository(
        ioDispatcher = get(named("IO")),
    )
}

// Injection in constructor
class MyRepository(
    @Named("IO") private val ioDispatcher: CoroutineDispatcher,
)
```

---

## Injecting Dependencies

### Constructor Injection (preferred)

```kotlin
class GetOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val ioDispatcher: CoroutineDispatcher,
)

// Module wiring
factory {
    GetOrdersUseCase(
        orderRepository = get(),
        ioDispatcher = get(named("IO")),
    )
}
```

### In Activities / Fragments

```kotlin
class HomeActivity : AppCompatActivity() {
    private val viewModel: HomeViewModel by viewModel()
    private val analytics: AnalyticsTracker by inject()
}
```

### In Compose

```kotlin
@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = koinViewModel()
    // ...
}
```

---

## Lazy Injection

```kotlin
class HomeFragment : Fragment() {
    private val tracker: AnalyticsTracker by inject()  // lazy by default
}
```

---

## Module Organization (Best Practice)

Centralize all module files in your app module:

```
app/src/main/java/com/example/di/
├── AppModule.kt         # Context, dispatchers, framework dependencies
├── RepositoryModule.kt  # Repositories and data sources
├── DomainModule.kt      # Use Cases
└── ViewModelModule.kt   # ViewModels
```

```kotlin
// AppModule.kt
val appModule = module {
    single { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    single<CoroutineDispatcher>(named("IO")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("Default")) { Dispatchers.Default }
}

// RepositoryModule.kt
val repositoryModule = module {
    single<OrderRepository> {
        OrderRepositoryImpl(
            localDataSource = get(),
            remoteDataSource = get(),
            ioDispatcher = get(named("IO")),
        )
    }
    single<OrderLocalDataSource> { OrderRoomDataSource(database = get()) }
    single<OrderRemoteDataSource> { OrderApiDataSource(api = get()) }
}

// DomainModule.kt
val domainModule = module {
    factory { GetOrdersUseCase(repository = get(), ioDispatcher = get(named("IO"))) }
    factory { PlaceOrderUseCase(repository = get(), ioDispatcher = get(named("IO"))) }
}

// ViewModelModule.kt
val viewModelModule = module {
    viewModel { OrderListViewModel(getOrders = get()) }
    viewModel { params -> OrderDetailViewModel(getOrder = get(), orderId = params.get()) }
}
```

---

## Testing with Koin

### Option 1: KoinTest (JUnit4)

```kotlin
@RunWith(AndroidJUnit4::class)
class OrderRepositoryTest : KoinTest {

    private val repository: OrderRepository by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single<OrderRepository> { OrderRepositoryImpl(get(), get(), get(named("IO"))) }
                single<OrderLocalDataSource> { mockk() }
                single<OrderRemoteDataSource> { mockk() }
                single<CoroutineDispatcher>(named("IO")) { UnconfinedTestDispatcher() }
            }
        )
    }

    @Test
    fun `SHOULD return orders`() = runTest {
        val result = repository.getOrders()
        assertTrue(result.isSuccess)
    }
}
```

### Option 2: Override Module in Test (cleaner for unit tests)

For unit testing ViewModels and Use Cases, **prefer plain constructor injection** over Koin's test
utilities:

```kotlin
// No Koin needed — just inject manually
class OrderViewModelTest {
    private val mockRepo: OrderRepository = mockk()
    private val sut = OrderListViewModel(getOrders = GetOrdersUseCase(mockRepo, UnconfinedTestDispatcher()))
}
```

---

## Troubleshooting

| Error                       | Cause                                 | Fix                                            |
|-----------------------------|---------------------------------------|------------------------------------------------|
| `NoBeanDefFoundException`   | Dependency not declared in any module | Add `single/factory` for the type              |
| `InstanceCreationException` | Constructor threw an exception        | Check constructor params / `get()` calls       |
| Circular dependency         | A depends on B, B depends on A        | Introduce a third type or use `lazy { get() }` |
| Wrong dispatcher injected   | Missing `named()` qualifier           | Add `named("IO")` to declaration and injection |

### Debug Mode

```kotlin
startKoin {
    androidLogger(Level.DEBUG)  // prints all injections to Logcat
    // ...
}
```

---

## Checklist: New Dependency

- [ ] Type declared in appropriate module (`repository`, `domain`, or `viewmodel`)
- [ ] Correct scope used (`single` vs `factory` vs `viewModel`)
- [ ] Interface bound to implementation: `single<Interface> { Impl(get()) }`
- [ ] Named qualifier used for dispatchers
- [ ] Module registered in `Application.startKoin { modules(...) }`
- [ ] Compiles and `./gradlew test` passes

---

## References

- [Koin Documentation](https://insert-koin.io/docs/quickstart/android)
- [Koin for Compose](https://insert-koin.io/docs/reference/koin-android/compose)
- [Koin Testing](https://insert-koin.io/docs/reference/koin-test/testing)
