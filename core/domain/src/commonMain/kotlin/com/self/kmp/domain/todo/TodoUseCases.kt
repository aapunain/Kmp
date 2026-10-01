package com.self.kmp.domain.todo

import kotlinx.coroutines.flow.Flow

/** Streams the list in the requested order. */
public class ObserveTodosUseCase(
    private val todoRepository: TodoRepository,
) {
    public operator fun invoke(order: TodoOrder): Flow<List<Todo>> = todoRepository.observeTodos(order)
}

/** Outcome of trying to add a todo. */
public sealed interface AddTodoResult {
    public data object Added : AddTodoResult

    /** The text was empty or only whitespace. */
    public data object Blank : AddTodoResult
}

/**
 * Business rule: a todo must have visible text.
 *
 * Trimming happens here rather than in the UI so that every caller gets the same
 * behaviour, and so the rule is testable without Compose. Duplicates are allowed by
 * design — two todos with the same text are two separate tasks.
 */
public class AddTodoUseCase(
    private val todoRepository: TodoRepository,
) {
    public suspend operator fun invoke(rawText: String): AddTodoResult {
        val text = rawText.trim()
        if (text.isEmpty()) return AddTodoResult.Blank

        todoRepository.addTodo(text)
        return AddTodoResult.Added
    }
}

public class ToggleTodoUseCase(
    private val todoRepository: TodoRepository,
) {
    public suspend operator fun invoke(todo: Todo) {
        todoRepository.setDone(id = todo.id, isDone = !todo.isDone)
    }
}

public class DeleteTodoUseCase(
    private val todoRepository: TodoRepository,
) {
    public suspend operator fun invoke(id: Long) {
        todoRepository.deleteTodo(id)
    }
}
