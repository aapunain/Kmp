package com.self.kmp.domain.todo

import kotlinx.coroutines.flow.Flow

/**
 * The port the domain needs for todos. Implemented in `:core:data` over
 * `:core:database`, so nothing here knows a database exists.
 */
public interface TodoRepository {
    public fun observeTodos(order: TodoOrder): Flow<List<Todo>>

    public suspend fun addTodo(text: String)

    public suspend fun setDone(
        id: Long,
        isDone: Boolean,
    )

    public suspend fun deleteTodo(id: Long)
}
