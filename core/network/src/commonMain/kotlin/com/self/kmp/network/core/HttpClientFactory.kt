package com.self.kmp.network.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val BASE_URL: String = "https://api.example.com/"

internal val AppJson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

/**
 * The engine is a parameter rather than a hardcoded choice. That is the seam
 * that lets this sample run on MockEngine while a real build swaps in
 * OkHttp/Darwin/CIO/Js without touching any other file.
 */
internal fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    // Turns non-2xx responses into exceptions so safeApiCall can classify them
    // in one place instead of every call site checking status codes.
    expectSuccess = true

    install(ContentNegotiation) {
        json(AppJson)
    }

    defaultRequest {
        url(BASE_URL)
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }
}
