package com.self.kmp.database.internal

import com.self.kmp.database.TodoLocalStore

internal actual fun createTodoLocalStore(): TodoLocalStore = InMemoryTodoLocalStore()
