package com.self.kmp.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal sealed interface NavigationIntent {
    /** Push [route]. Opening the screen that is already on top does nothing. */
    data class Open(
        val route: NavKey,
    ) : NavigationIntent

    /** Pop one screen. The root is never popped: Nav3 cannot render an empty stack. */
    data object Back : NavigationIntent
}

/**
 * Owns the back stack, so that it lives as long as the screen's `ViewModelStore`
 * rather than as long as one composition.
 *
 * On Android a configuration change (rotation, dark mode, locale, a foldable resizing)
 * destroys and recreates the Activity. A back stack held in `remember` was rebuilt from
 * scratch each time, which lost the user's place *and* replayed the launch deep link.
 * A ViewModel survives the recreation, and [deepLink] is only read when it is first
 * created, so a re-delivered launch intent is ignored.
 *
 * It does **not** survive process death; see ADR-0017 for why that was accepted.
 *
 * The stack is exposed as an immutable snapshot, not a `SnapshotStateList`, so this
 * class has no Compose dependency and follows the same state + intent shape as every
 * other ViewModel in the project.
 */
internal class NavigationViewModel(
    deepLink: String?,
) : ViewModel() {
    private val _backStack = MutableStateFlow(backStackFor(deepLink))
    val backStack: StateFlow<List<NavKey>> = _backStack.asStateFlow()

    fun onIntent(intent: NavigationIntent) {
        when (intent) {
            is NavigationIntent.Open -> {
                // Two identical keys on top of each other would both be composed during
                // the transition and share one saveable-state key, which Compose rejects.
                _backStack.update { stack -> if (stack.lastOrNull() == intent.route) stack else stack + intent.route }
            }

            NavigationIntent.Back -> {
                _backStack.update { stack -> if (stack.size > 1) stack.dropLast(1) else stack }
            }
        }
    }
}
