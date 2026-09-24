package com.self.kmp.network.core

/**
 * Transport-level failure vocabulary. HTTP status codes live here and nowhere
 * above: :data translates these into `AppError` before anything else sees them.
 */
sealed interface ApiError {

    /** The request never reached the server. */
    data object NoInternet : ApiError

    /** The server did not answer in time. */
    data object Timeout : ApiError

    /** The server answered with a non-2xx status. */
    data class Http(val code: Int, val body: String? = null) : ApiError

    /** The response body did not match the DTO. */
    data class Serialization(val message: String? = null) : ApiError

    data class Unknown(val message: String? = null) : ApiError
}
