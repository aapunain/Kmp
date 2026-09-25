package com.self.kmp.network.items.datasource

import com.self.kmp.contract.ApiRoutes
import com.self.kmp.contract.items.ItemsResponseDto
import com.self.kmp.network.core.ApiError
import com.self.kmp.network.core.ApiResult
import com.self.kmp.network.core.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

/**
 * MockEngine lets the transport layer be tested on every target without a
 * server. These tests assert the contract: the right method, the right path, and
 * that a non-2xx status becomes an ApiError rather than an exception.
 */
class ItemsRemoteDataSourceTest {
    @Test
    fun getItemsCallsTheItemsEndpointAndParsesTheBody() = runTest {
        val engine =
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/${ApiRoutes.ITEMS}", request.url.encodedPath)
                respond(
                    content = """{"items":[{"id":"1","name":"Kotlin"},{"id":"2","name":"Ktor"}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        val result = KtorItemsRemoteDataSource(createHttpClient(engine)).getItems()

        assertIs<ApiResult.Success<ItemsResponseDto>>(result)
        assertEquals(listOf("Kotlin", "Ktor"), result.data.items?.map { it.name })
    }

    @Test
    fun unknownFieldsInTheResponseAreIgnored() = runTest {
        val engine =
            MockEngine {
                respond(
                    content = """{"items":[{"id":"1","name":"Kotlin","addedByBackendLater":true}],"page":1}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        val result = KtorItemsRemoteDataSource(createHttpClient(engine)).getItems()

        assertIs<ApiResult.Success<*>>(result)
    }

    @Test
    fun serverErrorIsReturnedAsApiErrorHttpRatherThanThrown() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }

        val result = KtorItemsRemoteDataSource(createHttpClient(engine)).getItems()

        val error = assertIs<ApiResult.Failure>(result).error
        assertEquals(500, assertIs<ApiError.Http>(error).code)
    }

    @Test
    fun aBodyThatDoesNotMatchTheDtoBecomesApiErrorSerialization() = runTest {
        val engine =
            MockEngine {
                respond(
                    content = """{"items":"this should have been an array"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        val result = KtorItemsRemoteDataSource(createHttpClient(engine)).getItems()

        assertIs<ApiError.Serialization>(assertIs<ApiResult.Failure>(result).error)
    }
}
