# ADR-0006: Remote data sources return `ApiResult` rather than throwing

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

Two options for the network layer's error surface. Either data sources throw typed
exceptions and the repository catches them, or every data source function returns
`ApiResult<Dto>` so failure is part of the signature.

The recommendation at design time was throwing, on the grounds that it keeps the
signature contract-shaped: `suspend fun getItems(): ItemsResponseDto` reads exactly
like the endpoint. The project owner chose returning `ApiResult`, for a uniform
result shape across every API call.

## Decision

Every remote data source function returns `ApiResult<T>`:

```kotlin
public interface ItemsRemoteDataSource {
    public suspend fun getItems(): ApiResult<ItemsResponseDto>
}
```

Ktor exceptions are classified in exactly one place, `safeApiCall`, which every data
source funnels through. That is what makes "all APIs return the same shape" true
rather than aspirational, and it keeps each data source implementation a one-liner.

`safeApiCall` must rethrow `CancellationException` before its generic catch.
Swallowing it breaks structured concurrency and leaves coroutines running after their
scope is gone.

## Consequences

Easy: no call site can forget to handle failure, because failure is in the type. One
place to change when a new transport failure mode appears. Tests can construct
failures without throwing.

Hard: the signature is slightly noisier than the bare endpoint shape. `ApiResult` is
public API of `:core:network`, which `:core:data` must unwrap before mapping.

A bug this design surfaced: catching only `SerializationException` misclassified every
malformed response, because Ktor wraps those in `ContentConvertException`. Documented
in [KMP_PITFALLS.md](../KMP_PITFALLS.md#2-ktor-does-not-throw-serializationexception-for-bad-json).

## Enforcement

`ItemsRemoteDataSourceTest` asserts the classification for a 500, a malformed body and
an unknown-field payload. Review only for new data sources, though the `ApiResult`
return type makes the pattern hard to miss.

## Alternatives considered

**Throw typed exceptions, catch in the repository.** Recommended but not chosen.
Would have kept signatures marginally cleaner at the cost of a non-uniform contract.

**Return `kotlin.Result`.** Rejected: it carries a `Throwable`, which reintroduces
untyped failure and does not model transport cases.
