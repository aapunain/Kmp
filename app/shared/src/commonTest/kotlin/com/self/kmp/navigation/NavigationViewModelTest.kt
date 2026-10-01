package com.self.kmp.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The state transitions only. That the instance survives an Android configuration change
 * is a property of `ViewModelStore`, not of this class, and needs a device to observe.
 *
 * No `Dispatchers.setMain` is needed: the view model never launches a coroutine, it only
 * updates a `MutableStateFlow`, so every transition is visible synchronously.
 */
class NavigationViewModelTest {
    @Test
    fun startsFromTheDeepLinksBackStack() {
        assertEquals(listOf<NavKey>(TodoRoute), NavigationViewModel(deepLink = null).backStack.value)
        assertEquals(
            listOf<NavKey>(TodoRoute, SampleRoute),
            NavigationViewModel(deepLink = "kmp://sample").backStack.value,
        )
    }

    @Test
    fun openPushesOnTop() {
        val viewModel = NavigationViewModel(deepLink = null)

        viewModel.onIntent(NavigationIntent.Open(SampleRoute))

        assertEquals(listOf<NavKey>(TodoRoute, SampleRoute), viewModel.backStack.value)
    }

    @Test
    fun openingTheScreenAlreadyOnTopDoesNotStackADuplicate() {
        // A double tap on the FAB menu item would otherwise push the sample twice, and two
        // equal keys share one saveable-state key, which Compose rejects at runtime.
        val viewModel = NavigationViewModel(deepLink = null)

        viewModel.onIntent(NavigationIntent.Open(SampleRoute))
        viewModel.onIntent(NavigationIntent.Open(SampleRoute))

        assertEquals(listOf<NavKey>(TodoRoute, SampleRoute), viewModel.backStack.value)
    }

    @Test
    fun backPopsOneScreen() {
        val viewModel = NavigationViewModel(deepLink = "kmp://sample")

        viewModel.onIntent(NavigationIntent.Back)

        assertEquals(listOf<NavKey>(TodoRoute), viewModel.backStack.value)
    }

    @Test
    fun backAtTheRootIsIgnoredBecauseNavDisplayCannotRenderAnEmptyStack() {
        val viewModel = NavigationViewModel(deepLink = null)

        viewModel.onIntent(NavigationIntent.Back)
        viewModel.onIntent(NavigationIntent.Back)

        assertEquals(listOf<NavKey>(TodoRoute), viewModel.backStack.value)
    }

    @Test
    fun theLaunchDeepLinkIsNotReplayedAfterTheUserNavigatesAway() {
        // The rotation bug, at the level this class can see it: opened via kmp://sample,
        // user goes Back, and the stack stays where the user left it. Previously the
        // composition rebuilt the stack from the link on every Activity recreation.
        val viewModel = NavigationViewModel(deepLink = "kmp://sample")

        viewModel.onIntent(NavigationIntent.Back)

        assertEquals(listOf<NavKey>(TodoRoute), viewModel.backStack.value)
    }
}
