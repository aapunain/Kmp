# ADR-0013: MockEngine is the default HTTP engine; the engine is a parameter

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

A real Ktor client needs a different engine per platform: OkHttp for Android, Darwin
for iOS, CIO or Java for JVM, Js for JS and Wasm. That is five artifacts plus an
`expect`/`actual` factory before a single request can be made, and Wasm engine support
was the least certain of the five.

The immediate goal was a working list on all five platforms.

## Decision

The engine is a **constructor parameter**, and the sample binds `MockEngine`:

```kotlin
internal fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(AppJson) }
    defaultRequest { url(BASE_URL); header(HttpHeaders.Accept, ContentType.Application.Json) }
}
```

`MockEngine` is published for every Kotlin target, so one implementation serves
Android, iOS, JVM, JS and Wasm with no per-platform wiring.

`expectSuccess = true` is deliberate: it turns non-2xx responses into exceptions so
`safeApiCall` can classify them in one place rather than every call site checking
status codes.

The mock payload intentionally contains two malformed records — one with a null `id`,
one with a blank `name` — to prove the DTO tolerates bad data and that the mappers drop
it before the domain sees it.

## Consequences

Easy: the app runs on all five platforms with zero engine configuration, and tests
need no server, ports or CORS. Swapping to real engines touches one file.

Hard: nothing exercises a real socket yet. The `:server` serves the same
`ItemsResponseDto` on the same shared path ([ADR-0010](0010-shared-http-contract.md)),
so the types are already aligned, but the client is not pointed at it.

To switch: add the five engine artifacts, add an `expect fun platformHttpClientEngine()`,
bind it in `networkModule()` instead of `createMockEngine()`, and set `BASE_URL` to
`http://10.0.2.2:8080/` for the Android emulator or `localhost:8080` elsewhere.

## Enforcement

Review only. The parameterised engine makes the alternative awkward enough to notice.

## Alternatives considered

**Real engines from the start.** Rejected for the initial milestone: five artifacts and
uncertain Wasm support, for no gain while the backend returns static data.

**A hand-rolled fake `HttpClient`.** Rejected: `MockEngine` exercises the real Ktor
pipeline including `ContentNegotiation`, which is where the `ContentConvertException`
bug was actually found.
