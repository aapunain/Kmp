package com.self.kmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/JS is single threaded: there is no thread pool to move blocking work
// to, so Default is the only honest answer here.
internal actual val platformIoDispatcher: CoroutineDispatcher = Dispatchers.Default
