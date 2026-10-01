package com.self.kmp.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Deep link parsing is common code, so one test covers all five platforms. The
 * platform-specific part is only *delivery* — an Android intent, an iOS URL scheme, the
 * browser location.
 */
class DeepLinkTest {
    @Test
    fun customSchemeOpensTheRequestedScreen() {
        assertEquals(listOf<NavKey>(TodoRoute), backStackFor("kmp://todo"))
        assertEquals(listOf<NavKey>(TodoRoute, SampleRoute), backStackFor("kmp://sample"))
    }

    @Test
    fun httpsFormOpensTheSameScreens() {
        assertEquals(listOf<NavKey>(TodoRoute), backStackFor("https://kmp.self.com/todo"))
        assertEquals(
            listOf<NavKey>(TodoRoute, SampleRoute),
            backStackFor("https://kmp.self.com/sample"),
        )
    }

    @Test
    fun theSampleIsOpenedOnTopOfTheTodoListSoBackWorks() {
        // Not just the sample on its own: a deep link should still leave somewhere to
        // go back to.
        assertEquals(2, backStackFor("kmp://sample").size)
        assertEquals(TodoRoute, backStackFor("kmp://sample").first())
    }

    @Test
    fun unknownAndAbsentLinksFallBackToTheTodoList() {
        listOf(null, "", "kmp://nope", "https://kmp.self.com/", "garbage").forEach { link ->
            assertEquals(listOf<NavKey>(TodoRoute), backStackFor(link), "link=$link")
        }
    }

    @Test
    fun parsingIgnoresCaseTrailingSlashesAndQueryStrings() {
        listOf(
            "kmp://SAMPLE",
            "kmp://sample/",
            "kmp://sample?utm_source=test",
            "https://kmp.self.com/Sample/",
        ).forEach { link ->
            assertEquals(listOf<NavKey>(TodoRoute, SampleRoute), backStackFor(link), "link=$link")
        }
    }
}
