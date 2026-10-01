package com.self.kmp.database.internal

import com.self.kmp.database.TodoLocalStore
import com.self.kmp.database.TodoRecord
import com.self.kmp.database.TodoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomTodoLocalStore(
    private val todoDao: TodoDao,
) : TodoLocalStore {
    override fun observe(sort: TodoSort): Flow<List<TodoRecord>> {
        val rows =
            when (sort) {
                TodoSort.NEWEST_FIRST -> todoDao.observeNewestFirst()
                TodoSort.OLDEST_FIRST -> todoDao.observeOldestFirst()
            }
        return rows.map { entities -> entities.map(TodoEntity::toRecord) }
    }

    override suspend fun add(
        text: String,
        createdAt: Long,
    ) {
        todoDao.insert(TodoEntity(text = text, isDone = false, createdAt = createdAt))
    }

    override suspend fun setDone(
        id: Long,
        isDone: Boolean,
    ) {
        todoDao.setDone(id = id, isDone = isDone)
    }

    override suspend fun delete(id: Long) {
        todoDao.delete(id)
    }
}

private fun TodoEntity.toRecord(): TodoRecord = TodoRecord(
    id = id,
    text = text,
    isDone = isDone,
    createdAt = createdAt,
)
