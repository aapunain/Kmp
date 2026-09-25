package com.self.kmp.network.core

import com.self.kmp.contract.ApiRoutes
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

private val ITEMS_RESPONSE_JSON =
    """
    {
      "items": [
        { "id": "1", "name": "Kotlin Multiplatform" },
        { "id": "2", "name": "Compose Multiplatform" },
        { "id": "3", "name": "Ktor Client" },
        { "id": "4", "name": "kotlinx.serialization" },
        { "id": "5", "name": "Koin" },
        { "id": "6", "name": "Coroutines" },
        { "id": "7", "name": "" },
        { "id": null, "name": "dropped because it has no id" }
      ]
    }
    """.trimIndent()

/**
 * Stand-in for the real backend.
 *
 * MockEngine is published for every Kotlin target, so this one implementation
 * serves Android, iOS, JVM, JS and Wasm identically. The last two entries are
 * intentionally malformed in ways a well-behaved server would not produce: they
 * prove the DTO tolerates bad data and that the mappers drop it before the
 * domain sees it. The rendered list is the same six items either way.
 */
internal fun createMockEngine(): HttpClientEngine = MockEngine { request ->
    when (request.url.encodedPath) {
        "/${ApiRoutes.ITEMS}" -> {
            respond(
                content = ITEMS_RESPONSE_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        else -> {
            respondError(HttpStatusCode.NotFound)
        }
    }
}
