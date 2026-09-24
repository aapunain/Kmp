package com.self.kmp.data.common

import com.self.kmp.domain.common.AppError
import com.self.kmp.network.core.ApiError

/**
 * The only function in the codebase that can see both `ApiError` and `AppError`.
 *
 * This is what buys the guarantee that no HTTP status code exists above the data
 * layer: :presentation does not depend on :network, so `ApiError` is not even on
 * its compile classpath.
 */
internal fun ApiError.toAppError(): AppError = when (this) {
    ApiError.NoInternet -> AppError.NoConnectivity
    ApiError.Timeout -> AppError.NoConnectivity
    is ApiError.Serialization -> AppError.UnexpectedResponse
    is ApiError.Unknown -> AppError.Unknown(message)
    is ApiError.Http -> when (code) {
        401, 403 -> AppError.Unauthorized
        404 -> AppError.NotFound
        in 500..599 -> AppError.ServerUnavailable
        else -> AppError.Unknown("HTTP $code")
    }
}
