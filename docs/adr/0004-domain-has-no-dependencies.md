# ADR-0004: `:core:domain` depends on nothing

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

The domain layer holds business policy. Every dependency it acquires is a constraint
on where that policy can run and a thing that can break it. The first version
depended on `koin-core`, solely so the module could publish its own
`domainModule()` DI wiring.

A KMP subtlety matters here: "pure Kotlin" has two meanings. A module using the
`kotlin("jvm")` plugin cannot be consumed by iOS, JS or Wasm at all. `:core:domain`
is a multiplatform module whose code happens to live entirely in `commonMain` — the
target list is still declared, there is just no platform-specific source set.

## Decision

`:core:domain` has an empty `commonMain.dependencies` block. Verified:

```
> ./gradlew :core:domain:dependencies --configuration jvmCompileClasspath

jvmCompileClasspath - Compile classpath for 'jvm/main'.
\--- org.jetbrains.kotlin:kotlin-stdlib:2.4.20
     \--- org.jetbrains:annotations:13.0
```

Specifically excluded:

- **Koin.** `domainModule()` moved to `:app:shared/di/DomainModule.kt`. This is
  possible because use cases are public API, so the composition root can construct
  them. Modules whose implementations are `internal` must still self-wire; see
  [ADR-0009](0009-koin-and-wiring-ownership.md).
- **kotlinx-coroutines.** `suspend` is a language feature, not a library one. Add
  coroutines only when `Flow` appears in a repository signature, and add it as `api`.

Infrastructure interfaces do not belong here either. `DispatcherProvider` lives in
`:core:concurrency` because dispatchers are a technical concern the business logic
knows nothing about. Only genuine *domain capabilities* — biometrics, location,
secure storage — get an interface in this module.

Test: would a backend engineer reading `:core:domain` recognise every type as a
business concept? If not, it is in the wrong module.

## Consequences

Easy: domain tests need no infrastructure at all — no HTTP, no Compose, no device, no
test framework beyond `kotlin-test`. This is the clearest signal the layering is real
rather than decorative. The layer also compiles in parallel with everything else.

Hard: anything the domain needs must be injected as an interface it declares, which
is more ceremony than calling a library directly. `domainModule()` living in
`:app:shared` is mildly surprising, so its KDoc explains why it is not where you
would look for it.

## Enforcement

- `./gradlew architectureCheck` fails if `:core:domain` declares any project dependency.
- A library dependency would be caught in review; the empty dependencies block plus
  this ADR make the intent explicit.

## Alternatives considered

**Keep `koin-core` for self-wiring consistency.** Rejected by the project owner in
favour of a genuinely dependency-free domain. Consistency lost, purity gained.

**Put `domainModule()` in `:core:data`.** Rejected: the data layer should not own the
domain's object graph.

**A separate `:core:domain-di` module.** Rejected as ceremony — it would double the
module count to relocate one function.
