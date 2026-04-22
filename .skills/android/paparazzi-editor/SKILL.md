---
name: paparazzi-editor
description: Guide for writing screenshot/snapshot tests for Jetpack Compose UI using Paparazzi. Use when adding visual regression tests for new screens or components, recording new snapshots, or diagnosing snapshot failures in CI.
---

# Paparazzi Editor

## Overview

[Paparazzi](https://github.com/cashapp/paparazzi) renders Compose UI on the JVM — no emulator or
device needed. It generates PNG snapshots that act as visual regression baselines committed to the
repository.

---

## Setup

### Dependencies

```toml
# gradle/libs.versions.toml
[versions]
paparazzi = "1.3.5"

[plugins]
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
```

```kotlin
// module/build.gradle.kts
plugins {
    alias(libs.plugins.paparazzi)
}
```

> Paparazzi applies its own JVM renderer — no Android emulator required.

---

## Basic Test Structure

```kotlin
class HomeScreenPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun `home screen default state`() {
        paparazzi.snapshot {
            MyAppTheme {
                HomeScreen(
                    state = HomeViewState(
                        isLoading = false,
                        items = sampleItems(),
                    ),
                    onEvent = {},
                    sideEffects = emptyFlow(),
                    onNavigation = {},
                )
            }
        }
    }
}
```

---

## Multi-State Snapshots

Test all relevant states explicitly:

```kotlin
class OrderListScreenTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `loading state`() {
        paparazzi.snapshot("loading") {
            MyAppTheme {
                OrderListScreen(
                    state = OrderListViewState(isLoading = true),
                    onEvent = {},
                    sideEffects = emptyFlow(),
                    onNavigation = {},
                )
            }
        }
    }

    @Test
    fun `empty state`() {
        paparazzi.snapshot("empty") {
            MyAppTheme {
                OrderListScreen(
                    state = OrderListViewState(orders = emptyList()),
                    onEvent = {},
                    sideEffects = emptyFlow(),
                    onNavigation = {},
                )
            }
        }
    }

    @Test
    fun `populated state`() {
        paparazzi.snapshot("populated") {
            MyAppTheme {
                OrderListScreen(
                    state = OrderListViewState(orders = sampleOrders()),
                    onEvent = {},
                    sideEffects = emptyFlow(),
                    onNavigation = {},
                )
            }
        }
    }

    @Test
    fun `error state`() {
        paparazzi.snapshot("error") {
            MyAppTheme {
                OrderListScreen(
                    state = OrderListViewState(errorMessage = "Connection failed"),
                    onEvent = {},
                    sideEffects = emptyFlow(),
                    onNavigation = {},
                )
            }
        }
    }
}
```

---

## Dark Mode with TestParameterInjector

Reduce boilerplate for light/dark variants:

```kotlin
@RunWith(TestParameterInjector::class)
class OrderCardSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    enum class Theme(val uiMode: Int) {
        Light(Configuration.UI_MODE_NIGHT_NO),
        Dark(Configuration.UI_MODE_NIGHT_YES),
    }

    @Test
    fun `order card`(@TestParameter theme: Theme) {
        paparazzi.snapshot(theme.name.lowercase()) {
            CompositionLocalProvider(
                LocalConfiguration provides Configuration().apply { uiMode = theme.uiMode }
            ) {
                MyAppTheme {
                    Surface {
                        OrderCard(
                            order = Order("1", "Pizza Margherita", 12.99),
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}
```

---

## Device Configurations

```kotlin
// Common device configs
DeviceConfig.PIXEL_5           // 1080x2340, 440dpi
DeviceConfig.NEXUS_5           // 1080x1920, 480dpi
DeviceConfig.PIXEL_C           // Tablet

// Custom config
DeviceConfig(
    screenWidth = 360,
    screenHeight = 800,
    xdpi = 420,
    ydpi = 420,
    orientation = ScreenOrientation.PORTRAIT,
    nightMode = NightMode.NIGHT,
    fontScale = 1.5f,           // Large text accessibility test
)
```

---

## Font Scale Tests

```kotlin
@Test
fun `order card - large text`() {
    paparazzi.snapshot("large_font") {
        MyAppTheme {
            Surface {
                OrderCard(
                    order = sampleOrder(),
                    onClick = {},
                )
            }
        }
    }
}

// Paparazzi config with font scale
val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5.copy(fontScale = 1.5f)
)
```

---

## Gradle Commands

```bash
# Record new snapshots (creates PNG baselines)
./gradlew :feature:orders:recordPaparazziDebug

# Verify against existing baselines (CI)
./gradlew :feature:orders:verifyPaparazziDebug

# Run all snapshot tests
./gradlew verifyPaparazziDebug
```

> **Commit snapshots** to the repository. They are the baseline for visual regression detection.

---

## Snapshot File Structure

After recording, snapshots are saved at:

```
feature/orders/src/test/snapshots/images/
  com.example.feature.orders_OrderListScreenTest_loading.png
  com.example.feature.orders_OrderListScreenTest_populated.png
  com.example.feature.orders_OrderCardSnapshotTest_light.png
  com.example.feature.orders_OrderCardSnapshotTest_dark.png
```

---

## Deterministic Rendering

Paparazzi tests must be fully deterministic:

```kotlin
// ❌ Non-deterministic — will fail on second run
paparazzi.snapshot {
    Text(text = "Updated: ${System.currentTimeMillis()}")
}

// ✅ Static data
paparazzi.snapshot {
    Text(text = "Updated: Jan 1, 2024")
}

// ✅ Disable animations
paparazzi.snapshot {
    // Paparazzi freezes animations by default — no action needed for standard animations
    // For custom animated values, use static data in state
    MyComponent(progress = 0.75f)  // static, not animated
}
```

---

## Test Data Helpers

Create a shared file for snapshot test data:

```kotlin
// test/java/.../snapshots/SnapshotTestData.kt
object SnapshotTestData {
    fun sampleOrders() = listOf(
        Order(id = "1", name = "Pizza Margherita", price = 12.99),
        Order(id = "2", name = "Caesar Salad", price = 8.50),
        Order(id = "3", name = "Tiramisu", price = 5.00),
    )

    fun sampleUser() = User(
        id = "u1",
        name = "Jane Doe",
        email = "jane@example.com",
        avatarUrl = null,
    )
}
```

---

## CI Configuration

```yaml
# .github/workflows/snapshot_test.yml
- name: Verify Snapshots
  run: ./gradlew verifyPaparazziDebug

- name: Upload snapshot diffs on failure
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: snapshot-diffs
    path: '**/build/paparazzi/failures/'
```

---

## Updating Snapshots

When a UI change is intentional:

```bash
# 1. Re-record snapshots for affected module
./gradlew :feature:orders:recordPaparazziDebug

# 2. Review the diff in git
git diff -- '*.png'

# 3. Commit updated snapshots with your PR
git add feature/orders/src/test/snapshots/
git commit -m "chore: update order snapshots after layout change"
```

---

## Checklist: New Snapshot Test

- [ ] Test class named `[Screen/Component]PaparazziTest` or `[Screen/Component]SnapshotTest`
- [ ] All relevant states covered: loading, empty, error, populated
- [ ] Wrapped in app theme
- [ ] Static test data (no `System.currentTimeMillis()`, no random values)
- [ ] Light and dark mode covered (if app supports dark mode)
- [ ] Snapshots recorded and committed: `./gradlew recordPaparazziDebug`
- [ ] `./gradlew verifyPaparazziDebug` passes locally before push

---

## References

- [Paparazzi GitHub](https://github.com/cashapp/paparazzi)
- [Paparazzi documentation](https://cashapp.github.io/paparazzi/)
- [TestParameterInjector](https://github.com/google/TestParameterInjector)
