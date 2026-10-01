package com.self.kmp.domain.todo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * The todo business rules, tested without a database, a Compose runtime or a dispatcher.
 * That is the whole point of keeping this module free of dependencies: these tests are
 * the fastest and most portable in the project.
 */
class TodoUseCasesTest {
    private class RecordingTodoRepository(
        private val todos: List<Todo> = emptyList(),
    ) : TodoRepository {
        val added = mutableListOf<String>()
        val doneCalls = mutableListOf<Pair<Long, Boolean>>()
        val deleted = mutableListOf<Long>()
        val ordersRequested = mutableListOf<TodoOrder>()

        override fun observeTodos(order: TodoOrder): Flow<List<Todo>> {
            ordersRequested += order
            return flowOf(todos)
        }

        override suspend fun addTodo(text: String) {
            added += text
        }

        override suspend fun setDone(
            id: Long,
            isDone: Boolean,
        ) {
            doneCalls += id to isDone
        }

        override suspend fun deleteTodo(id: Long) {
            deleted += id
        }
    }

    @Test
    fun addsTrimmedText() = runTest {
        val repository = RecordingTodoRepository()

        val result = AddTodoUseCase(repository).invoke("  buy milk  ")

        assertEquals(AddTodoResult.Added, result)
        assertEquals(listOf("buy milk"), repository.added)
    }

    @Test
    fun rejectsBlankTextWithoutTouchingTheRepository() = runTest {
        val repository = RecordingTodoRepository()
        val useCase = AddTodoUseCase(repository)

        listOf("", "   ", "\t", "\n  \n").forEach { input ->
            assertEquals(AddTodoResult.Blank, useCase(input), "input=[$input]")
        }

        assertTrue(repository.added.isEmpty(), "nothing should have been written")
    }

    @Test
    fun allowsDuplicatesBecauseTwoIdenticalTasksAreStillTwoTasks() = runTest {
        val repository = RecordingTodoRepository()
        val useCase = AddTodoUseCase(repository)

        useCase("water plants")
        useCase("water plants")

        assertEquals(listOf("water plants", "water plants"), repository.added)
    }

    @Test
    fun toggleFlipsTheCurrentState() = runTest {
        val repository = RecordingTodoRepository()

        ToggleTodoUseCase(repository).invoke(
            Todo(id = 7, text = "x", isDone = false, createdAt = 0),
        )

        assertEquals(listOf(7L to true), repository.doneCalls)
    }

    @Test
    fun toggleOnADoneTodoMarksItUndone() = runTest {
        val repository = RecordingTodoRepository()

        ToggleTodoUseCase(repository).invoke(
            Todo(id = 7, text = "x", isDone = true, createdAt = 0),
        )

        assertEquals(listOf(7L to false), repository.doneCalls)
    }

    @Test
    fun deletePassesTheIdStraightThrough() = runTest {
        val repository = RecordingTodoRepository()

        DeleteTodoUseCase(repository).invoke(id = 42)

        assertEquals(listOf(42L), repository.deleted)
    }

    @Test
    fun observeForwardsTheRequestedOrderAndTheRepositorysRows() = runTest {
        val rows = listOf(Todo(id = 1, text = "a", isDone = false, createdAt = 0))
        val repository = RecordingTodoRepository(rows)
        val useCase = ObserveTodosUseCase(repository)

        assertEquals(rows, useCase(TodoOrder.OLDEST_FIRST).first())
        useCase(TodoOrder.NEWEST_FIRST).first()

        // Ordering is pushed down to the repository, not applied here: the database can
        // do it in the query, and re-sorting in memory would fight the reactive stream.
        assertEquals(
            listOf(TodoOrder.OLDEST_FIRST, TodoOrder.NEWEST_FIRST),
            repository.ordersRequested,
        )
    }
}
