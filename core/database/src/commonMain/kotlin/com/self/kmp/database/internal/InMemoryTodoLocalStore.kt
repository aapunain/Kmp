package com.self.kmp.database.internal

import com.self.kmp.database.TodoLocalStore
import com.self.kmp.database.TodoRecord
import com.self.kmp.database.TodoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Used on JS and Wasm, where a real SQLite driver needs a Worker and OPFS headers.
 *
 * Behaviourally identical to the Room store apart from durability: todos live until the
 * page is reloaded. Reactive in the same way, so the UI code path is unchanged.
 */
internal class InMemoryTodoLocalStore : TodoLocalStore {
    private val rows = MutableStateFlow<List<TodoRecord>>(emptyList())
    private var nextId = 1L

    override fun observe(sort: TodoSort): Flow<List<TodoRecord>> = rows.asStateFlow().map { current ->
        when (sort) {
            TodoSort.NEWEST_FIRST -> current.sortedByDescending { it.createdAt }
            TodoSort.OLDEST_FIRST -> current.sortedBy { it.createdAt }
        }
    }

    override suspend fun add(
        text: String,
        createdAt: Long,
    ) {
        rows.update { current ->
            current + TodoRecord(id = nextId++, text = text, isDone = false, createdAt = createdAt)
        }
    }

    override suspend fun setDone(
        id: Long,
        isDone: Boolean,
    ) {
        rows.update { current ->
            current.map { row -> if (row.id == id) row.copy(isDone = isDone) else row }
        }
    }

    override suspend fun delete(id: Long) {
        rows.update { current -> current.filterNot { it.id == id } }
    }
}
