package com.self.kmp

import com.self.kmp.contract.ApiRoutes
import com.self.kmp.contract.items.ItemDto
import com.self.kmp.contract.items.ItemsResponseDto
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }

        // The route path and the response type both come from :contract, which
        // the clients also compile against. Rename a field in ItemDto and this
        // file stops compiling at the same moment the iOS app does.
        get("/${ApiRoutes.ITEMS}") {
            call.respond(
                ItemsResponseDto(
                    items = listOf(
                        ItemDto(id = "1", name = "Kotlin Multiplatform"),
                        ItemDto(id = "2", name = "Compose Multiplatform"),
                        ItemDto(id = "3", name = "Ktor Client"),
                        ItemDto(id = "4", name = "kotlinx.serialization"),
                        ItemDto(id = "5", name = "Koin"),
                        ItemDto(id = "6", name = "Coroutines"),
                    ),
                ),
            )
        }
    }
}
