package com.self.kmp.presentation.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.kmp.domain.todo.AddTodoResult
import com.self.kmp.domain.todo.AddTodoUseCase
import com.self.kmp.domain.todo.DeleteTodoUseCase
import com.self.kmp.domain.todo.ObserveTodosUseCase
import com.self.kmp.domain.todo.Todo
import com.self.kmp.domain.todo.TodoOrder
import com.self.kmp.domain.todo.ToggleTodoUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Display-ready row. `id` is kept because the UI needs it to toggle and delete. */
public data class TodoRow(
    val id: Long,
    val text: String,
    val isDone: Boolean,
)

public data class TodoUiState(
    val isLoading: Boolean = true,
    val todos: List<TodoRow> = emptyList(),
    val order: TodoOrder = TodoOrder.NEWEST_FIRST,
    val isAddDialogVisible: Boolean = false,
    val draftText: String = "",
    val draftError: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && todos.isEmpty()
}

public sealed interface TodoIntent {
    public data object OpenAddDialog : TodoIntent

    public data object DismissAddDialog : TodoIntent

    public data class DraftChanged(
        val text: String,
    ) : TodoIntent

    public data object ConfirmAdd : TodoIntent

    public data class ToggleDone(
        val id: Long,
    ) : TodoIntent

    public data class Delete(
        val id: Long,
    ) : TodoIntent

    public data class OrderChanged(
        val order: TodoOrder,
    ) : TodoIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
public class TodoViewModel(
    private val observeTodos: ObserveTodosUseCase,
    private val addTodo: AddTodoUseCase,
    private val toggleTodo: ToggleTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase,
) : ViewModel() {
    private val order = MutableStateFlow(TodoOrder.NEWEST_FIRST)
    private val dialog = MutableStateFlow(DialogState())

    // The list is driven by the database, so the order change re-subscribes rather than
    // re-sorting in memory. flatMapLatest cancels the previous query.
    private val todos: StateFlow<List<Todo>?> =
        order
            .flatMapLatest { observeTodos(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    public val state: StateFlow<TodoUiState> =
        combine(todos, order, dialog.asStateFlow()) { rows, currentOrder, dialogState ->
            TodoUiState(
                isLoading = rows == null,
                todos = rows.orEmpty().map { it.toRow() },
                order = currentOrder,
                isAddDialogVisible = dialogState.isVisible,
                draftText = dialogState.text,
                draftError = dialogState.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())

    public fun onIntent(intent: TodoIntent) {
        when (intent) {
            TodoIntent.OpenAddDialog -> {
                dialog.value = DialogState(isVisible = true)
            }

            TodoIntent.DismissAddDialog -> {
                dialog.value = DialogState()
            }

            is TodoIntent.DraftChanged -> {
                dialog.update { it.copy(text = intent.text, error = null) }
            }

            TodoIntent.ConfirmAdd -> {
                confirmAdd()
            }

            is TodoIntent.ToggleDone -> {
                viewModelScope.launch {
                    todos.value?.firstOrNull { it.id == intent.id }?.let { toggleTodo(it) }
                }
            }

            is TodoIntent.Delete -> {
                viewModelScope.launch { deleteTodo(intent.id) }
            }

            is TodoIntent.OrderChanged -> {
                order.value = intent.order
            }
        }
    }

    private fun confirmAdd() {
        val draft = dialog.value.text
        viewModelScope.launch {
            when (addTodo(draft)) {
                AddTodoResult.Added -> {
                    dialog.value = DialogState()
                }

                // The rule lives in the use case; this only renders its verdict.
                AddTodoResult.Blank -> {
                    dialog.update { it.copy(error = "Enter something first.") }
                }
            }
        }
    }

    private data class DialogState(
        val isVisible: Boolean = false,
        val text: String = "",
        val error: String? = null,
    )
}

private fun Todo.toRow(): TodoRow = TodoRow(id = id, text = text, isDone = isDone)
