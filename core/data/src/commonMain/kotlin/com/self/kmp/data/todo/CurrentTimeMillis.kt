package com.self.kmp.data.todo

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * `System.currentTimeMillis()` is JVM-only and `Date.now()` is web-only, but
 * `kotlin.time.Clock` is in the common stdlib — so no `expect`/`actual` is needed and
 * `:core:data` stays free of platform source sets.
 */
@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
