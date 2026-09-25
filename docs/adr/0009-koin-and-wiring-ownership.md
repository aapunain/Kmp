# ADR-0009: Koin for DI, with wiring ownership following visibility

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

Implementations are `internal` so that layering is enforced by the compiler. But that
creates a problem for the composition root: `:app:shared` cannot name
`ItemsRepositoryImpl`, so it cannot construct it. Something has to bridge that.

Options were Koin, a hand-written graph object, or Kodein. Manual DI was recommended
for readability; the project owner chose Koin.

## Decision

Koin 4.2.2. The wiring rule:

**Ownership of DI wiring follows visibility.**

- A module with `internal` implementations **publishes its own Koin module** as its
  only DI surface: `dataModule()`, `networkModule()`, `presentationModule()`,
  `concurrencyModule()`. Nobody else can name the classes, so nobody else can wire them.
- A module whose API is entirely public **is wired from the composition root**.
  `:core:domain`'s use cases are public, so `domainModule()` lives in
  `:app:shared/di/DomainModule.kt`. This is what lets `:core:domain` depend on
  nothing ([ADR-0004](0004-domain-has-no-dependencies.md)).

Koin starts from the `KoinApplication` **composable** inside `App()`, not from a
platform lifecycle callback:

```kotlin
@Composable
public fun App() {
    KoinApplication(application = {
        modules(concurrencyModule(), networkModule(), dataModule(), domainModule(), presentationModule())
    }) { /* ... */ }
}
```

One startup path for Android, iOS, desktop, JS and Wasm. No `Application.onCreate`, no
`iOSApp.init`, no duplication across `main()` functions.

## Consequences

Easy: `:app:shared` lists five functions and never sees an implementation class.
Adding a binding touches exactly one file, inside the module that owns the class.
Startup is identical on every platform.

Hard: Koin resolves at **runtime**, so a compiling build proves nothing about the
graph. That gap is closed by `CompositionRootTest`, which starts the real graph and
pulls a use case through it — it runs on all five targets.

`domainModule()` living outside `:core:domain` is mildly surprising; its KDoc says so
explicitly and explains why.

## Enforcement

- `explicitApi()` plus `internal` makes an unwired implementation unreachable.
- `CompositionRootTest` fails if any binding is missing, on every target.

## Alternatives considered

**Manual DI via a hand-written graph object.** Recommended: zero dependencies, and the
graph is ordinary readable Kotlin. Not chosen; Koin is the more common production setup.

**Koin Annotations / KSP.** Rejected for now: more build complexity, and generated
wiring is harder to follow than a `module { }` block.

**Start Koin per platform.** Rejected: five startup paths that can drift, for no gain.
