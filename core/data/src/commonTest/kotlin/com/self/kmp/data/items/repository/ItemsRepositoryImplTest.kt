package com.self.kmp.data.items.repository

import com.self.kmp.concurrency.DispatcherProvider
import com.self.kmp.contract.items.ItemDto
import com.self.kmp.contract.items.ItemsResponseDto
import com.self.kmp.domain.common.AppError
import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.items.model.Item
import com.self.kmp.network.core.ApiError
import com.self.kmp.network.core.ApiResult
import com.self.kmp.network.items.datasource.ItemsRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * The data layer's two responsibilities, tested in isolation: DTO to domain
 * mapping, and ApiError to AppError translation.
 *
 * The injected [DispatcherProvider] is what makes this possible on every target.
 * If the repository referenced Dispatchers.IO directly there would be nothing to
 * substitute, and the test would not even compile for JS or Wasm.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsRepositoryImplTest {

    private class FakeItemsRemoteDataSource(
        private val result: ApiResult<ItemsResponseDto>,
    ) : ItemsRemoteDataSource {
        override suspend fun getItems(): ApiResult<ItemsResponseDto> = result
    }

    private class TestDispatcherProvider(
        dispatcher: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
    }

    private fun repository(result: ApiResult<ItemsResponseDto>) = ItemsRepositoryImpl(
        itemsRemoteDataSource = FakeItemsRemoteDataSource(result),
        dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher()),
    )

    @Test
    fun dtosAreMappedToDomainModelsAndUnusableRecordsAreDropped() = runTest {
        val response = ItemsResponseDto(
            items = listOf(
                ItemDto(id = "1", name = "Kotlin"),
                ItemDto(id = null, name = "no id, cannot be represented"),
                ItemDto(id = "  ", name = "blank id, also dropped"),
                ItemDto(id = "2", name = null),
            ),
        )

        val result = repository(ApiResult.Success(response)).getItems()

        assertIs<AppResult.Success<List<Item>>>(result)
        assertEquals(
            listOf(Item(id = "1", name = "Kotlin"), Item(id = "2", name = "")),
            result.data,
        )
    }

    @Test
    fun aMissingItemsArrayBecomesAnEmptyListRatherThanAFailure() = runTest {
        val result = repository(ApiResult.Success(ItemsResponseDto(items = null))).getItems()

        assertIs<AppResult.Success<List<Item>>>(result)
        assertEquals(emptyList(), result.data)
    }

    @Test
    fun httpStatusCodesAreTranslatedIntoDomainErrors() = runTest {
        val cases = mapOf(
            401 to AppError.Unauthorized,
            403 to AppError.Unauthorized,
            404 to AppError.NotFound,
            503 to AppError.ServerUnavailable,
        )

        cases.forEach { (code, expected) ->
            val result = repository(ApiResult.Failure(ApiError.Http(code))).getItems()
            assertEquals(expected, assertIs<AppResult.Failure>(result).error, "HTTP $code")
        }
    }

    @Test
    fun transportFailuresAreTranslatedIntoDomainErrors() = runTest {
        val cases = mapOf<ApiError, AppError>(
            ApiError.NoInternet to AppError.NoConnectivity,
            ApiError.Timeout to AppError.NoConnectivity,
            ApiError.Serialization("boom") to AppError.UnexpectedResponse,
        )

        cases.forEach { (apiError, expected) ->
            val result = repository(ApiResult.Failure(apiError)).getItems()
            assertEquals(expected, assertIs<AppResult.Failure>(result).error, apiError.toString())
        }
    }
}
