package com.self.kmp.network.core

/**
 * Uniform return type for every remote call, so no data source ever throws at
 * the data layer and every call site is forced to handle failure.
 */
sealed interface ApiResult<out T> {

    data class Success<out T>(val data: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}
