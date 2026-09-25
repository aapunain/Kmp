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
| `:presentation` | State transitions per intent, error → message mapping | Fake use case + `Dispatchers.setMain` |
| `:app:shared` | The DI graph resolves, and the full chain works end to end | The real Koin modules |

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
./gradlew koverLog      # per-module and aggregate line coverage
./gradlew koverVerify    # enforces the floor; part of ./gradlew verify
```

Measured on the JVM target only — that is the only place instrumentation works. For
code in `commonMain` it is representative, but it is not measuring the iOS or Wasm
runs. Composables and DI modules are excluded from the numbers.

The floor (75%) is a **ratchet against regression, not a target**. Raise it when
real coverage climbs. Do not write tests to move the number.

## What not to do

- Do not add a mocking library.
- Do not test private functions. Test through the public surface.
- Do not assert on implementation details like call ordering unless ordering is the
  actual requirement.
- Do not put a test only in `jvmTest` when it belongs in `commonTest`. You lose the
  four other targets, which is where the interesting failures are.
