package com.self.kmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// kotlinx.coroutines 1.11.0 declares Dispatchers.IO as `internal` on Native, so
// there is no public IO dispatcher to delegate to on Apple targets. Default is
// backed by a real multi-threaded pool here, which is the closest correct
// substitute. If genuinely blocking work is ever added on iOS, the answer is a
// dedicated `newFixedThreadPoolContext`, not this dispatcher.
internal actual val platformIoDispatcher: CoroutineDispatcher = Dispatchers.Default
