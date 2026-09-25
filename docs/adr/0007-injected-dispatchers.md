# ADR-0007: Inject dispatchers; ban `Dispatchers.IO` from common code

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

`Dispatchers.IO` is not portable. It exists on JVM and Android, does not exist on JS
or Wasm — single event loop, nothing to offload to — and in kotlinx-coroutines 1.11.0
it is declared `internal` on Apple targets:

```
e: Cannot access 'val IO: CoroutineDispatcher': it is internal in 'kotlinx.coroutines.Dispatchers'.
```

That error was produced by this project's own `:core:concurrency:compileKotlinIosArm64`.

Separately, `:core:data` must stay free of platform source sets so that the data
layer is 100% common code.

## Decision

A `DispatcherProvider` interface in its own module, `:core:concurrency`, injected via
Koin wherever it is needed:

```kotlin
public interface DispatcherProvider {
    public val main: CoroutineDispatcher
    public val default: CoroutineDispatcher
    public val io: CoroutineDispatcher
}

internal expect val platformIoDispatcher: CoroutineDispatcher
```

Five one-line actuals: `Dispatchers.IO` for android and jvm, `Dispatchers.Default` for
ios, js and wasmJs. On Apple targets `Default` is backed by a real multi-threaded
pool, which is the closest correct substitute; genuinely blocking work there wants a
dedicated `newFixedThreadPoolContext`, not this dispatcher.

`Dispatchers.IO` may not be referenced anywhere outside `core/concurrency/src/`.

Note the module placement: dispatchers are **infrastructure**, so this interface does
not go in `:core:domain`. Only genuine domain capabilities do.

## Consequences

Easy: `:core:data` has no platform source sets at all. Repository tests are
deterministic on every target by injecting `UnconfinedTestDispatcher` — there is
nothing to substitute if code references `Dispatchers.IO` statically, and on JS and
Wasm such code would not even compile.

Hard: a whole Gradle module for one interface and five one-line files. Also,
`withContext(dispatchers.io)` around a pure Ktor call is technically redundant, since
Ktor already suspends without blocking; the code says so in a comment. It earns its
place once mapping becomes expensive or disk access appears, and it earns its place
today in tests.

## Enforcement

- `./gradlew conventionsCheck` fails on any `Dispatchers.IO` reference outside
  `core/concurrency/src/`. It strips comments first, so documentation may discuss the
  API without tripping the rule.
- `DefaultDispatcherProviderTest` runs `withContext(dispatchers.io)` on all five
  targets, so a regression fails at runtime as well as at compile time.

## Alternatives considered

**Reference `Dispatchers.IO` directly and accept JVM-only code.** Not viable; four of
five targets would not compile.

**`Dispatchers.Default` everywhere, no abstraction.** Rejected: nothing to substitute
in tests, and it hides a real platform difference.

**Put `DispatcherProvider` in `:core:domain`.** Rejected: dispatchers are a technical
concern, and putting them there would breach [ADR-0004](0004-domain-has-no-dependencies.md).

**Declare it in `:core:data` and supply actuals from `:app:shared`.** Rejected: it
scatters platform wiring and leaves no home for dispatchers if another layer needs them.
