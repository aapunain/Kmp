package com.self.kmp.domain.common

/**
 * The only failure vocabulary the application layers above [com.self.kmp.domain]
 * are allowed to see.
 *
 * Note what is absent: there is no HTTP status code, no exception, no Ktor type.
 * Translating transport failures into these cases is the job of the data layer,
 * which is the single place where `ApiError` and `AppError` are both visible.
 */
public sealed interface AppError {
    /** The request could not reach the backend at all, or it timed out. */
    public data object NoConnectivity : AppError

    /** The caller is not authenticated, or the session is no longer valid. */
    public data object Unauthorized : AppError

    /** The requested resource does not exist. */
    public data object NotFound : AppError

    /** The backend is reachable but currently unable to serve the request. */
    public data object ServerUnavailable : AppError

    /** The backend answered, but not in a shape this app understands. */
    public data object UnexpectedResponse : AppError

    /** Anything we have not modelled explicitly. */
    public data class Unknown(
        val message: String? = null,
    ) : AppError
}
