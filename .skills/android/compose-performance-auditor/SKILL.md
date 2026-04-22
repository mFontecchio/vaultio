---
name: compose-performance-auditor
description: Audit and fix Jetpack Compose runtime performance issues. Use when diagnosing slow rendering, janky scrolling, excessive recompositions, or frame drops in Compose UI. Performs code-first review and guides profiling when needed.
---

# Compose Performance Auditor

## Overview

Audit Compose UI performance end-to-end: from code review to root-cause analysis and concrete fixes.

## Workflow

```
Code available? ──Yes──▶ Code-First Review
      │
      No
      ▼
Describe symptoms ──▶ Ask for code/context ──▶ Code-First Review
      │
      If inconclusive
      ▼
Guide Profiling (Layout Inspector / Perfetto)
```

---

## 1. Code-First Review

### What to Collect

- Target composable code
- Data flow: state, `remember`, ViewModel connections
- Symptoms and reproduction steps

### What to Look For

| Issue                                 | Symptom                                                    |
|---------------------------------------|------------------------------------------------------------|
| Recomposition storms                  | Whole tree recomposes on small state change                |
| Unstable lambda captures              | New lambda instance every recomposition                    |
| Missing `remember`                    | Expensive object recreated every frame                     |
| Unstable class parameters             | Compose can't skip recomposition                           |
| Heavy work in composition             | Sorting / filtering / formatting during draw               |
| Unstable keys in lazy lists           | Items re-render on unrelated changes                       |
| State read too early                  | Entire composition recomposes instead of layout/draw phase |
| Large images without size constraints | Decoding full resolution unnecessarily                     |
| Deep nesting / intrinsic measurements | Layout pass is O(n²)                                       |

---

## 2. Common Code Smells and Fixes

### Unstable Lambda Captures

```kotlin
// ❌ New lambda created every recomposition
Button(onClick = { viewModel.doAction(item.id) }) { Text("Action") }

// ✅ Stable reference
val onAction = remember(item.id) { { viewModel.doAction(item.id) } }
Button(onClick = onAction) { Text("Action") }
```

### Expensive Work in Composition

```kotlin
// ❌ Sorts on every recomposition
@Composable fun ItemList(items: List<Item>) {
    val sorted = items.sortedBy { it.name }
    LazyColumn { items(sorted) { ItemRow(it) } }
}

// ✅ Computed once per unique input
@Composable fun ItemList(items: List<Item>) {
    val sorted = remember(items) { items.sortedBy { it.name } }
    LazyColumn { items(sorted) { ItemRow(it) } }
}
```

### Missing Key in LazyColumn

```kotlin
// ❌ Index-based — redraws all items on insert/delete
LazyColumn { items(items) { ItemRow(it) } }

// ✅ Stable key — only changed items recompose
LazyColumn { items(items, key = { it.id }) { ItemRow(it) } }
```

### Unstable Data Classes

```kotlin
// ❌ List<T> is unstable — Compose cannot skip recomposition
data class UiState(val items: List<Item>, val isLoading: Boolean)

// ✅ Annotate as @Immutable (if truly never mutated externally)
@Immutable
data class UiState(val items: List<Item>, val isLoading: Boolean)

// ✅ Or use kotlinx.collections.immutable
@Immutable
data class UiState(val items: ImmutableList<Item>, val isLoading: Boolean)
```

### State Read Too Early (Defer to Layout/Draw Phase)

```kotlin
// ❌ Read during composition — whole tree recomposes on every scroll
@Composable fun ParallaxHeader(scrollState: ScrollState) {
    val offset = scrollState.value  // read in composition phase
    Image(modifier = Modifier.offset(y = offset.dp)) { ... }
}

// ✅ Defer read to layout phase
@Composable fun ParallaxHeader(scrollState: ScrollState) {
    Image(modifier = Modifier.offset { IntOffset(0, scrollState.value) }) { ... }
}

// ✅ Defer read to draw phase
@Composable fun ParallaxHeader(scrollState: ScrollState) {
    Box(modifier = Modifier.drawBehind {
        val y = scrollState.value.toFloat()
        drawRect(color = Color.Red, topLeft = Offset(0f, y))
    })
}
```

