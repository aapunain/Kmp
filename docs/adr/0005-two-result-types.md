# ADR-0005: Separate transport and domain result types, with one translator

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

Ktor throws `ClientRequestException`, `ServerResponseException`,
`HttpRequestTimeoutException` and `ContentConvertException`. None of these may escape
the data layer — a ViewModel catching a Ktor exception means the layering has failed.

The question is how many result types to have. One shared type is less code. The
first draft put `AppResult`/`AppError` in a `:core` module visible to everyone,
with `AppError.Server(code: Int)` carrying the HTTP status.

## Decision

Two result types, in different modules:

```
:core:network    ApiResult<T> / ApiError    HTTP status codes live here
      │  translated by :core:data, the only module that sees both
      ▼
:core:domain     AppResult<T> / AppError    no transport vocabulary at all
```

`AppError` is expressed in domain language — `NoConnectivity`, `Unauthorized`,
`NotFound`, `ServerUnavailable`, `UnexpectedResponse`, `Unknown`. No status codes, no
exceptions, no Ktor types. The earlier `Server(code: Int)` case was removed once
`AppError` became a domain citizen: a status code is transport vocabulary.

`:core:data` owns the translation, in `ApiErrorMapper.toAppError()`. Because
`:presentation` does not depend on `:core:network`, `ApiError` is not on its compile
classpath at all.

## Consequences

Easy: "no HTTP status code above the data layer" is a compile-time guarantee rather
than a review convention. Each layer's failure vocabulary reads in its own language.

Hard: two types, two mapping steps, and a second file to touch when adding a failure
mode. That second type is precisely what buys the guarantee — a single shared type
would put status codes on the UI's classpath.

## Enforcement

- `./gradlew architectureCheck` keeps `:presentation` from depending on `:core:network`,
  which makes the leak impossible rather than merely discouraged.
- `ItemsRepositoryImplTest` asserts the status-code-to-`AppError` mapping directly.

## Alternatives considered

**One shared result type in a lower module.** Rejected: it puts transport concepts on
every consumer's classpath and removes the compiler's ability to stop the leak.

**Repositories throw domain exceptions.** Rejected: a returned `AppResult` forces the
caller to handle failure at the type level. An exception can be ignored silently.

**Arrow `Either`.** Rejected as an additional dependency for a project this size; a
sealed interface is sufficient and has no external cost.
