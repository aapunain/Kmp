package com.self.kmp.database.di

import com.self.kmp.database.TodoLocalStore
import com.self.kmp.database.internal.createTodoLocalStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The module's only public surface, alongside [TodoLocalStore] itself.
 *
 * A `single`: Room holds open file handles and is expensive to construct, so exactly one
 * per process.
 */
public fun databaseModule(): Module = module {
    single<TodoLocalStore> { createTodoLocalStore() }
}
