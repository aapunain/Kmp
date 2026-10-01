package com.self.kmp.database

import kotlinx.coroutines.flow.Flow

/** A row of the `todo` table, free of any Room type. */
public data class TodoRecord(
    val id: Long,
    val text: String,
    val isDone: Boolean,
    val createdAt: Long,
)

public enum class TodoSort { NEWEST_FIRST, OLDEST_FIRST }

/**
 * The only public surface of `:core:database` for the todo table.
 *
 * One narrow interface per table rather than a single app-wide store, so this cannot
 * grow into a god-interface as features arrive. Room, the DAO, the entity and the
 * database are all `internal`; nothing above this module can name them.
 */
public interface TodoLocalStore {
    public fun observe(sort: TodoSort): Flow<List<TodoRecord>>

    public suspend fun add(
        text: String,
        createdAt: Long,
    )

    public suspend fun setDone(
        id: Long,
        isDone: Boolean,
    )

    public suspend fun delete(id: Long)
}
