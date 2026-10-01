package com.self.kmp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The todo list. The app's start destination. */
@Serializable
public data object TodoRoute : NavKey

/** The original network-backed items list, kept as a sample. */
@Serializable
public data object SampleRoute : NavKey

/**
 * Deep links, one per screen.
 *
 * Both a custom scheme and an https host are accepted so the same parser serves an
 * Android intent filter, an iOS URL scheme, and a web URL.
 *
 *   kmp://todo            https://kmp.self.com/todo
 *   kmp://sample          https://kmp.self.com/sample
 */
public object DeepLinks {
    public const val SCHEME: String = "kmp"
    public const val HOST: String = "kmp.self.com"

    public const val PATH_TODO: String = "todo"
    public const val PATH_SAMPLE: String = "sample"
}

/**
 * Turns a deep link into a back stack.
 *
 * Plain string parsing rather than a URI type, because `android.net.Uri` does not exist
 * in common code and the shapes we accept are simple. Returns a stack rather than a
 * single key so that opening the sample directly still leaves the todo list underneath
 * for Back to return to.
 *
 * Unknown or absent links fall back to the todo list.
 */
public fun backStackFor(deepLink: String?): List<NavKey> {
    val path =
        deepLink
            ?.substringAfter("://", missingDelimiterValue = "")
            ?.substringBefore('?')
            ?.removePrefix(DeepLinks.HOST)
            ?.trim('/')
            ?.lowercase()

    return when (path) {
        DeepLinks.PATH_SAMPLE -> listOf(TodoRoute, SampleRoute)
        else -> listOf(TodoRoute)
    }
}