### `derivedStateOf` Misuse

`derivedStateOf` only observes **`State<T>` reads inside its lambda**. Captured plain values (
unwrapped via `by`) are not tracked.

```kotlin
// ❌ animatedValue is already a plain Float (unwrapped by `by`)
val animated by animateFloatAsState(target)
val text by remember { derivedStateOf { animated.toInt().toString() } }
// `text` never updates — animated is a Float, not a State<Float>

// ✅ Just compute directly; animateFloatAsState triggers recomposition anyway
val animated by animateFloatAsState(target)
val text = animated.toInt().toString()

// ✅ Or read the raw State object inside derivedStateOf
val animatedState = animateFloatAsState(target)  // no `by`
val text by remember { derivedStateOf { animatedState.value.toInt().toString() } }
```

### Object Allocation in Composition

```kotlin
// ❌ New Modifier created every recomposition
Box(modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(8.dp)))

// ✅ Remember static modifiers
val cardModifier = remember { Modifier.padding(16.dp).clip(RoundedCornerShape(8.dp)) }
Box(modifier = cardModifier)
```

---

## 3. Stability Checklist

| Type                                             | Stable? | Fix                                         |
|--------------------------------------------------|---------|---------------------------------------------|
| Primitives (`Int`, `String`, `Boolean`, `Float`) | ✅ Yes   | —                                           |
| `data class` with only stable fields             | ✅ Yes   | Ensure all fields stable                    |
| `List`, `Map`, `Set`                             | ❌ No    | Use `ImmutableList` / `@Immutable`          |
| Classes with `var`                               | ❌ No    | Add `@Stable` if contractually stable       |
| Lambda `{}`                                      | ❌ No    | `remember { }` when capturing mutable state |
| Enum, sealed class                               | ✅ Yes   | —                                           |

---

## 4. Guide to Profiling

When code review is inconclusive, collect runtime data:

### Layout Inspector (Android Studio)

1. Run app in **debug** mode
2. Open **Layout Inspector** → connect to process
3. Enable **Show Recomposition Counts**
4. Interact with the slow screen
5. Identify composables with high recomposition counts

> ⚠️ Debug builds have overhead. Use for counting recompositions, not absolute timing.

### Perfetto / System Trace

1. Build a **release** build (R8 enabled, no debug overhead)
2. Open **Android Studio → Profiler → CPU → Record**
3. Choose **System Trace** or export to Perfetto
4. Look for: jank frames (>16ms), long Choreographer frames, slow `measure`/`layout`/`draw`

### Macrobenchmark

For repeatable, CI-comparable metrics:

```kotlin
@Test
fun scrollBenchmark() = benchmarkRule.measureRepeated(
    packageName = "com.example.myapp",
    metrics = listOf(FrameTimingMetric()),
    startupMode = StartupMode.COLD,
) {
    pressHome()
    startActivityAndWait()
    device.findObject(By.res("list")).scroll(Direction.DOWN, 10)
}
```

---

## 5. Output Format

Report findings as:

```
## Compose Performance Report

### Summary
- X recomposition storms detected
- Y unstable types found
- Z missing `remember` blocks

### Findings (ordered by impact)

#### 1. [High] Unstable lambda in `ItemList`
- File: src/.../ItemList.kt:42
- Issue: New lambda created every recomposition
- Fix: Wrap in `remember(item.id) { { ... } }`

#### 2. [Medium] Missing key in LazyColumn
...

### Estimated Impact
- Before: ~30 recompositions/interaction
- After (estimated): ~5 recompositions/interaction
```

---

## References

- [Jetpack Compose Performance](https://developer.android.com/develop/ui/compose/performance)
- [Compose Stability Explained](https://developer.android.com/develop/ui/compose/performance/stability)
- [Debugging Recomposition](https://developer.android.com/develop/ui/compose/tooling/layout-inspector)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
- [Compose phases (composition / layout / draw)](https://developer.android.com/develop/ui/compose/phases)
