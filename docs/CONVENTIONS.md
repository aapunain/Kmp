# Conventions

Code-level rules. Module-level rules are in [ARCHITECTURE.md](ARCHITECTURE.md); the
reasoning behind each decision is in [adr/](adr/).

## Package layout

Gradle paths and Kotlin packages are intentionally independent. Packages name the
**layer plus the feature**, so moving a module between directories costs no import
changes:

```
:contract       com.self.kmp.contract[.<feature>]
:core:domain    com.self.kmp.domain.<feature>.{model, repository, usecase}
:core:data      com.self.kmp.data.<feature>.{repository, mapper}   + .common
:core:network   com.self.kmp.network.core                          + .<feature>.{dto, datasource}
:core:concurrency  com.self.kmp.concurrency
:presentation   com.self.kmp.presentation.<feature>
:app:shared     com.self.kmp[.di]
```

Packaging by feature inside each layer is what makes a later split into
`feature/<name>/` a folder move rather than an untangling job.

## The error model

Two result types, deliberately. See [ADR-0005](adr/0005-two-result-types.md).

```
:core:network   ApiResult<T> / ApiError    HTTP status codes live here
      │
      │  ApiError -> AppError, DTO -> domain model
      ▼         (ApiErrorMapper + ItemMapper, the only translation point)
:core:domain    AppResult<T> / AppError    no transport vocabulary at all
      │
      ▼         AppError -> String via toUiMessage()
:presentation   ItemsUiState.errorMessage  a sentence a user can read
```

Rules:

- Remote data sources **return** `ApiResult`, they do not throw. Every Ktor
  exception is classified in exactly one place, `safeApiCall`.
- `safeApiCall` must rethrow `CancellationException` before its generic catch.
  Swallowing it breaks structured concurrency.
- `AppError` contains no HTTP codes, no exceptions, no Ktor types. If you need to
  add a case, express it in domain language (`ServerUnavailable`, not `Http(503)`).
- `:presentation` cannot see `ApiResult` at all — it does not depend on
  `:core:network`. That is enforced by the compiler, not by discipline.

## Presentation: MVI-lite

One immutable state class, one sealed intent type, one entry point.

```kotlin
public data class ItemsUiState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),   // display-ready, not List<Item>
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

- `UiState` holds **display-ready** types. Mapping domain to display happens in the
  ViewModel so the domain model never grows fields that exist only for a layout.
- Business rules live in use cases, not ViewModels. Filtering and sorting the item
  list is `GetItemsUseCase`'s job.
- **State and one-shot effects are different things.** Navigation, snackbars and
  toasts are not state; replaying them on recomposition causes duplicate
  navigation. When you need them, add a separate `Channel<UiEffect>` — do not put
  them in `UiState`.
- Screens split in two: a stateful `XScreen()` that resolves the ViewModel via
  `koinViewModel()`, and a stateless `internal XContent(state, onIntent)` that can
  be previewed and tested with a hand-built state.

## Dependency injection

Koin. See [ADR-0009](adr/0009-koin-and-wiring-ownership.md).

- Every module with `internal` implementations publishes one function returning a
  Koin `Module`: `dataModule()`, `networkModule()`, `presentationModule()`,
  `concurrencyModule()`. That is the module's only DI surface.
- `:core:domain` is the exception: its use cases are public, so `domainModule()`
  lives in `:app:shared/di/`. Ownership of wiring follows visibility.
- Koin starts from the `KoinApplication` composable inside `App()`, not from a
  platform lifecycle callback. One startup path for all five platforms.
- ViewModels are registered with `viewModel { }` and resolved with `koinViewModel()`.

## Visibility

`explicitApi()` is enabled, so this is enforced rather than suggested.

- Implementations are `internal`: `ItemsRepositoryImpl`, `KtorItemsRemoteDataSource`,
  `createHttpClient`, all mappers.
- Interfaces, models, result types and DI functions are `public`, spelled out.
- If you find yourself making something `public` so another module can reach it,
  stop: that is usually the signal that the code is in the wrong module.

## Platform-specific code

Two different situations, two different answers. See
[ADR-0007](adr/0007-injected-dispatchers.md).

- **Infrastructure detail** the business logic knows nothing about (HTTP engine,
  database driver, dispatchers): `expect`/`actual` inside the module that owns it,
  kept `internal`. It must not appear in `:core:domain`.
- **Domain capability** the business logic genuinely cares about (biometrics,
  location, secure storage): an interface in `:core:domain`, implementation supplied
  per platform, injected via Koin.

Test: would a backend engineer reading `:core:domain` recognise every type as a
business concept? If not, it is in the wrong place.

## Naming

- `actual` files carry a platform suffix: `Platform.ios.kt`, `Platform.android.kt`.
  detekt's `MatchingDeclarationName` is disabled because of this convention.
- `@Composable` functions are PascalCase; ktlint's `function-naming` and detekt's
  `FunctionNaming` are configured accordingly.
- Files are PascalCase, including entry points: `Main.kt`, not `main.kt`.
- DTO types end in `Dto` and live only in `:contract`.
- Use cases are named for the action: `GetItemsUseCase`, invoked via `operator fun invoke`.

## Formatting

Spotless + ktlint, configured by `.editorconfig` so the IDE and the build agree.
Run `./gradlew spotlessApply`. Do not hand-format; do not argue with it.

One quirk: ktlint rejects KDoc (`/** */`) in `.gradle.kts` script bodies, because
those comments are not attached to a declaration. Use `//` comments in build files.
