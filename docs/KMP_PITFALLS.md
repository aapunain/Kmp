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

---

## 10. KSP is incompatible with the Gradle configuration cache

**2026-09.** Found the moment Room's KSP processor was added to `:core:database`.

```
Configuration cache state could not be cached: field 'processorClasspath' of task
':core:database:kspAndroid' of type 'com.google.devtools.ksp.gradle.KspAATask':
error writing value of type 'org.gradle.api.internal.file....'
```

This is a hard failure, not a warning — every build fails while the cache is on. It
is not fixable from this side; it is a KSP limitation.

**Resolution.** `org.gradle.configuration-cache=false` in `gradle.properties`, with the
reason recorded next to it. Builds are slower because every invocation re-runs
configuration. `org.gradle.caching=true` stays on and still does most of the work.

Re-test this whenever KSP is upgraded. It is the single biggest build-speed item we
are carrying, and it is the main practical cost of choosing Room over SQLDelight —
see [ADR-0016](adr/0016-room-local-persistence.md).

---

## 11. Room KMP needs a KSP configuration per target, and there is no `ksp(...)`

**2026-09.** Found by `:core:database:compileKotlinIosArm64` failing on a missing
`actual` for `AppDatabaseConstructor`.

In a multiplatform project the plain `ksp(libs.room.compiler)` configuration does not
exist. Each target has its own, and a target you forget simply gets no generated code
— which surfaces later as a confusing "no actual for expect" error rather than as a
missing-processor error.

```kotlin
dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64", "kspJs", "kspWasmJs")
        .forEach { add(it, libs.room.compiler) }
}
```

Two more Room KMP specifics:

- Room generates an `expect object ... : RoomDatabaseConstructor<T>`, so the module
  needs `freeCompilerArgs.add("-Xexpect-actual-classes")` or every build warns about
  generated code you cannot edit.
- `exportSchema = true` requires the `androidx.room3` Gradle plugin to supply
  `room.schemaLocation`. At 3.0.3 that plugin is only published as `3.1.0-alpha01`, so
  the schema export is off until migration tests need it.

Add a target to `kmp.library` and you must add its KSP configuration here too. Nothing
enforces that link.

---

## 12. `sqlite-bundled` has no JS or Wasm variant

**2026-09.** Found by `:core:database:compileKotlinJs` failing to resolve the driver.

`androidx.sqlite:sqlite-bundled` ships a compiled SQLite for Android, JVM and Apple
targets only. The web story is `androidx.sqlite:sqlite-web` with
`WebWorkerSQLiteDriver` — it does exist, but it needs a JS `Worker`, which means an npm
dependency on `@sqlite.org/sqlite-wasm`, a worker entry script, webpack wiring, and
COOP/COEP response headers before OPFS will work.

**Resolution.** The driver dependency is declared per source set (`androidMain`,
`jvmMain`, `iosMain`), and the seam sits at the *store* rather than at the Room builder:
`internal expect fun createTodoLocalStore()`. JS and Wasm return
`InMemoryTodoLocalStore`, which satisfies the same port, so no layer above
`:core:database` knows which one it got.

The lesson is general: when a library covers four of five targets, put the `expect` at
the smallest interface that lets one platform answer differently — not at the library's
own entry point.

---

## 13. Navigation 3 artifacts are split across two groups

**2026-09.** Found by `navigation3-ui` failing to resolve for non-Android targets.

- `androidx.navigation3:navigation3-runtime` — from Google's Maven, and genuinely
  multiplatform (common/js/jvm/native/wasm variants).
- `org.jetbrains.androidx.navigation3:navigation3-ui` — the JetBrains port. The Google
  artifact of the same name is Android-only.
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3` — likewise
  JetBrains, and versioned with lifecycle rather than with Nav3.

Guessing the group from the runtime artifact gives an unresolvable coordinate. This
split is the norm for Compose Multiplatform: check whether the JetBrains port exists
before assuming the Google coordinate is multiplatform.

---

## 14. `androidx.startup` providers use `android:authorities`, plural

**2026-09.** Found by a manifest merger failure in `:core:database`.

An `<provider>` element declares `android:authorities`, not `android:authority`. The
singular form is silently wrong in some tools and a merger error in others, and the
message does not name the attribute.

The authority must also be unique per module, or two library manifests collide:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup-database"
    android:exported="false"
    tools:node="merge">
```

This is how `:core:database` gets an Application `Context` for Room's file path
without asking the app to pass one in.

---

## 15. `stateIn(WhileSubscribed)` produces nothing until something collects

**2026-09.** Found while writing `TodoViewModelTest`.

A view model that exposes `flow.stateIn(viewModelScope, WhileSubscribed(5_000), initial)`
looks like a `StateFlow` you can read, but reading `state.value` with no active
collector returns the *initial* value forever. The upstream never runs. A test written
the obvious way passes on a view model that is completely broken, because the initial
value is usually the empty state.

**Resolution.** Subscribe first, then assert:

```kotlin
private val mainDispatcher = UnconfinedTestDispatcher()   // one scheduler for both

@Test
fun x() = runTest(mainDispatcher) {                       // shares the scheduler
    val viewModel = viewModel(repository)
    backgroundScope.launch { viewModel.state.collect { } } // cancelled for us
    // ... now state.value is real
}
```

Two details that are easy to get wrong:

- Pass the same dispatcher to `runTest` that `Dispatchers.setMain` received. A bare
  `UnconfinedTestDispatcher()` creates its own `TestCoroutineScheduler`, and then
  `advanceUntilIdle()` in the test body will not advance `viewModelScope`.
- Use `backgroundScope`, not `launch`, or `runTest` waits forever for a flow that
  never completes.

---

## 16. Generated code silently tanks the coverage gate

**2026-09.** Found by `koverVerify` dropping from 81.6% to 38.8% in one commit.

Room's KSP output (`AppDatabase_Impl`, `TodoDao_Impl` and their anonymous inner
classes) is large, and Kover counts it like anything else. Compose is the same problem
in a different shape: it lifts the lambdas inside a `@Composable` into a synthetic
`ComposableSingletons$...Kt` holder, and an `annotatedBy("...Composable")` filter does
**not** exclude it, because the holder itself carries no annotation.

**Resolution.** Explicit `classes(...)` excludes in the root `kover` block, each with a
written reason. The rule we follow: exclude code that is *generated* or that *cannot
execute on a host JVM*, and never exclude code merely because it is untested.

Confirm what is actually uncovered before adding an exclusion:

```
./gradlew koverXmlReport   # then read build/reports/kover/report.xml per class
```

In this case the report showed roughly a third of the misses were generated, and the
rest were genuinely untested — so the fix was four exclusions plus 47 real tests.

---

## 17. `remember` does not survive an Android configuration change

**2026-10.** Found in review: the Nav3 back stack was held in `remember`.

Rotation, dark mode, locale, font scale and foldable/split-screen resizes all destroy
and recreate the Activity, and with it the composition. Anything in `remember` is
rebuilt. On iOS, desktop and web the same events only re-layout, so the bug is
invisible everywhere except Android.

It was worse than "back to the start screen": the stack was rebuilt from
`intent.data`, so a launch deep link was **replayed** on every rotation, even after the
user had navigated away from it.

**Resolution.** State that must outlive a recreation belongs in a ViewModel (survives
configuration changes) or in saved state (`rememberSaveable`, `rememberNavBackStack`,
which also survive process death). The back stack now lives in `NavigationViewModel`;
see [ADR-0017](adr/0017-navigation-3.md).

Read "a plain `remember` is enough here" as a claim to verify by rotating the device.
