# Testing

## The constraint that shapes everything

**Mocking libraries do not work here.** MockK and Mockito are JVM-only. Code in
`commonTest` compiles for iOS, JS and Wasm, where bytecode generation and
reflection are unavailable. Adding MockK would break four of five targets.

So: **hand-written fakes**. This turns out to be a benefit rather than a tax — a
fake forces you to design a small interface, and it produces tests that assert
behaviour instead of asserting which methods were called.

```kotlin
private class FakeItemsRepository(
    private val result: AppResult<List<Item>>,
) : ItemsRepository {
    override suspend fun getItems(): AppResult<List<Item>> = result
}
```

## What to test in each layer

| Layer | What to assert | What you need |
| --- | --- | --- |
| `:core:domain` | Business rules: filtering, ordering, validation | Nothing. Plain `commonTest` |
| `:core:data` | DTO → domain mapping, `ApiError` → `AppError` translation | Fake data source + test dispatcher |
| `:core:network` | Method, path, parsing, status-code classification | Ktor `MockEngine` |
| `:core:database` | The store contract in `commonTest`; the real SQL in `jvmTest` | See below |
| `:presentation` | State transitions per intent, error → message mapping | Fake use case + `Dispatchers.setMain` |
| `:app:shared` | The DI graph resolves, deep links parse, the full chain works | The real Koin modules |

`:core:domain` tests needing zero infrastructure is the point of the layer, and the
clearest signal that the layering is real.

## Running tests

```bash
./gradlew verify                                  # everything, all five targets

./gradlew :core:domain:jvmTest                    # fastest feedback
./gradlew :core:domain:testAndroidHostTest        # Android (JVM-hosted)
./gradlew :core:domain:iosSimulatorArm64Test      # macOS only
./gradlew :core:domain:wasmJsTest                 # runs in headless Chrome
./gradlew :core:domain:jsTest                     # runs in headless Chrome
./gradlew :server:test
```

**A green `jvmTest` proves almost nothing.** Both bugs recorded in
[KMP_PITFALLS.md](KMP_PITFALLS.md) compiled and passed on the JVM. One of them only
failed on Apple targets; the other only surfaced from a test that fed the parser a
malformed payload.

## Source set names

Kotlin Multiplatform source sets, not the Android ones people expect. Note this
project uses AGP's KMP library plugin, so Android tests are `androidHostTest`
(JVM-hosted) and `androidDeviceTest` (instrumented) — not `androidUnitTest` /
`androidInstrumentedTest`.

```
src/commonTest/      runs on every target
src/jvmTest/         desktop only
src/iosTest/         both iOS targets
src/webTest/         shared by js and wasmJs
src/androidHostTest/ Android, on the JVM
```

Anything in `commonTest` executes five times over, once per target. That is
intentional: it is how platform-specific breakage gets caught.

Several detekt rules need these names listed explicitly, because detekt's defaults
only know `**/test/**`. See `config/detekt/detekt.yml`.

## Coroutines in tests

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsRepositoryImplTest {
    private class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
        override val main = dispatcher
        override val default = dispatcher
        override val io = dispatcher
    }
    // inject TestDispatcherProvider(UnconfinedTestDispatcher())
}
```

Injecting `DispatcherProvider` is what makes repository tests deterministic on every
target. If the repository referenced `Dispatchers.IO` directly there would be
nothing to substitute — and on JS and Wasm it would not even compile.

For ViewModels, `viewModelScope` is bound to the main dispatcher, so substitute it:

```kotlin
@BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
@AfterTest fun tearDown() = Dispatchers.resetMain()
```

`Dispatchers.setMain` is declared in common by `kotlinx-coroutines-test`, which is
why one ViewModel test works on all five targets.

**If the state is built with `stateIn(..., WhileSubscribed(...))`, that is not enough.**
Nothing upstream runs until something collects, so `state.value` returns the initial
value and the test passes against a completely broken view model. Subscribe first:

```kotlin
private val mainDispatcher = UnconfinedTestDispatcher()   // one scheduler, shared

