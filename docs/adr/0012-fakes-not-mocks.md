# ADR-0012: Hand-written fakes, and every test runs on every target

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

MockK and Mockito are JVM-only. They rely on bytecode generation and reflection, which
do not exist on Kotlin/Native, Kotlin/JS or Kotlin/Wasm. Code in `commonTest` compiles
for all five targets, so adding a mocking library would break four of them.

This is not a limitation to work around; it forces a better default.

## Decision

Hand-written fakes, in `commonTest`, running on every target:

```kotlin
private class FakeItemsRepository(
    private val result: AppResult<List<Item>>,
) : ItemsRepository {
    override suspend fun getItems(): AppResult<List<Item>> = result
}
```

Per layer:

| Layer | Test approach |
| --- | --- |
| `:core:domain` | Plain `commonTest`, no infrastructure |
| `:core:data` | Fake data source + injected test dispatcher |
| `:core:network` | Ktor `MockEngine` |
| `:presentation` | Fake use case + `Dispatchers.setMain` |
| `:app:shared` | Real Koin graph, end to end |

Tests belong in `commonTest` unless they are genuinely platform-specific. A test
placed only in `jvmTest` forfeits the four targets where the interesting failures live.

## Consequences

Easy: tests assert behaviour rather than which methods were called, so they survive
refactoring. A fake forces the interface to stay small. The same test covers five
platforms.

Hard: a fake must be written by hand for each interface, and complex interaction
verification is awkward. Both are acceptable — awkwardness here is usually a signal
the interface is too large.

## Enforcement

- `./gradlew verify` runs `commonTest` on JVM, Android host, iOS simulator, Wasm and JS.
- Adding MockK would fail to compile for four targets, so the rule is effectively
  self-enforcing.

## Alternatives considered

**MockK for JVM tests only, with logic tested only on the JVM.** Rejected: it defeats
the purpose of multiplatform tests. Both bugs in
[KMP_PITFALLS.md](../KMP_PITFALLS.md) passed on the JVM.

**A multiplatform mocking library such as Mokkery.** Rejected for now: another
dependency and a KSP step, to replace fakes that are a few lines each. Worth revisiting
if fakes become a real burden.
