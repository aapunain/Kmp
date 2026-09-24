package com.self.kmp.presentation.items

import com.self.kmp.domain.common.AppError
import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.items.model.Item
import com.self.kmp.domain.items.repository.ItemsRepository
import com.self.kmp.domain.items.usecase.GetItemsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * `viewModelScope` is bound to the main dispatcher, so the test substitutes it.
 * `Dispatchers.setMain` is declared in common by kotlinx-coroutines-test, which
 * is why one test works for every target.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModelTest {

    private class FakeItemsRepository(
        private val result: AppResult<List<Item>>,
    ) : ItemsRepository {
        override suspend fun getItems(): AppResult<List<Item>> = result
    }

    private fun viewModel(result: AppResult<List<Item>>) =
        ItemsViewModel(GetItemsUseCase(FakeItemsRepository(result)))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun itemsAreLoadedOnInitAndExposedAsDisplayStrings() = runTest {
        val viewModel = viewModel(
            AppResult.Success(
                listOf(Item(id = "1", name = "Kotlin"), Item(id = "2", name = "Ktor")),
            ),
        )

        val state = viewModel.state.value
        assertEquals(listOf("Kotlin", "Ktor"), state.items)
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun aDomainErrorBecomesAUserFacingMessageAndNotAnException() = runTest {
        val viewModel = viewModel(AppResult.Failure(AppError.NoConnectivity))

        val state = viewModel.state.value
        assertTrue(state.items.isEmpty())
        assertEquals(AppError.NoConnectivity.toUiMessage(), state.errorMessage)
    }

    @Test
    fun retryClearsThePreviousErrorAndReloads() = runTest {
        val viewModel = viewModel(AppResult.Success(listOf(Item(id = "1", name = "Kotlin"))))

        viewModel.onIntent(ItemsIntent.Retry)

        val state = viewModel.state.value
        assertEquals(listOf("Kotlin"), state.items)
        assertNull(state.errorMessage)
    }
}
