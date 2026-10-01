package com.self.kmp.data.todo

import com.self.kmp.concurrency.DispatcherProvider
import com.self.kmp.database.TodoLocalStore
import com.self.kmp.database.TodoRecord
import com.self.kmp.database.TodoSort
import com.self.kmp.domain.todo.Todo
import com.self.kmp.domain.todo.TodoOrder
import com.self.kmp.domain.todo.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Translates between the store's `TodoRecord` and the domain's `Todo`.
 *
 * There is no network here and no `AppResult`: the local database is the source of
 * truth, and a query against it does not fail in ways the domain needs to model. If
 * todos ever sync to `:server`, that is when this gains an `AppResult` surface.
 */
internal class TodoRepositoryImpl(
    private val todoLocalStore: TodoLocalStore,
    private val dispatchers: DispatcherProvider,
) : TodoRepository {
    override fun observeTodos(order: TodoOrder): Flow<List<Todo>> = todoLocalStore
        .observe(order.toSort())
        .map { records -> records.map(TodoRecord::toDomain) }
        .flowOn(dispatchers.io)

    override suspend fun addTodo(text: String) = withContext(dispatchers.io) {
        todoLocalStore.add(text = text, createdAt = currentTimeMillis())
    }

    override suspend fun setDone(
        id: Long,
        isDone: Boolean,
    ) = withContext(dispatchers.io) {
        todoLocalStore.setDone(id = id, isDone = isDone)
    }

    override suspend fun deleteTodo(id: Long) = withContext(dispatchers.io) {
        todoLocalStore.delete(id)
    }
}

private fun TodoOrder.toSort(): TodoSort = when (this) {
    TodoOrder.NEWEST_FIRST -> TodoSort.NEWEST_FIRST
    TodoOrder.OLDEST_FIRST -> TodoSort.OLDEST_FIRST
}

private fun TodoRecord.toDomain(): Todo = Todo(
    id = id,
    text = text,
    isDone = isDone,
    createdAt = createdAt,
)