@Test
fun x() = runTest(mainDispatcher) {                        // note: same dispatcher
    val viewModel = viewModel(repository)
    backgroundScope.launch { viewModel.state.collect { } }  // cancelled at test end
    // state.value is now real
}
```

`TodoViewModelTest` is the worked example. The full trap, including why the dispatcher
has to be shared, is [pitfall 15](KMP_PITFALLS.md).

## Database tests

Two layers, because the two targets that cannot run SQLite still run in production:

- **`commonTest` → `InMemoryTodoLocalStoreTest`.** Asserts the `TodoLocalStore`
  contract: ordering on read, unique ids, no-op writes to a stale id, an existing
  observer seeing later writes. This runs on all five targets, and on JS and Wasm it is
  testing the store those platforms genuinely use.
- **`jvmTest` → `RoomTodoLocalStoreTest`.** Builds a real in-memory SQLite database
  through `BundledSQLiteDriver` and exercises the generated DAO. This is the only test
  that can catch a wrong `ORDER BY`, a column name that does not match the entity,
  `autoGenerate` not generating, or a `Boolean` that does not survive SQLite's integer
  storage.

```kotlin
database = Room.inMemoryDatabaseBuilder<AppDatabase>()
    .setDriver(BundledSQLiteDriver())
    .build()
```

Do not pass `setQueryCoroutineContext(Dispatchers.IO)` — it fails `conventionsCheck`,
and production does not pass it either, so omitting it is also the more faithful test.

The two suites deliberately make the same assertions where they can. The schema and
the DAO are common code, so verifying them once on the JVM verifies them for Android
and iOS too.

## Network tests

`MockEngine` is published for every Kotlin target, so one test covers all of them:

```kotlin
val engine = MockEngine { request ->
    assertEquals(HttpMethod.Get, request.method)
    assertEquals("/${ApiRoutes.ITEMS}", request.url.encodedPath)
    respond(content = """{"items":[]}""", status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"))
}
```

Always include a **malformed payload** case. That is how the
`ContentConvertException` misclassification was found.

## Coverage

```bash
./gradlew koverLog       # aggregate, then one line per module
./gradlew koverVerify    # enforces the floor; part of ./gradlew verify
```

**Read the first line of `koverLog`, not the per-module ones.** The filters live in the
root `kover` block and apply to the aggregated report only — each module computes its
own number without them. So `:presentation` reports ~32% because every Composable
counts, and `:feature:auth` ~34% because the biometric adapters do. The aggregate, and
the gate, is 91%. Nothing is wrong; the per-module figures are just measuring something
different.

Measured on the JVM target only — that is the only place instrumentation works. For
code in `commonMain` it is representative, but it is not measuring the iOS or Wasm
runs.

The floor (85%) is a **ratchet against regression, not a target**. Raise it when real
coverage climbs. Do not write tests to move the number.

Excluded, each with a reason in the root `kover` block: Composables and DI modules,
generated code (Room's `*_Impl`, Compose's `ComposableSingletons`), and thin platform
adapters that cannot execute on a host JVM (the Room builders, the biometric prompts).

**"Untested" is never a reason to exclude something.** When the number drops, read the
report before reaching for a filter:

```bash
./gradlew koverXmlReport   # then read build/reports/kover/report.xml per class
```

## What not to do

- Do not add a mocking library.
- Do not test private functions. Test through the public surface.
- Do not assert on implementation details like call ordering unless ordering is the
  actual requirement.
- Do not put a test only in `jvmTest` when it belongs in `commonTest`. You lose the
  four other targets, which is where the interesting failures are. The one accepted
  reason is a dependency that has no variant for the other targets —
  `RoomTodoLocalStoreTest` and the bundled SQLite driver.
- Do not exclude code from coverage because it is untested. Generated, or unable to run
  on a host JVM, are the only accepted reasons, and each needs a written one.
