package com.self.kmp

import com.self.kmp.contract.ApiRoutes
import com.self.kmp.contract.items.ItemsResponseDto
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the contract module is actually shared rather than merely declared: the
 * server serializes [ItemsResponseDto] and the test deserializes it back into the
 * very same class the mobile and web clients use.
 */
class ItemsRouteTest {

    @Test
    fun itemsRouteServesTheSharedContractType() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.get("/${ApiRoutes.ITEMS}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body: ItemsResponseDto = response.body()
        assertEquals(
            listOf(
                "Kotlin Multiplatform",
                "Compose Multiplatform",
                "Ktor Client",
                "kotlinx.serialization",
                "Koin",
                "Coroutines",
            ),
            body.items?.map { it.name },
        )
    }
}
