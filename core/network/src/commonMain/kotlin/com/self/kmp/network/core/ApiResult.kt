package com.self.kmp.network.core

/**
 * Uniform return type for every remote call, so no data source ever throws at
 * the data layer and every call site is forced to handle failure.
 */
public sealed interface ApiResult<out T> {
    public data class Success<out T>(
        val data: T,
    ) : ApiResult<T>

    public data class Failure(
        val error: ApiError,
    ) : ApiResult<Nothing>
}
