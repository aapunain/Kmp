package com.self.kmp.database.internal

import com.self.kmp.database.TodoSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The store JS and Wasm actually run on, so its behaviour is not a test convenience —
 * it is production behaviour on two of the five targets.
 *
 * These assertions are deliberately written against the [com.self.kmp.database.TodoLocalStore]
 * contract rather than the implementation, so the same expectations describe the
 * Room-backed store. The one difference is durability, which no unit test can observe.
 */
class InMemoryTodoLocalStoreTest {
    @Test
    fun startsEmpty() = runTest {
        assertTrue(InMemoryTodoLocalStore().observe(TodoSort.NEWEST_FIRST).first().isEmpty())
    }

    @Test
    fun addAssignsIncreasingIdsAndStartsUndone() = runTest {
        val store = InMemoryTodoLocalStore()

        store.add(text = "first", createdAt = 1)
        store.add(text = "second", createdAt = 2)

        val rows = store.observe(TodoSort.OLDEST_FIRST).first()
        assertEquals(listOf("first", "second"), rows.map { it.text })
        assertEquals(listOf(1L, 2L), rows.map { it.id }, "ids must be unique and stable")
        assertTrue(rows.none { it.isDone }, "a new todo is not done")
    }

    @Test
    fun sortIsAppliedOnReadSoTheSameDataServesBothOrders() = runTest {
        val store = InMemoryTodoLocalStore()
        store.add(text = "older", createdAt = 100)
        store.add(text = "newer", createdAt = 200)

        assertEquals(
            listOf("newer", "older"),
            store.observe(TodoSort.NEWEST_FIRST).first().map { it.text },
        )
        assertEquals(
            listOf("older", "newer"),
            store.observe(TodoSort.OLDEST_FIRST).first().map { it.text },
        )
    }

    @Test
    fun duplicateTextIsAllowedBecauseTwoIdenticalTasksAreStillTwoTasks() = runTest {
        val store = InMemoryTodoLocalStore()

        store.add(text = "water plants", createdAt = 1)
        store.add(text = "water plants", createdAt = 2)

        val rows = store.observe(TodoSort.OLDEST_FIRST).first()
        assertEquals(2, rows.size)
        assertEquals(2, rows.map { it.id }.distinct().size)
    }

    @Test
    fun setDoneUpdatesOnlyTheTargetRow() = runTest {
        val store = InMemoryTodoLocalStore()
        store.add(text = "first", createdAt = 1)
        store.add(text = "second", createdAt = 2)

        store.setDone(id = 1, isDone = true)

        val rows = store.observe(TodoSort.OLDEST_FIRST).first()
        assertEquals(listOf(true, false), rows.map { it.isDone })
    }

    @Test
    fun setDoneIsReversible() = runTest {
        val store = InMemoryTodoLocalStore()
        store.add(text = "first", createdAt = 1)

        store.setDone(id = 1, isDone = true)
        store.setDone(id = 1, isDone = false)

        assertTrue(
            store
                .observe(TodoSort.NEWEST_FIRST)
                .first()
                .single()
                .isDone
                .not(),
        )
    }

    @Test
    fun writesToAnUnknownIdAreNoOpsRatherThanFailures() = runTest {
        val store = InMemoryTodoLocalStore()
        store.add(text = "first", createdAt = 1)

        // A stale id can arrive from the UI after a delete; the same SQL UPDATE/DELETE
        // would simply match no rows, so the in-memory store must behave the same way.
        store.setDone(id = 404, isDone = true)
        store.delete(id = 404)

        val rows = store.observe(TodoSort.NEWEST_FIRST).first()
        assertEquals(1, rows.size)
        assertTrue(rows.single().isDone.not())
    }

    @Test
    fun deleteRemovesOnlyTheTargetRow() = runTest {
        val store = InMemoryTodoLocalStore()
        store.add(text = "keep", createdAt = 1)
        store.add(text = "remove", createdAt = 2)

        store.delete(id = 2)

        assertEquals(listOf("keep"), store.observe(TodoSort.NEWEST_FIRST).first().map { it.text })
    }

    @Test
    fun anExistingObserverSeesLaterWrites() = runTest {
        val store = InMemoryTodoLocalStore()
        val rows = store.observe(TodoSort.NEWEST_FIRST)

        assertTrue(rows.first().isEmpty())
        store.add(text = "added after subscribing", createdAt = 1)

        // Reactive, not a one-shot read: this is what lets the UI update without the
        // view model re-querying after every write.
        assertEquals(listOf("added after subscribing"), rows.first().map { it.text })
    }

    @Test
    fun idsAreNotReusedAfterADelete() = runTest {
        val store = InMemoryTodoLocalStore()
        store.add(text = "first", createdAt = 1)
        store.delete(id = 1)

        store.add(text = "second", createdAt = 2)

        assertEquals(
            2L,
            store
                .observe(TodoSort.NEWEST_FIRST)
                .first()
                .single()
                .id,
        )
    }
}
