package com.self.kmp.network.items.datasource

import com.self.kmp.network.core.ApiResult
import com.self.kmp.network.core.safeApiCall
import com.self.kmp.network.items.dto.ItemsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * The contract with the backend, expressed as Kotlin.
 *
 * Each function signature says what the endpoint takes and what it returns.
 * There is no domain type here and no Ktor type in the signature, so :data can
 * fake this interface in tests without any HTTP machinery.
 */
interface ItemsRemoteDataSource {

    /** GET /items */
    suspend fun getItems(): ApiResult<ItemsResponseDto>
}

internal class KtorItemsRemoteDataSource(
    private val httpClient: HttpClient,
) : ItemsRemoteDataSource {

    override suspend fun getItems(): ApiResult<ItemsResponseDto> = safeApiCall {
        httpClient.get("items").body()
    }
}
