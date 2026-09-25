# ADR-0010: A `:contract` module holding the HTTP contract shared with the server

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

The wizard's `:core` module needed either deleting or a real purpose
([ADR-0003](0003-no-catch-all-modules.md)). Renaming it `:common` was proposed and
rejected — same category of unenforceable name.

Meanwhile the mock payload in `:core:network` and the `:server` response shape were
unrelated strings. A field rename on the server would break clients silently at
runtime. Since this project owns its backend, that is avoidable.

## Decision

`:contract`, with a charter narrow enough to enforce:

> `@Serializable` request/response models and endpoint path constants shared by the
> clients and the server. No logic, no platform code, no dependency beyond
> kotlinx-serialization.

Both `:core:network` and `:server` depend on it. The route path is a shared constant:

```kotlin
public object ApiRoutes {
    public const val ITEMS: String = "items"
}
```

The server registers `get("/${ApiRoutes.ITEMS}")` and responds with
`ItemsResponseDto`; the client requests `ApiRoutes.ITEMS` and parses the same class.
Rename a field in `ItemDto` and the server, the Android app and the iOS framework all
stop compiling together.

Charters shift accordingly: `:contract` owns the wire **shape**, `:core:network` owns
the wire **mechanics** (HTTP, JSON, `ApiResult`, data sources).

`sayHello` moved into `:server` — it is server-only sample logic and does not qualify
under the charter.

## Consequences

Easy: one source of truth for the wire format, checked by the compiler rather than by
a Postman collection. `ItemsRouteTest` proves it by deserializing the server's response
into the very class the clients use.

Hard: DTOs are now visible to anything depending on `:contract`, so they are less
contained than when they sat inside `:core:network`. `:core:network` must expose
`:contract` with `api` rather than `implementation`, because the DTO appears in
`ItemsRemoteDataSource`'s signature. And `:contract` needs
`api(kotlinx-serialization-json)` so consumers have `@Serializable` on their classpath.

**This only works because we own the backend.** Against a third-party API the module is
impossible and the DTOs belong in `:core:network`.

## Enforcement

- `./gradlew architectureCheck` pins `:core:network → :contract` and `:server → :contract`.
- `ItemsRouteTest` in `:server` deserializes into the shared type, so the contract is
  exercised rather than merely declared.

## Alternatives considered

**Delete `:core` entirely** and keep DTOs in `:core:network`. This was the
recommendation if the real backend were third-party. Not chosen, because the project
owns `:server`.

**Rename `:core` to `:common`.** Rejected: an unenforceable name, see ADR-0003.

**Generate clients from an OpenAPI spec.** Reasonable at larger scale; disproportionate
here, and it gives up compile-time coupling in favour of a generation step.
