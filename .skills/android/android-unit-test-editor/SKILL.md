---
name: android-unit-test-editor
description: Guide for writing unit tests for Android Kotlin code — ViewModels, Use Cases, Repositories, and utility classes. Uses MockK and Kotlin Coroutines Test. Framework-agnostic patterns following GIVEN/WHEN/THEN structure.
---

# Android Unit Test Editor

## Overview

Unit tests validate isolated business logic: Use Cases, ViewModels, Repositories, mappers, and
extensions. This guide uses **MockK** for mocking and **kotlinx.coroutines.test** for coroutine
testing.

---

## Setup

```toml
# gradle/libs.versions.toml
[versions]
mockk = "1.13.12"
coroutines-test = "1.8.1"
turbine = "1.1.0"

[libraries]
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines-test" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
junit = { module = "junit:junit", version = "4.13.2" }
```

```kotlin
// module/build.gradle.kts
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

---

## Naming Convention

```
SHOULD [expected behavior] WHEN [action/event] GIVEN [precondition]
```

Examples:

- `` `SHOULD update state to loading WHEN load event received GIVEN initial state` ``
- `` `SHOULD return error WHEN network call fails GIVEN no cached data` ``
- `` `SHOULD emit navigation event WHEN submit clicked GIVEN valid form` ``

---

## Test Structure

```kotlin
@Test
fun `SHOULD ... WHEN ... GIVEN ...`() = runTest {
    // GIVEN
    val input = "test"
    coEvery { mockDependency.fetch(input) } returns Result.success(expectedData)

    // WHEN
    sut.doAction(input)
    advanceUntilIdle()

    // THEN
    assertEquals(expectedData, sut.state.value.data)
}
```

---

## Use Case Tests

```kotlin
class GetOrdersUseCaseTest {

    private val repository: OrderRepository = mockk()
    private lateinit var sut: GetOrdersUseCase

