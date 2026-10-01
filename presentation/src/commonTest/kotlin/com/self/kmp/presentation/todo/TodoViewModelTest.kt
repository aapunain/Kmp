package com.self.kmp.presentation.todo

import com.self.kmp.domain.todo.AddTodoUseCase
import com.self.kmp.domain.todo.DeleteTodoUseCase
import com.self.kmp.domain.todo.ObserveTodosUseCase
import com.self.kmp.domain.todo.Todo
import com.self.kmp.domain.todo.TodoOrder
import com.self.kmp.domain.todo.TodoRepository
import com.self.kmp.domain.todo.ToggleTodoUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * The view model's state is a `stateIn(..., WhileSubscribed)` pipeline, so nothing runs
 * until something collects. Every test therefore starts a collector before asserting;
 * reading `state.value` without one would only ever see the initial value.
 *
 * `Dispatchers.setMain` is declared in common by kotlinx-coroutines-test, which is why
 * one test covers all five targets. Passing the same dispatcher to `runTest` keeps the
 * test body and `viewModelScope` on one scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {
    /**
     * Reactive in the same way the real repository is: a write becomes visible on the
     * stream the view model is already collecting. A fake that only recorded calls could
     * not catch a view model that writes but never re-reads.
     */
    private class FakeTodoRepository : TodoRepository {
        private val rows = MutableStateFlow<List<Todo>>(emptyList())
        private var nextId = 1L
        private var nextCreatedAt = 0L

        val current: List<Todo> get() = rows.value

        override fun observeTodos(order: TodoOrder): Flow<List<Todo>> = rows.map { todos ->
            when (order) {
                TodoOrder.NEWEST_FIRST -> todos.sortedByDescending { it.createdAt }
                TodoOrder.OLDEST_FIRST -> todos.sortedBy { it.createdAt }
            }
        }

        override suspend fun addTodo(text: String) {
            rows.update { todos ->
                todos +
                    Todo(
                        id = nextId++,
                        text = text,
                        isDone = false,
                        createdAt = nextCreatedAt++,
                    )
            }
        }

        override suspend fun setDone(
            id: Long,
            isDone: Boolean,
        ) {
            rows.update { todos -> todos.map { if (it.id == id) it.copy(isDone = isDone) else it } }
        }

        override suspend fun deleteTodo(id: Long) {
            rows.update { todos -> todos.filterNot { it.id == id } }
        }
    }

    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: TodoRepository) = TodoViewModel(
        observeTodos = ObserveTodosUseCase(repository),
        addTodo = AddTodoUseCase(repository),
        toggleTodo = ToggleTodoUseCase(repository),
        deleteTodo = DeleteTodoUseCase(repository),
    )

    /** Subscribes so the `WhileSubscribed` pipeline is live for the rest of the test. */
    private fun CoroutineScope.observe(viewModel: TodoViewModel) {
        launch { viewModel.state.collect { } }
    }

    @Test
    fun anEmptyDatabaseReportsEmptyRatherThanLoadingOnceTheFirstQueryLands() = runTest(mainDispatcher) {
        val viewModel = viewModel(FakeTodoRepository())

        backgroundScope.observe(viewModel)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty, "an empty database is the empty state, not the loading state")
        assertEquals(TodoOrder.NEWEST_FIRST, state.order, "newest first is the default")
    }

    @Test
    fun confirmingAnAddClosesTheDialogAndTheNewTodoAppears() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)

        viewModel.onIntent(TodoIntent.OpenAddDialog)
        assertTrue(viewModel.state.value.isAddDialogVisible)

        viewModel.onIntent(TodoIntent.DraftChanged("buy milk"))
        assertEquals("buy milk", viewModel.state.value.draftText)

        viewModel.onIntent(TodoIntent.ConfirmAdd)

        val state = viewModel.state.value
        assertFalse(state.isAddDialogVisible, "a successful add dismisses the dialog")
        assertEquals("", state.draftText, "the draft is cleared, not left behind")
        assertEquals(listOf("buy milk"), state.todos.map { it.text })
        assertFalse(state.isEmpty)
    }

    @Test
    fun blankTextIsRejectedInPlaceWithTheDialogLeftOpen() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)

        viewModel.onIntent(TodoIntent.OpenAddDialog)
        viewModel.onIntent(TodoIntent.DraftChanged("   "))
        viewModel.onIntent(TodoIntent.ConfirmAdd)

        val state = viewModel.state.value
        assertTrue(state.isAddDialogVisible, "the dialog stays open so the text can be fixed")
        assertNotNull(state.draftError)
        assertTrue(repository.current.isEmpty(), "nothing was written")
    }

    @Test
    fun typingAgainClearsThePreviousError() = runTest(mainDispatcher) {
        val viewModel = viewModel(FakeTodoRepository())
        backgroundScope.observe(viewModel)

        viewModel.onIntent(TodoIntent.OpenAddDialog)
        viewModel.onIntent(TodoIntent.DraftChanged(""))
        viewModel.onIntent(TodoIntent.ConfirmAdd)
        assertNotNull(viewModel.state.value.draftError)

        viewModel.onIntent(TodoIntent.DraftChanged("b"))

        assertNull(viewModel.state.value.draftError)
    }

    @Test
    fun dismissingTheDialogDiscardsTheDraft() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)

        viewModel.onIntent(TodoIntent.OpenAddDialog)
        viewModel.onIntent(TodoIntent.DraftChanged("half typed"))
        viewModel.onIntent(TodoIntent.DismissAddDialog)

        val state = viewModel.state.value
        assertFalse(state.isAddDialogVisible)
        assertEquals("", state.draftText)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun togglingFlipsTheRowAndTogglingAgainFlipsItBack() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)
        repository.addTodo("water plants")

        val id =
            viewModel.state.value.todos
                .single()
                .id
        viewModel.onIntent(TodoIntent.ToggleDone(id))
        assertTrue(
            viewModel.state.value.todos
                .single()
                .isDone,
        )

        viewModel.onIntent(TodoIntent.ToggleDone(id))
        assertFalse(
            viewModel.state.value.todos
                .single()
                .isDone,
        )
    }

    @Test
    fun togglingAnIdThatIsNotOnScreenIsIgnored() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)
        repository.addTodo("water plants")

        viewModel.onIntent(TodoIntent.ToggleDone(id = 999))

        assertFalse(
            viewModel.state.value.todos
                .single()
                .isDone,
        )
    }

    @Test
    fun deletingRemovesTheRow() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)
        repository.addTodo("first")
        repository.addTodo("second")

        val doomed =
            viewModel.state.value.todos
                .first { it.text == "first" }
                .id
        viewModel.onIntent(TodoIntent.Delete(doomed))

        assertEquals(
            listOf("second"),
            viewModel.state.value.todos
                .map { it.text },
        )
    }

    @Test
    fun changingTheOrderReQueriesRatherThanReSortingInMemory() = runTest(mainDispatcher) {
        val repository = FakeTodoRepository()
        val viewModel = viewModel(repository)
        backgroundScope.observe(viewModel)
        repository.addTodo("older")
        repository.addTodo("newer")

        assertEquals(
            listOf("newer", "older"),
            viewModel.state.value.todos
                .map { it.text },
        )

        viewModel.onIntent(TodoIntent.OrderChanged(TodoOrder.OLDEST_FIRST))

        val state = viewModel.state.value
        assertEquals(TodoOrder.OLDEST_FIRST, state.order)
        assertEquals(listOf("older", "newer"), state.todos.map { it.text })
    }

    @Test
    fun theDefaultStateIsLoadingSoTheUiDoesNotFlashEmpty() {
        // No collector on purpose: this is what the UI renders on the very first frame,
        // before the database has answered.
        val initial = TodoUiState()
        assertTrue(initial.isLoading)
        assertFalse(initial.isEmpty, "loading is not the same as empty")
    }
}
