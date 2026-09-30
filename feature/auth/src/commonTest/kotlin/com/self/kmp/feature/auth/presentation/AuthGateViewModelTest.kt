package com.self.kmp.feature.auth.presentation

import com.self.kmp.feature.auth.data.InMemoryAuthSessionCache
import com.self.kmp.feature.auth.domain.AuthRepository
import com.self.kmp.feature.auth.domain.AuthSessionCache
import com.self.kmp.feature.auth.domain.DeviceAuthAvailability
import com.self.kmp.feature.auth.domain.DeviceAuthResult
import com.self.kmp.feature.auth.domain.MarkAuthenticatedUseCase
import com.self.kmp.feature.auth.domain.ShouldRunAuthUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AuthGateViewModelTest {
    private class FakeAuthRepository(
        private val availability: DeviceAuthAvailability,
    ) : AuthRepository {
        override suspend fun availability(): DeviceAuthAvailability = availability
    }

    private fun viewModel(
        availability: DeviceAuthAvailability,
        cache: AuthSessionCache = InMemoryAuthSessionCache(),
    ) = AuthGateViewModel(
        shouldRunAuth = ShouldRunAuthUseCase(FakeAuthRepository(availability), cache),
        markAuthenticated = MarkAuthenticatedUseCase(cache),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun asksTheUiForAPromptWhenTheDeviceCanAuthenticate() = runTest {
        val state = viewModel(DeviceAuthAvailability.AVAILABLE).state.value

        assertEquals(AuthGatePhase.AWAITING_PROMPT, state.phase)
        assertEquals(1, state.attempt)
    }

    @Test
    fun unlocksImmediatelyWhereTheDeviceCannotAuthenticate() = runTest {
        // Desktop and web, and an Android device with no screen lock.
        val state = viewModel(DeviceAuthAvailability.NOT_SUPPORTED).state.value

        assertEquals(AuthGatePhase.UNLOCKED, state.phase)
        assertNull(state.message)
    }

    @Test
    fun unlocksAndRemembersTheSessionWhenThePromptSucceeds() = runTest {
        val cache = InMemoryAuthSessionCache()
        val viewModel = viewModel(DeviceAuthAvailability.AVAILABLE, cache)

        viewModel.onIntent(AuthGateIntent.PromptCompleted(DeviceAuthResult.Success))

        assertEquals(AuthGatePhase.UNLOCKED, viewModel.state.value.phase)
        assertTrue(cache.isAuthenticated(), "a successful unlock must not re-prompt this process")
    }

    @Test
    fun blocksWithAMessageWhenTheUserCancels() = runTest {
        val viewModel = viewModel(DeviceAuthAvailability.AVAILABLE)

        viewModel.onIntent(AuthGateIntent.PromptCompleted(DeviceAuthResult.Cancelled))

        val state = viewModel.state.value
        assertEquals(AuthGatePhase.BLOCKED, state.phase)
        assertEquals(DeviceAuthResult.Cancelled.toUiMessage(), state.message)
    }

    @Test
    fun retryIncrementsTheAttemptSoTheUiEffectReFires() = runTest {
        val viewModel = viewModel(DeviceAuthAvailability.AVAILABLE)
        viewModel.onIntent(AuthGateIntent.PromptCompleted(DeviceAuthResult.Failed))
        val blockedAttempt = viewModel.state.value.attempt

        viewModel.onIntent(AuthGateIntent.Retry)

        val state = viewModel.state.value
        assertEquals(AuthGatePhase.AWAITING_PROMPT, state.phase)
        assertEquals(blockedAttempt + 1, state.attempt)
        assertNull(state.message)
    }
}
