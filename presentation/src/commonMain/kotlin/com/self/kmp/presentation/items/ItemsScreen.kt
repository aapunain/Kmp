package com.self.kmp.presentation.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point: the only Composable that knows about DI or a ViewModel.
 */
@Composable
fun ItemsScreen(
    modifier: Modifier = Modifier,
    viewModel: ItemsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ItemsContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/**
 * Stateless and dependency-free, so it can be previewed, screenshot-tested, or
 * driven from a test with a hand-built state. Splitting the screen this way is
 * what keeps "UI logic in the presentation layer" from becoming "UI logic tangled
 * with DI".
 */
@Composable
internal fun ItemsContent(
    state: ItemsUiState,
    onIntent: (ItemsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.semantics { }, // loading indicator is announced by role
            )

            state.errorMessage != null -> ErrorState(
                message = state.errorMessage,
                onRetry = { onIntent(ItemsIntent.Retry) },
            )

            state.isEmpty -> Text(
                text = "Nothing to show yet.",
                style = MaterialTheme.typography.bodyLarge,
            )

            else -> ItemsList(items = state.items)
        }
    }
}

@Composable
private fun ItemsList(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = items,
            key = { index, item -> "$index-$item" },
        ) { _, item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