    @Before
    fun setup() {
        sut = GetOrdersUseCase(
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `SHOULD return success WHEN repository returns data`() = runTest {
        // GIVEN
        val expected = listOf(Order("1", "Pizza"))
        coEvery { repository.getOrders() } returns Result.success(expected)

        // WHEN
        val result = sut()

        // THEN
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `SHOULD return failure WHEN repository throws`() = runTest {
        // GIVEN
        coEvery { repository.getOrders() } returns Result.failure(IOException("Net error"))

        // WHEN
        val result = sut()

        // THEN
        assertTrue(result.isFailure)
    }
}
```

---

## ViewModel Tests

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
        sut.onViewEvent(OrderViewEvent.OnScreenOpened)
        advanceUntilIdle()

        // THEN
        with(sut.state.value) {
            assertEquals(orders, this.orders)
            assertFalse(isLoading)
            assertNull(errorMessage)
        }
    }

    @Test
    fun `SHOULD set error message WHEN load fails`() = runTest {
        // GIVEN
        coEvery { getOrdersUseCase() } returns Result.failure(RuntimeException("Fail"))

        // WHEN
        sut.onViewEvent(OrderViewEvent.OnScreenOpened)
        advanceUntilIdle()

        // THEN
        assertNotNull(sut.state.value.errorMessage)
        assertFalse(sut.state.value.isLoading)
    }
}
```

### MainDispatcherRule

Required for `viewModelScope.launch` to work in unit tests:

```kotlin
class MainDispatcherRule(
    private val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
        dispatcher.cleanupTestCoroutines()
    }
}
```

Or with `StandardTestDispatcher` (preferred in coroutines-test 1.6+):

```kotlin
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

---

## Flow Testing with Turbine

[Turbine](https://github.com/cashapp/turbine) provides a clean API for testing Flows:

```kotlin
@Test
fun `SHOULD emit navigation effect WHEN order clicked`() = runTest {
    // GIVEN
    val orderId = "order-123"
    coEvery { getOrdersUseCase() } returns Result.success(listOf(Order(orderId, "Pizza")))

    // WHEN + THEN
    sut.sideEffects.test {
        sut.onViewEvent(OrderViewEvent.OnOrderClicked(orderId))
        val effect = awaitItem()
        assertTrue(effect is OrderSideEffect.Navigation.GoToOrderDetail)
        assertEquals(orderId, (effect as OrderSideEffect.Navigation.GoToOrderDetail).orderId)
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## Repository Tests

```kotlin
class OrderRepositoryImplTest {

    private val localDataSource: OrderLocalDataSource = mockk()
    private val remoteDataSource: OrderRemoteDataSource = mockk()
    private lateinit var sut: OrderRepository

    @Before
    fun setup() {
        sut = OrderRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `SHOULD return local data WHEN cache is valid`() = runTest {
        // GIVEN
        val cached = listOf(Order("1", "Sushi"))
        coEvery { localDataSource.getOrders() } returns cached

        // WHEN
        val result = sut.getOrders()

        // THEN
        assertEquals(cached, result.getOrNull())
        coVerify(exactly = 0) { remoteDataSource.getOrders() }  // remote NOT called
    }

    @Test
    fun `SHOULD fetch remote and save WHEN cache is empty`() = runTest {
        // GIVEN
        coEvery { localDataSource.getOrders() } returns emptyList()
        val remote = listOf(Order("2", "Ramen"))
        coEvery { remoteDataSource.getOrders() } returns Result.success(remote)
        coEvery { localDataSource.saveOrders(any()) } just Runs

        // WHEN
        val result = sut.getOrders()

        // THEN
        assertEquals(remote, result.getOrNull())
        coVerify { localDataSource.saveOrders(remote) }
    }
}
```

---

## MockK Quick Reference

```kotlin
// Mock creation
val mockRepo: OrderRepository = mockk()
val spyService: MyService = spyk(MyService())   // real object + partial mocking
val relaxedMock: LogService = mockk(relaxed = true)  // all functions return defaults

// Stubbing
every { mockRepo.isInitialized } returns true
coEvery { mockRepo.getOrders() } returns Result.success(orders)
every { mockRepo.getOrders() } throws RuntimeException("fail")
coEvery { mockRepo.save(any()) } just Runs       // Unit functions

// Capturing
val slot = slot<Order>()
coEvery { mockRepo.save(capture(slot)) } just Runs
// After call: slot.captured → the Order that was passed

// Verification
verify { mockRepo.isInitialized }
coVerify { mockRepo.getOrders() }
coVerify(exactly = 0) { mockRepo.getOrders() }   // never called
coVerify(atLeast = 2) { mockRepo.getOrders() }
verifyOrder { mockRepo.getOrders(); mockRepo.save(any()) }

// Argument matchers
coEvery { mockRepo.find(any()) } returns order
coEvery { mockRepo.find(match { it.startsWith("order") }) } returns order
```

---

## Parameterized Tests

```kotlin
@RunWith(Parameterized::class)
class EmailValidatorTest(private val email: String, private val isValid: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} → {1}")
        fun data() = listOf(
            arrayOf("user@example.com", true),
            arrayOf("notanemail", false),
            arrayOf("@missing.com", false),
            arrayOf("", false),
        )
    }

    @Test
    fun `validate email`() {
        assertEquals(isValid, EmailValidator.isValid(email))
    }
}
```

---

## Checklist: New Test Class

- [ ] Test file mirrors source structure (`test/` mirrors `main/`)
- [ ] Follows `SHOULD...WHEN...GIVEN` naming
- [ ] `@Before` sets up fresh SUT per test
- [ ] `MainDispatcherRule` applied for ViewModel tests
- [ ] `runTest` used for suspend functions
- [ ] `advanceUntilIdle()` called after async operations
- [ ] All mock interactions verified where relevant
- [ ] Edge cases covered (empty, error, boundary values)

---

## References

- [MockK documentation](https://mockk.io/)
- [kotlinx.coroutines.test](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Turbine](https://github.com/cashapp/turbine)
- [Android Testing Guide](https://developer.android.com/training/testing)
