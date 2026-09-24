package com.self.kmp.concurrency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

/**
 * Runs on every target, which is the point: it proves each platform actually
 * supplied a usable `io` dispatcher. This is the test that would have caught
 * `Dispatchers.IO` being unavailable on Native and on the web.
 */
class DefaultDispatcherProviderTest {

    @Test
    fun ioDispatcherIsUsableOnThisPlatform() = runTest {
        val dispatchers = DefaultDispatcherProvider()

        val result = withContext(dispatchers.io) { "ran" }

        assertEquals("ran", result)
    }

    @Test
    fun defaultDispatcherIsUsableOnThisPlatform() = runTest {
        val dispatchers = DefaultDispatcherProvider()

        val result = withContext(dispatchers.default) { "ran" }

        assertEquals("ran", result)
    }
}
