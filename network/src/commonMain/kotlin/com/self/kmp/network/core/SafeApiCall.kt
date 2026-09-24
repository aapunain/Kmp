package com.self.kmp.network.core

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.ContentConvertException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException

/**
 * The single place where Ktor exceptions are turned into [ApiError]. Every
 * remote data source funnels through here, which is what makes "all APIs return
 * the same shape" true rather than aspirational.
 */
internal suspend inline fun <T> safeApiCall(
    crossinline block: suspend () -> T,
): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (cancellation: CancellationException) {
    // Never swallow cancellation: doing so breaks structured concurrency and
    // leaves coroutines running after their scope is gone.
    throw cancellation
} catch (clientError: ClientRequestException) {
    ApiResult.Failure(
        ApiError.Http(
            code = clientError.response.status.value,
            body = runCatching { clientError.response.bodyAsText() }.getOrNull(),
        ),
    )
} catch (serverError: ServerResponseException) {
    ApiResult.Failure(
        ApiError.Http(
            code = serverError.response.status.value,
            body = runCatching { serverError.response.bodyAsText() }.getOrNull(),
        ),
    )
} catch (timeout: HttpRequestTimeoutException) {
    ApiResult.Failure(ApiError.Timeout)
} catch (conversion: ContentConvertException) {
    // Ktor's ContentNegotiation wraps deserialization failures in
    // JsonConvertException rather than rethrowing SerializationException, so
    // catching only the latter would silently misreport a malformed payload
    // as an unknown error.
    ApiResult.Failure(ApiError.Serialization(conversion.message))
} catch (noTransformation: NoTransformationFoundException) {
    // Response arrived without a content type ContentNegotiation can convert.
    ApiResult.Failure(ApiError.Serialization(noTransformation.message))
} catch (serialization: SerializationException) {
    ApiResult.Failure(ApiError.Serialization(serialization.message))
} catch (other: Exception) {
    // A real engine also raises platform IO exceptions here; those map to
    // ApiError.NoInternet once OkHttp/Darwin/CIO are wired in.
    ApiResult.Failure(ApiError.Unknown(other.message))
}
