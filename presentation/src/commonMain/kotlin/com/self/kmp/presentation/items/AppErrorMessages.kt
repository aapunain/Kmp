package com.self.kmp.presentation.items

import com.self.kmp.domain.common.AppError

/**
 * Domain failure to human sentence. This mapping lives in the presentation layer
 * because wording is a UI concern; in a real app these would be localized string
 * resources rather than literals.
 */
internal fun AppError.toUiMessage(): String = when (this) {
    AppError.NoConnectivity -> "No connection. Check your network and try again."
    AppError.Unauthorized -> "Your session has expired. Please sign in again."
    AppError.NotFound -> "We couldn't find what you were looking for."
    AppError.ServerUnavailable -> "The service is temporarily unavailable."
    AppError.UnexpectedResponse -> "Something came back in an unexpected format."
    is AppError.Unknown -> message ?: "Something went wrong."
}
