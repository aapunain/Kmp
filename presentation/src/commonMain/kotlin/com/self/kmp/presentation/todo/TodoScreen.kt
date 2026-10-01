package com.self.kmp.presentation.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.self.kmp.domain.todo.TodoOrder
import org.koin.compose.viewmodel.koinViewModel

/**
 * The home screen. Stateful entry point: the only Composable here that knows about DI.
 *
 * [onOpenSample] is a navigation callback rather than a nav dependency, so
 * `:presentation` needs no navigation library and this screen stays previewable.
 */
@Composable
public fun TodoScreen(
    onOpenSample: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TodoContent(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenSample = onOpenSample,
        modifier = modifier,
    )
}

/** Stateless, so it can be previewed and tested with a hand-built state. */
@Composable
internal fun TodoContent(
    state: TodoUiState,
    onIntent: (TodoIntent) -> Unit,
    onOpenSample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExpandingFab(
                onAddTodo = { onIntent(TodoIntent.OpenAddDialog) },
                onOpenSample = onOpenSample,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OrderFilter(
                order = state.order,
                onOrderChange = { onIntent(TodoIntent.OrderChanged(it)) },
            )

            when {
                state.isLoading -> {
                    Centered { CircularProgressIndicator() }
                }

                state.isEmpty -> {
                    Centered {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Text("Nothing to do yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap the + button to add your first todo.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> {
                    TodoList(
                        todos = state.todos,
                        onToggle = { onIntent(TodoIntent.ToggleDone(it)) },
                        onDelete = { onIntent(TodoIntent.Delete(it)) },
                    )
                }
            }
        }
    }

    if (state.isAddDialogVisible) {
        AddTodoDialog(
            text = state.draftText,
            error = state.draftError,
            onTextChange = { onIntent(TodoIntent.DraftChanged(it)) },
            onConfirm = { onIntent(TodoIntent.ConfirmAdd) },
            onDismiss = { onIntent(TodoIntent.DismissAddDialog) },
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun OrderFilter(
    order: TodoOrder,
    onOrderChange: (TodoOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = order == TodoOrder.NEWEST_FIRST,
            onClick = { onOrderChange(TodoOrder.NEWEST_FIRST) },
            label = { Text("Newest") },
        )
        FilterChip(
            selected = order == TodoOrder.OLDEST_FIRST,
            onClick = { onOrderChange(TodoOrder.OLDEST_FIRST) },
            label = { Text("Oldest") },
        )
    }
}

@Composable
private fun TodoList(
    todos: List<TodoRow>,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = todos, key = { it.id }) { todo ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = todo.isDone,
                        onCheckedChange = { onToggle(todo.id) },
                    )
                    Text(
                        text = todo.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    )
                    TextButton(onClick = { onDelete(todo.id) }) { Text("Delete") }
                }
            }
        }
    }
}

/**
 * Two mini actions that expand above the main button, per the agreed design. Expansion
 * is local UI state: nothing outside this composable cares whether the menu is open.
 */
@Composable
private fun ExpandingFab(
    onAddTodo: () -> Unit,
    onOpenSample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Add todo") },
                    icon = { Text("+") },
                    onClick = {
                        expanded = false
                        onAddTodo()
                    },
                )
                ExtendedFloatingActionButton(
                    text = { Text("Go to sample") },
                    icon = { Text(">") },
                    onClick = {
                        expanded = false
                        onOpenSample()
                    },
                )
            }
        }

        FloatingActionButton(onClick = { expanded = !expanded }) {
            Text(text = if (expanded) "x" else "+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun AddTodoDialog(
    text: String,
    error: String?,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Dismisses on outside tap and on back, which is AlertDialog's default behaviour.
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New todo") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = true,
                    isError = error != null,
                    label = { Text("What needs doing?") },
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
