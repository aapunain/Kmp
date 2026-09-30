package com.self.kmp.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.kmp.feature.auth.domain.DeviceAuthResult
import com.self.kmp.feature.auth.domain.MarkAuthenticatedUseCase
import com.self.kmp.feature.auth.domain.ShouldRunAuthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public enum class AuthGatePhase {
    /** Deciding whether a prompt is needed. */
    CHECKING,

    /** A prompt should be shown. The UI owns the actual call. */
    AWAITING_PROMPT,

    UNLOCKED,

    /** Authentication was required and not obtained. */
    BLOCKED,
}

public data class AuthGateUiState(
    val phase: AuthGatePhase = AuthGatePhase.CHECKING,
    val message: String? = null,
    /**
     * Incremented on every attempt. The UI keys its `LaunchedEffect` on this so that a
     * retry re-fires even though [phase] returns to the same value.
     */
    val attempt: Int = 0,
)

public sealed interface AuthGateIntent {
    /** Decide whether to prompt. Sent once on creation. */
    public data object Start : AuthGateIntent

    /** The UI finished showing the prompt and is reporting the outcome. */
    public data class PromptCompleted(
        val result: DeviceAuthResult,
    ) : AuthGateIntent

    public data object Retry : AuthGateIntent
}

/**
 * Holds the gate's state machine. It never touches a platform API and never holds a
 * [com.self.kmp.feature.auth.domain.DeviceAuthPrompt]: showing the prompt needs an
 * Activity on Android, which is UI-scoped, so the UI performs that step and reports
 * back a domain result.
 */
public class AuthGateViewModel(
    private val shouldRunAuth: ShouldRunAuthUseCase,
    private val markAuthenticated: MarkAuthenticatedUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthGateUiState())
    public val state: StateFlow<AuthGateUiState> = _state.asStateFlow()

    init {
        onIntent(AuthGateIntent.Start)
    }

    public fun onIntent(intent: AuthGateIntent) {
        when (intent) {
            AuthGateIntent.Start -> decide()
            AuthGateIntent.Retry -> requestPrompt()
            is AuthGateIntent.PromptCompleted -> handle(intent.result)
        }
    }

    private fun decide() {
        viewModelScope.launch {
            if (shouldRunAuth()) {
                requestPrompt()
            } else {
                _state.update { it.copy(phase = AuthGatePhase.UNLOCKED, message = null) }
            }
        }
    }

    private fun requestPrompt() {
        _state.update {
            it.copy(
                phase = AuthGatePhase.AWAITING_PROMPT,
                message = null,
                attempt = it.attempt + 1,
            )
        }
    }

    private fun handle(result: DeviceAuthResult) {
        if (result == DeviceAuthResult.Success) {
            markAuthenticated()
            _state.update { it.copy(phase = AuthGatePhase.UNLOCKED, message = null) }
        } else {
            _state.update { it.copy(phase = AuthGatePhase.BLOCKED, message = result.toUiMessage()) }
        }
    }
}

/** Wording is a UI concern; in a real app these would be localized resources. */
internal fun DeviceAuthResult.toUiMessage(): String = when (this) {
    DeviceAuthResult.Success -> ""
    DeviceAuthResult.Cancelled -> "Unlock to continue."
    DeviceAuthResult.Failed -> "We couldn't verify you. Try again."
    is DeviceAuthResult.Error -> message ?: "Authentication is unavailable right now."
}
