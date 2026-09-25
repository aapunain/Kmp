package com.self.kmp.presentation.items

/**
 * Every way the user can affect this screen, as one closed set of types.
 *
 * The ViewModel exposes a single `onIntent` entry point instead of one public
 * method per action, so adding a behaviour is a compile error until the
 * `when` in the ViewModel handles it.
 */
public sealed interface ItemsIntent {
    public data object Load : ItemsIntent

    public data object Retry : ItemsIntent
}
