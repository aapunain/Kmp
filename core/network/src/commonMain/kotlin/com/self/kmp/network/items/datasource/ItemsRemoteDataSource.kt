package com.self.kmp.network.items.datasource

import com.self.kmp.contract.ApiRoutes
import com.self.kmp.contract.items.ItemsResponseDto
import com.self.kmp.network.core.ApiResult
import com.self.kmp.network.core.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * The contract with the backend, expressed as Kotlin.
 *
 * Both the path and the response type come from :contract, which the server also
 * depends on. Renaming a field or moving an endpoint is therefore a compile
 * error on both sides rather than a 4xx discovered in QA.
 */
public interface ItemsRemoteDataSource {
    /** GET /items */
    public suspend fun getItems(): ApiResult<ItemsResponseDto>
}

internal class KtorItemsRemoteDataSource(
    private val httpClient: HttpClient,
) : ItemsRemoteDataSource {
    override suspend fun getItems(): ApiResult<ItemsResponseDto> = safeApiCall {
        httpClient.get(ApiRoutes.ITEMS).body()
    }
}
