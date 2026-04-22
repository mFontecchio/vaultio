---
name: compose-editor
description: Guide for writing idiomatic, maintainable, and accessible Jetpack Compose UI. Use when creating new screens or components, reviewing existing Compose code, or refactoring XML layouts to Compose.
---

# Compose Editor

## Overview

This skill provides patterns and rules for writing Compose UI that is readable, testable, stable,
and accessible. It covers composable design, state management, side effects, accessibility, and
preview strategies.

---

## Composable Function Rules

### Naming

```kotlin
// ✅ PascalCase for composables
@Composable fun UserProfileCard(...) { }

// ❌ camelCase
@Composable fun userProfileCard(...) { }
```

### Parameter Order

```kotlin
@Composable
fun MyComponent(
    // 1. Required data params
    title: String,
    subtitle: String,
    // 2. Optional callbacks
    onClick: (() -> Unit)? = null,
    // 3. Modifier — always last before content, always with default
    modifier: Modifier = Modifier,
    // 4. Trailing composable lambda (if any)
    content: (@Composable () -> Unit)? = null,
)
```

### Single Responsibility

Split composables that exceed ~80 lines or handle more than one concern:

```kotlin
// ❌ Monolithic
@Composable fun ProfileScreen(user: User) {
    Column {
        // 150+ lines of header, bio, posts, actions
    }
}

// ✅ Decomposed
@Composable fun ProfileScreen(user: User) {
    Column {
        ProfileHeader(user)
        ProfileBio(user.bio)
        ProfilePostList(user.posts)
        ProfileActions(user.id)
    }
}
```

---

## State Management

### `remember` for Expensive Computations

```kotlin
// ❌ Runs on every recomposition
@Composable fun SortedList(items: List<Item>) {
    val sorted = items.sortedBy { it.name }
    LazyColumn { items(sorted) { ItemRow(it) } }
}

// ✅ Computed once per unique input
@Composable fun SortedList(items: List<Item>) {
    val sorted = remember(items) { items.sortedBy { it.name } }
    LazyColumn { items(sorted) { ItemRow(it) } }
}
```

### `derivedStateOf` for Dependent State

Use when you want to derive a value from **another `State<T>`** and avoid over-recomposition:

```kotlin
// ✅ Only recomposes when firstVisibleItemIndex crosses 0
@Composable fun BackToTopButton(listState: LazyListState) {
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    if (showButton) FloatingActionButton(onClick = { /* ... */ }) { ... }
}
```

> ⚠️ `derivedStateOf` only tracks **`State<T>` reads inside its lambda**. Captured plain `val`s (
> already unwrapped via `by`) are not tracked.

### `remember` vs `rememberSaveable`

| Need                                       | Use                                  |
|--------------------------------------------|--------------------------------------|
| Survive recomposition                      | `remember`                           |
| Survive screen rotation / process death    | `rememberSaveable`                   |
| Complex types (not `Parcelable`/primitive) | `rememberSaveable(stateSaver = ...)` |

---

## Side Effects

| Effect API               | When to Use                                                        |
|--------------------------|--------------------------------------------------------------------|
| `LaunchedEffect(key)`    | Start a coroutine when `key` changes (or once if `Unit`)           |
| `DisposableEffect(key)`  | Register/unregister listeners with cleanup via `onDispose`         |
| `SideEffect`             | Synchronize non-Compose state after every successful recomposition |
| `rememberCoroutineScope` | Trigger a coroutine from an event handler (e.g., button tap)       |

```kotlin
// Collect a Flow of side effects once
LaunchedEffect(Unit) {
    viewModel.sideEffects.collect { effect ->
        when (effect) {
            is MySideEffect.ShowToast -> context.toast(effect.message)
            is MySideEffect.Navigation -> onNavigation(effect)
        }
    }
}

// Lifecycle-aware listener
DisposableEffect(sensor) {
    sensor.register(listener)
    onDispose { sensor.unregister(listener) }
}
```

