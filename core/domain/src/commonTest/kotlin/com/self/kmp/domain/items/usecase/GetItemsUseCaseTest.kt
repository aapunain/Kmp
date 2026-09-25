package com.self.kmp.domain.items.usecase

import com.self.kmp.domain.common.AppError
import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.items.model.Item
import com.self.kmp.domain.items.repository.ItemsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/**
 * The payoff of a dependency-free domain module: these tests need no HTTP, no
 * Compose, no device, and no mocking library. MockK and Mockito are JVM only, so
 * a hand-written fake is the portable answer in KMP anyway.
 */
class GetItemsUseCaseTest {
    private class FakeItemsRepository(
        private val result: AppResult<List<Item>>,
    ) : ItemsRepository {
        override suspend fun getItems(): AppResult<List<Item>> = result
    }

    @Test
    fun blankNamesAreDroppedAndTheRestIsSortedCaseInsensitively() = runTest {
        val repository =
            FakeItemsRepository(
                AppResult.Success(
                    listOf(
                        Item(id = "1", name = "Zebra"),
                        Item(id = "2", name = "   "),
                        Item(id = "3", name = "apple"),
                        Item(id = "4", name = "Mango"),
                    ),
                ),
            )

        val result = GetItemsUseCase(repository).invoke()

        val names = (result as AppResult.Success).data.map(Item::name)
        assertEquals(listOf("apple", "Mango", "Zebra"), names)
    }

    @Test
    fun failuresArePropagatedUnchanged() = runTest {
        val repository = FakeItemsRepository(AppResult.Failure(AppError.NoConnectivity))

        val result = GetItemsUseCase(repository).invoke()

        assertEquals(AppResult.Failure(AppError.NoConnectivity), result)
    }
}
