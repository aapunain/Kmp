package com.self.kmp

/**
 * Moved here from the retired :core module. It is server-only sample logic, and
 * it does not belong in :contract, whose charter covers wire models and endpoint
 * paths only.
 */
fun sayHello(to: String): String = "Hello, $to!"