---

## Accessibility

### Content Descriptions

```kotlin
// ❌ Missing description
Icon(imageVector = Icons.Default.Favorite, contentDescription = null)

// ✅ Meaningful description
Icon(imageVector = Icons.Default.Favorite, contentDescription = "Add to favorites")
```

### Touch Target Size

Minimum 48dp × 48dp for interactive elements:

```kotlin
IconButton(
    onClick = { ... },
    modifier = Modifier.size(48.dp),  // ensure minimum tap area
) {
    Icon(Icons.Default.Close, contentDescription = "Close dialog")
}
```

### Semantic Roles

```kotlin
Box(
    modifier = Modifier.semantics {
        role = Role.Button
        contentDescription = "Open menu"
    }
)
```

### Screen Reader Order

Use `Modifier.semantics { traversalIndex = n }` to control reading order when visual order differs
from logical order.

---

## Theming

- **Never hardcode colors, sizes, or typography**
- Always read from `MaterialTheme.*` (or your design system's theme)

```kotlin
// ❌ Hardcoded
Text(text = "Hello", fontSize = 16.sp, color = Color(0xFF000000))

// ✅ Theme-aware
Text(text = "Hello", style = MaterialTheme.typography.bodyLarge)
```

---

## Previews

Always create `@Preview` functions alongside every composable:

```kotlin
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OrderCardPreview() {
    MyAppTheme {
        Surface {
            OrderCard(
                order = Order(id = "1", title = "Pizza Margherita", price = 12.99),
                onClick = {},
            )
        }
    }
}
```

Use multiple previews to cover: loading, empty, error, and populated states.

---

## Lazy Lists

```kotlin
// ❌ No key — identity based on index, causes full re-render on insert/delete
LazyColumn {
    items(items) { item -> ItemRow(item) }
}

// ✅ Stable key — only changed items recompose
LazyColumn {
    items(items, key = { it.id }) { item -> ItemRow(item) }
}
```

For sticky headers use `stickyHeader { }`. For mixed content types use `itemsIndexed` or custom
content types via `contentType`.

---

## State Hoisting

Push state **up** to the lowest common ancestor that needs it. Composables should be stateless when
possible:

```kotlin
// ✅ State hoisted — composable is pure and testable
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(value = query, onValueChange = onQueryChange, modifier = modifier)
}

// Caller owns the state
var query by remember { mutableStateOf("") }
SearchBar(query = query, onQueryChange = { query = it })
```

---

## Common Patterns

### Loading / Error / Content

```kotlin
when {
    state.isLoading -> CircularProgressIndicator()
    state.error != null -> ErrorMessage(state.error)
    state.items.isEmpty() -> EmptyState()
    else -> ContentList(state.items)
}
```

### Pull-to-Refresh

```kotlin
val pullState = rememberPullToRefreshState()
LaunchedEffect(pullState.isRefreshing) {
    if (pullState.isRefreshing) {
        onRefresh()
    }
}
LaunchedEffect(isLoading) {
    if (!isLoading) pullState.endRefresh()
}
```

### Keyboard Navigation (IME)

```kotlin
val focusManager = LocalFocusManager.current
val keyboard = LocalSoftwareKeyboardController.current

TextField(
    ...,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
)
// Last field
TextField(
    ...,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
)
```

---

## Checklist: New Composable

- [ ] PascalCase naming
- [ ] Parameters in correct order (data → callbacks → modifier → content)
- [ ] `remember` used for expensive calculations
- [ ] No business logic inside composable
- [ ] No hardcoded colors, dimensions, or typography
- [ ] Content descriptions on all icons/images
- [ ] `@Preview` with at least light + dark mode
- [ ] State hoisted if composable can be stateless
- [ ] `key` provided in `LazyColumn`/`LazyRow`

---

## References

- [Compose API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
- [Side Effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
- [Compose Accessibility](https://developer.android.com/develop/ui/compose/accessibility)
