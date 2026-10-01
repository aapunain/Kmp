package com.self.kmp.data.todo

import com.self.kmp.concurrency.DispatcherProvider
import com.self.kmp.database.TodoLocalStore
import com.self.kmp.database.TodoRecord
import com.self.kmp.database.TodoSort
import com.self.kmp.domain.todo.Todo
import com.self.kmp.domain.todo.TodoOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * The data layer's job here is translation in both directions: the domain's [TodoOrder]
 * down to the store's [TodoSort], and the store's [TodoRecord] up to the domain's [Todo].
 * Neither type is visible to the other layer, so this is the only place it can be tested.
 *
 * Note what is absent: no `AppResult`. The local database is the source of truth and a
 * read against it does not fail in a way the domain needs to model. See the KDoc on
 * `TodoRepositoryImpl`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoRepositoryImplTest {
    private class FakeTodoLocalStore(
        initial: List<TodoRecord> = emptyList(),
    ) : TodoLocalStore {
        val rows = MutableStateFlow(initial)
        val sortsRequested = mutableListOf<TodoSort>()
        val added = mutableListOf<Pair<String, Long>>()
        val doneCalls = mutableListOf<Pair<Long, Boolean>>()
        val deleted = mutableListOf<Long>()

        override fun observe(sort: TodoSort): Flow<List<TodoRecord>> {
            sortsRequested += sort
            return rows
        }

        override suspend fun add(
            text: String,
            createdAt: Long,
        ) {
            added += text to createdAt
        }

        override suspend fun setDone(
            id: Long,
            isDone: Boolean,
        ) {
            doneCalls += id to isDone
        }

        override suspend fun delete(id: Long) {
            deleted += id
        }
    }

    private class TestDispatcherProvider(
        dispatcher: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
    }

    private fun repository(store: TodoLocalStore) = TodoRepositoryImpl(
        todoLocalStore = store,
        dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher()),
    )

    @Test
    fun storeRecordsAreMappedToDomainModels() = runTest {
        val store =
            FakeTodoLocalStore(
                listOf(
                    TodoRecord(id = 1, text = "buy milk", isDone = false, createdAt = 10),
                    TodoRecord(id = 2, text = "water plants", isDone = true, createdAt = 20),
                ),
            )

        val todos = repository(store).observeTodos(TodoOrder.NEWEST_FIRST).first()

        assertEquals(
            listOf(
                Todo(id = 1, text = "buy milk", isDone = false, createdAt = 10),
                Todo(id = 2, text = "water plants", isDone = true, createdAt = 20),
            ),
            todos,
        )
    }

    @Test
    fun domainOrderIsTranslatedIntoTheStoresSortSoTheDatabaseDoesTheOrdering() = runTest {
        val store = FakeTodoLocalStore()
        val repository = repository(store)

        repository.observeTodos(TodoOrder.NEWEST_FIRST).first()
        repository.observeTodos(TodoOrder.OLDEST_FIRST).first()

        assertEquals(listOf(TodoSort.NEWEST_FIRST, TodoSort.OLDEST_FIRST), store.sortsRequested)
    }

    @Test
    fun anEmptyTableBecomesAnEmptyListAndNotAnError() = runTest {
        val todos = repository(FakeTodoLocalStore()).observeTodos(TodoOrder.NEWEST_FIRST).first()

        assertTrue(todos.isEmpty())
    }

    @Test
    fun addStampsTheCreationTimeSoTheDomainNeverHasToKnowTheClock() = runTest {
        val store = FakeTodoLocalStore()

        repository(store).addTodo("buy milk")

        val (text, createdAt) = store.added.single()
        assertEquals("buy milk", text)
        assertTrue(createdAt > 0, "a real timestamp should have been supplied, got $createdAt")
    }

    @Test
    fun toggleAndDeleteArePassedStraightThrough() = runTest {
        val store = FakeTodoLocalStore()
        val repository = repository(store)

        repository.setDone(id = 7, isDone = true)
        repository.deleteTodo(id = 9)

        assertEquals(listOf(7L to true), store.doneCalls)
        assertEquals(listOf(9L), store.deleted)
    }
}
