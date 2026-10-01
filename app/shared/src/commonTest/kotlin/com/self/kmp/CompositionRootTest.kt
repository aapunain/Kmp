package com.self.kmp

import com.self.kmp.concurrency.di.concurrencyModule
import com.self.kmp.data.di.dataModule
import com.self.kmp.di.domainModule
import com.self.kmp.di.navigationModule
import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.items.model.Item
import com.self.kmp.domain.items.repository.ItemsRepository
import com.self.kmp.domain.items.usecase.GetItemsUseCase
import com.self.kmp.navigation.NavigationViewModel
import com.self.kmp.navigation.SampleRoute
import com.self.kmp.navigation.TodoRoute
import com.self.kmp.network.di.networkModule
import com.self.kmp.presentation.di.presentationModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication

/**
 * Koin resolves at runtime, so a compiling build says nothing about whether the
 * graph is actually wired. This test starts the real composition root and pulls a
 * use case through it.
 *
 * It is also the only end-to-end test in the project: MockEngine -> Ktor ->
 * ItemsRemoteDataSource -> ItemsRepositoryImpl -> GetItemsUseCase, with every
 * boundary crossing a real mapper.
 */
class CompositionRootTest {
    private fun koin() = koinApplication {
        modules(
            concurrencyModule(),
            networkModule(),
            dataModule(),
            domainModule(),
            presentationModule(),
            navigationModule(),
        )
    }.koin

    @Test
    fun theNavigationViewModelReceivesTheDeepLinkThroughKoin() {
        // The parameter crosses Koin untyped, so a wrong getOrNull<T>() would compile and
        // silently start every launch on the default stack.
        val koin = koin()

        val linked = koin.get<NavigationViewModel> { parametersOf("kmp://sample") }
        val plain = koin.get<NavigationViewModel> { parametersOf(null) }

        assertEquals(listOf(TodoRoute, SampleRoute), linked.backStack.value)
        assertEquals(listOf(TodoRoute), plain.backStack.value)
    }

    @Test
    fun everyDependencyInTheGraphCanBeResolved() {
        val koin = koin()

        // The domain interface resolves to the data implementation, which :app:shared
        // cannot even name because it is internal to :data.
        assertIs<ItemsRepository>(koin.get<ItemsRepository>())
        assertIs<GetItemsUseCase>(koin.get<GetItemsUseCase>())
    }

    @Test
    fun theFullChainDeliversTheMockedPayloadAsDomainModels() = runTest {
        val getItems = koin().get<GetItemsUseCase>()

        val result = getItems()

        assertIs<AppResult.Success<List<Item>>>(result)
        assertEquals(
            listOf(
                "Compose Multiplatform",
                "Coroutines",
                "Koin",
                "Kotlin Multiplatform",
                "kotlinx.serialization",
                "Ktor Client",
            ),
            result.data.map(Item::name),
            "Two of the eight mocked records are unusable and must have been " +
                "dropped: one has a null id (mapper) and one has a blank name (use case).",
        )
    }
}
