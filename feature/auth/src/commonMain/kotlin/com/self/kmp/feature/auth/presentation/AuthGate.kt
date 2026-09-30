package com.self.kmp.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Shows [content] once the gate is unlocked.
 *
 * While locked, [content] is never invoked, so the screens behind it never compose and
 * their ViewModels never start work. The gate prevents the load rather than hiding it.
 */
@Composable
public fun AuthGate(
    modifier: Modifier = Modifier,
    viewModel: AuthGateViewModel = koinViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prompt = rememberDeviceAuthPrompt()

    // Keyed on `attempt` as well as `phase`: a retry sets the phase back to
    // AWAITING_PROMPT from BLOCKED, but a second failure would leave the phase
    // unchanged and the effect would not re-run without the counter.
    LaunchedEffect(state.phase, state.attempt) {
        if (state.phase == AuthGatePhase.AWAITING_PROMPT) {
            viewModel.onIntent(AuthGateIntent.PromptCompleted(prompt.authenticate()))
        }
    }

    if (state.phase == AuthGatePhase.UNLOCKED) {
        content()
    } else {
        AuthGateContent(
            state = state,
            onIntent = viewModel::onIntent,
            modifier = modifier,
        )
    }
}

/** Stateless, so it can be previewed and tested with a hand-built state. */
@Composable
internal fun AuthGateContent(
    state: AuthGateUiState,
    onIntent: (AuthGateIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.phase == AuthGatePhase.BLOCKED) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = "Locked", style = MaterialTheme.typography.titleLarge)
                state.message?.let { message ->
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Button(onClick = { onIntent(AuthGateIntent.Retry) }) {
                    Text("Unlock")
                }
            }
        } else {
            // CHECKING, or AWAITING_PROMPT while the system prompt is on screen.
            CircularProgressIndicator()
        }
    }
}
