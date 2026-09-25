package com.self.kmp.data.common

import com.self.kmp.domain.common.AppError
import com.self.kmp.network.core.ApiError

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private val HTTP_SERVER_ERRORS = 500..599

/**
 * The only function in the codebase that can see both `ApiError` and `AppError`.
 *
 * This is what buys the guarantee that no HTTP status code exists above the data
 * layer: :presentation does not depend on :core:network, so `ApiError` is not even
 * on its compile classpath.
 */
internal fun ApiError.toAppError(): AppError = when (this) {
    ApiError.NoInternet -> {
        AppError.NoConnectivity
    }

    ApiError.Timeout -> {
        AppError.NoConnectivity
    }

    is ApiError.Serialization -> {
        AppError.UnexpectedResponse
    }

    is ApiError.Unknown -> {
        AppError.Unknown(message)
    }

    is ApiError.Http -> {
        when (code) {
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> AppError.Unauthorized
            HTTP_NOT_FOUND -> AppError.NotFound
            in HTTP_SERVER_ERRORS -> AppError.ServerUnavailable
            else -> AppError.Unknown("HTTP $code")
        }
    }
}
