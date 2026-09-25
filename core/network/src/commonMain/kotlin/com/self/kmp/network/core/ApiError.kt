package com.self.kmp.network.core

/**
 * Transport-level failure vocabulary. HTTP status codes live here and nowhere
 * above: :core:data translates these into `AppError` before anything else sees
 * them.
 */
public sealed interface ApiError {
    /** The request never reached the server. */
    public data object NoInternet : ApiError

    /** The server did not answer in time. */
    public data object Timeout : ApiError

    /** The server answered with a non-2xx status. */
    public data class Http(
        val code: Int,
        val body: String? = null,
    ) : ApiError

    /** The response body did not match the DTO. */
    public data class Serialization(
        val message: String? = null,
    ) : ApiError

    public data class Unknown(
        val message: String? = null,
    ) : ApiError
}
