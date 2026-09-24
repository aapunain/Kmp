package com.self.kmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatchers are handed to the layers that need them instead of being
 * referenced statically. That keeps :data free of platform knowledge and makes
 * repository tests deterministic, since a test can inject a test dispatcher.
 */
interface DispatcherProvider {

    /** Main/UI dispatcher. Declared in common by kotlinx.coroutines on every target. */
    val main: CoroutineDispatcher

    /** CPU-bound work. */
    val default: CoroutineDispatcher

    /** Blocking or I/O-bound work. See [platformIoDispatcher] for the catch. */
    val io: CoroutineDispatcher
}

/**
 * `Dispatchers.IO` is declared only for JVM and Native targets. It does not
 * exist for Kotlin/JS or Kotlin/Wasm, where there is a single event loop and
 * nothing to offload to. Referencing it from common code would break the web
 * builds, so the value is supplied per platform.
 */
internal expect val platformIoDispatcher: CoroutineDispatcher

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = platformIoDispatcher
}
