package com.self.kmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Same story as Kotlin/JS: a single event loop, so Default it is.
internal actual val platformIoDispatcher: CoroutineDispatcher = Dispatchers.Default
