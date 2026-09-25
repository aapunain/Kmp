# ADR-0008: MVI-lite presentation with display-ready state

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

"MVVM/MVI" describes two patterns. Left unresolved, different developers and agents
would implement different shapes in the same codebase — some with one public method
per user action, some with an intent channel, some exposing domain models to Compose.

`androidx.lifecycle.ViewModel` is genuinely multiplatform at 2.11.0, so no
per-platform wrapper is required.

## Decision

One immutable state class, one sealed intent type, one entry point:

```kotlin
public data class ItemsUiState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val errorMessage: String? = null,
)

public sealed interface ItemsIntent {
    public data object Load : ItemsIntent
    public data object Retry : ItemsIntent
}

public class ItemsViewModel(private val getItems: GetItemsUseCase) : ViewModel() {
    public val state: StateFlow<ItemsUiState>
    public fun onIntent(intent: ItemsIntent)
}
```

Three further rules:

1. **`UiState` holds display-ready types.** `List<String>`, not `List<Item>`. Mapping
   happens in the ViewModel, so the domain model never grows fields that exist only
   to satisfy a layout.
2. **State and one-shot effects are separate.** Navigation, snackbars and toasts are
   not state; replaying them on recomposition causes duplicate navigation. Use a
   `Channel<UiEffect>` when needed.
3. **Screens split in two.** A stateful `XScreen()` resolving the ViewModel via
   `koinViewModel()`, and a stateless `internal XContent(state, onIntent)` that is
   previewable and testable with a hand-built state.

Business rules stay in use cases. `GetItemsUseCase` does the filtering and ordering,
not the ViewModel.

## Consequences

Easy: adding a user action is a compile error until the `when` in `onIntent` handles
it. State is a single value, so there is no combination of half-updated fields. The
stateless content function is trivially testable.

Hard: more types than a handful of public methods would need. A single state class
means an unrelated field change recomposes readers of the whole state unless the
composable reads narrowly.

## Enforcement

Review only. The shape is documented in
[CONVENTIONS.md](../CONVENTIONS.md#presentation-mvi-lite) and `ItemsViewModel` is the
reference implementation.

## Alternatives considered

**Plain MVVM with one method per action and several `StateFlow`s.** Rejected: multiple
flows permit inconsistent intermediate states, and there is no exhaustiveness check.

**Full MVI with a reducer and middleware.** Rejected as disproportionate for this size.
The chosen shape satisfies both labels without the machinery.

**Expose domain models in `UiState`.** Rejected: it pulls formatting concerns into the
domain over time.
