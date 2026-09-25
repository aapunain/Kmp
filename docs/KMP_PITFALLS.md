# KMP pitfalls

Platform traps that have already cost time in this repository. **Append-only** — add
new entries at the bottom with the date and how it was found. Do not delete entries;
they are the reason the corresponding rule or workaround exists.

Read this before writing code that touches coroutines, Ktor, or the build.

---

## 1. `Dispatchers.IO` is not portable

**2026-09.** Found by `:core:concurrency:compileKotlinIosArm64` failing.

`Dispatchers.IO` exists only on JVM and Android. On Apple targets it is declared
`internal` in kotlinx-coroutines 1.11.0, so it does not even compile:

```
e: Cannot access 'val IO: CoroutineDispatcher': it is internal in 'kotlinx.coroutines.Dispatchers'.
```

On JS and Wasm it does not exist at all — there is a single event loop and nothing
to offload to.

**Resolution.** `:core:concurrency` declares `internal expect val platformIoDispatcher`
with five actuals: `Dispatchers.IO` for android/jvm, `Dispatchers.Default` for
ios/js/wasmJs. Everything else injects `DispatcherProvider`.

Enforced by `./gradlew conventionsCheck`, which fails on any `Dispatchers.IO`
reference outside `core/concurrency/src/`. `DefaultDispatcherProviderTest` runs on
every target so a future regression is caught at runtime too.

If you genuinely need blocking work on iOS, the answer is a dedicated
`newFixedThreadPoolContext`, not `Dispatchers.Default`.

---

## 2. Ktor does not throw `SerializationException` for bad JSON

**2026-09.** Found by a test that fed the parser `{"items":"not an array"}`.

`ContentNegotiation` wraps deserialization failures in `JsonConvertException`, a
subtype of `io.ktor.serialization.ContentConvertException`. It is **not** a
`kotlinx.serialization.SerializationException`. Catching only the latter silently
classified every malformed response as `ApiError.Unknown` instead of
`ApiError.Serialization`, which downstream became `AppError.Unknown` rather than
`AppError.UnexpectedResponse`.

**Resolution.** `safeApiCall` catches `ContentConvertException` and
`NoTransformationFoundException` as well. Always include a malformed-payload case in
data source tests; this bug is invisible to a happy-path test.

---

## 3. Compose blocks web tests without an executable binary

**2026-09.** Found by `:presentation:wasmJsTest` failing configuration.

```
Compose UI tests for the 'wasmJs' target are not bundled with webpack: no
executable binary is declared, so the Skiko runtime required by Compose UI
cannot be loaded.
```

**Resolution.** `kmp.library.compose` declares `binaries.executable()` for both `js`
and `wasmJs`. Costs some build time, keeps the door open for Compose UI tests on web.

---

## 4. The `libs` accessor does not exist in precompiled script plugins

**2026-09.** Hit while writing `build-logic`.

Inside `build-logic/src/main/kotlin/*.gradle.kts` the generated type-safe `libs`
accessor is unavailable. Use the catalog extension instead:

```kotlin
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
libs.findLibrary("kotlin-test").get()
```

A convention plugin can only `apply` a plugin whose jar is on `build-logic`'s own
compile classpath — hence the `gradlePlugin-*` entries in the version catalog.

---

## 5. Android namespaces must be unique across every module

**2026-09.** Caught before it broke, while moving modules under `core/`.

Deriving the namespace from `project.name` produces a collision the moment two
modules share a leaf name — `:core:domain` and a future `:feature:items:domain` would
both want `com.self.kmp.domain`. Duplicate Android namespaces are a hard build failure.

**Resolution.** `kmp.library` derives it from the full path:

```kotlin
namespace = "com.self.kmp" + project.path.replace(":", ".")
```

---

## 6. macOS filesystems are case-insensitive

**2026-09.** Hit while renaming `main.kt` to `Main.kt` for ktlint.

A case-only rename looks like "destination already exists" to most tools. Use
`git mv`, which handles it correctly and records the rename.

---

## 7. ktlint rejects KDoc in `.gradle.kts` script bodies

**2026-09.** Found by the first `spotlessApply`.

A `/** ... */` block floating between `plugins {}` and `kotlin {}` is not attached to
a declaration, so ktlint reports `A KDoc is not allowed inside 'block'`. Use `//`
comments in build files.

---

## 8. detekt 2.x changed its config schema, and only 2.x supports Kotlin 2.4

**2026-09.** Found when the first detekt config was rejected outright.

The stable line (`io.gitlab.arturbosch.detekt` 1.23.8) is built against Kotlin 1.9.
Kotlin 2.4 support lives in `dev.detekt:2.0.0-alpha.6` — note the **new group
coordinates**. Its config keys differ from the ubiquitous 1.23 examples:

- `build:` / `maxIssues` — gone. Findings fail via the Gradle extension's
  `ignoreFailures = false`.
- `formatting:` ruleset — gone. ktlint runs through Spotless instead.
- `LongParameterList`: `allowedFunctionParameters`, not `functionThreshold`.
- `TooManyFunctions`: `allowedFunctionsPerClass`, not `thresholdInClasses`.
- `UnusedPrivateMember` split into `UnusedPrivateProperty` / `UnusedPrivateFunction`.

It is an **alpha**. If it starts misbehaving, prefer removing it from `qualityCheck`
and recording an ADR over living with a flaky gate.

Also: detekt's default excludes only know `**/test/**`, not KMP source set names, so
`commonTest`, `iosTest`, `webTest` and friends must be listed explicitly for rules
like `MagicNumber`.

---

## 9. detekt analyses generated code unless you scope it to `src`

**2026-09.** Found by findings in `build/generated/compose/resourceGenerator/`.

Pointing detekt at module directories picks up generated Compose resource accessors,
which produce findings nobody can act on. Scope it with
`include("**/src/**/*.kt")` and `exclude("**/build/**")`.
