package com.self.kmp.database.internal

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.self.kmp.database.TodoSort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Runs against a real SQLite database, in memory, through the bundled driver.
 *
 * This is the only test in the project that executes the generated Room code, and it is
 * the one that can catch the mistakes unit tests with a fake store cannot: a wrong
 * `ORDER BY`, a column name that does not match the entity, `autoGenerate` not actually
 * generating, or a `Boolean` that does not survive the round trip through SQLite's
 * integer storage.
 *
 * It lives in `jvmTest` rather than `commonTest` because the bundled driver has no
 * JS/Wasm variant — those targets run [InMemoryTodoLocalStore] in production, which has
 * its own test. The schema and the DAO are common code, so verifying them once on the
 * JVM verifies them for Android and iOS too.
 */
class RoomTodoLocalStoreTest {
    private lateinit var database: AppDatabase
    private lateinit var store: RoomTodoLocalStore

    @BeforeTest
    fun setUp() {
        // Built the same way production builds it, minus the file path: no
        // setQueryCoroutineContext, so Room's own default is what gets exercised.
        database =
            Room
                .inMemoryDatabaseBuilder<AppDatabase>()
                .setDriver(BundledSQLiteDriver())
                .build()
        store = RoomTodoLocalStore(database.todoDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun startsEmpty() = runTest {
        assertTrue(store.observe(TodoSort.NEWEST_FIRST).first().isEmpty())
    }

    @Test
    fun insertedRowsComeBackWithGeneratedIdsAndTheValuesTheyWentInWith() = runTest {
        store.add(text = "buy milk", createdAt = 1_700_000_000_000)

        val row = store.observe(TodoSort.NEWEST_FIRST).first().single()
        assertEquals("buy milk", row.text)
        assertEquals(1_700_000_000_000, row.createdAt)
        assertTrue(row.id > 0, "autoGenerate should have assigned an id, got ${row.id}")
        assertEquals(false, row.isDone)
    }

    @Test
    fun theOrderByClausesActuallyOrderTheRows() = runTest {
        store.add(text = "oldest", createdAt = 100)
        store.add(text = "middle", createdAt = 200)
        store.add(text = "newest", createdAt = 300)

        assertEquals(
            listOf("newest", "middle", "oldest"),
            store.observe(TodoSort.NEWEST_FIRST).first().map { it.text },
        )
        assertEquals(
            listOf("oldest", "middle", "newest"),
            store.observe(TodoSort.OLDEST_FIRST).first().map { it.text },
        )
    }

    @Test
    fun booleansSurviveTheRoundTripThroughSqlitesIntegerStorage() = runTest {
        store.add(text = "toggle me", createdAt = 1)
        val id =
            store
                .observe(TodoSort.NEWEST_FIRST)
                .first()
                .single()
                .id

        store.setDone(id = id, isDone = true)
        assertEquals(
            true,
            store
                .observe(TodoSort.NEWEST_FIRST)
                .first()
                .single()
                .isDone,
        )

        store.setDone(id = id, isDone = false)
        assertEquals(
            false,
            store
                .observe(TodoSort.NEWEST_FIRST)
                .first()
                .single()
                .isDone,
        )
    }

    @Test
    fun setDoneTouchesOnlyTheMatchingRow() = runTest {
        store.add(text = "first", createdAt = 1)
        store.add(text = "second", createdAt = 2)
        val first =
            store
                .observe(TodoSort.OLDEST_FIRST)
                .first()
                .first()
                .id

        store.setDone(id = first, isDone = true)

        assertEquals(
            listOf(true, false),
            store.observe(TodoSort.OLDEST_FIRST).first().map { it.isDone },
        )
    }

    @Test
    fun deleteRemovesOnlyTheMatchingRow() = runTest {
        store.add(text = "keep", createdAt = 1)
        store.add(text = "remove", createdAt = 2)
        val doomed =
            store
                .observe(TodoSort.NEWEST_FIRST)
                .first()
                .single { it.text == "remove" }
                .id

        store.delete(doomed)

        assertEquals(listOf("keep"), store.observe(TodoSort.NEWEST_FIRST).first().map { it.text })
    }

    @Test
    fun writesToAnUnknownIdMatchNoRowsInsteadOfFailing() = runTest {
        store.add(text = "first", createdAt = 1)

        store.setDone(id = 9_999, isDone = true)
        store.delete(id = 9_999)

        val rows = store.observe(TodoSort.NEWEST_FIRST).first()
        assertEquals(1, rows.size)
        assertEquals(false, rows.single().isDone)
    }

    @Test
    fun duplicateTextIsAcceptedAndGetsItsOwnRow() = runTest {
        store.add(text = "water plants", createdAt = 1)
        store.add(text = "water plants", createdAt = 2)

        val rows = store.observe(TodoSort.NEWEST_FIRST).first()
        assertEquals(2, rows.size)
        assertEquals(2, rows.map { it.id }.distinct().size, "ids must be distinct")
    }

    @Test
    fun anExistingObserverIsNotifiedOfLaterWrites() = runTest {
        val rows = store.observe(TodoSort.NEWEST_FIRST)
        assertTrue(rows.first().isEmpty())

        store.add(text = "added after subscribing", createdAt = 1)

        // Room's invalidation tracker is what makes the UI update without the view model
        // re-querying. If this ever regresses, the screen silently stops refreshing.
        assertEquals(listOf("added after subscribing"), rows.first().map { it.text })
    }
}
