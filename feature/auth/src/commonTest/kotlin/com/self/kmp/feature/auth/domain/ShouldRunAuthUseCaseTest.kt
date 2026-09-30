package com.self.kmp.feature.auth.domain

import com.self.kmp.feature.auth.data.InMemoryAuthSessionCache
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The launch policy, tested without a device. This is the point of keeping the decision
 * in the domain: the interesting behaviour is what happens when the device cannot
 * authenticate, or when the user already has, and neither needs hardware to verify.
 */
class ShouldRunAuthUseCaseTest {
    private class FakeAuthRepository(
        private val availability: DeviceAuthAvailability,
    ) : AuthRepository {
        var queried: Boolean = false
            private set

        override suspend fun availability(): DeviceAuthAvailability {
            queried = true
            return availability
        }
    }

    @Test
    fun promptsWhenTheDeviceSupportsAuthAndTheUserHasNotAuthenticatedYet() = runTest {
        val useCase =
            ShouldRunAuthUseCase(
                authRepository = FakeAuthRepository(DeviceAuthAvailability.AVAILABLE),
                authSessionCache = InMemoryAuthSessionCache(),
            )

        assertTrue(useCase())
    }

    @Test
    fun doesNotPromptTwiceInTheSameProcess() = runTest {
        val cache = InMemoryAuthSessionCache()
        val useCase =
            ShouldRunAuthUseCase(
                authRepository = FakeAuthRepository(DeviceAuthAvailability.AVAILABLE),
                authSessionCache = cache,
            )

        assertTrue(useCase())

        MarkAuthenticatedUseCase(cache).invoke()

        assertFalse(useCase())
    }

    @Test
    fun failsOpenAndSkipsTheCapabilityCheckOnceAuthenticated() = runTest {
        val cache = InMemoryAuthSessionCache().apply { markAuthenticated() }
        val repository = FakeAuthRepository(DeviceAuthAvailability.AVAILABLE)

        val shouldRun = ShouldRunAuthUseCase(repository, cache).invoke()

        assertFalse(shouldRun)
        assertFalse(repository.queried, "an authenticated session should short-circuit")
    }

    @Test
    fun failsOpenWhenTheDeviceCannotAuthenticate() = runTest {
        val cases =
            listOf(
                DeviceAuthAvailability.NOT_ENROLLED,
                DeviceAuthAvailability.NO_HARDWARE,
                DeviceAuthAvailability.NOT_SUPPORTED,
            )

        cases.forEach { availability ->
            val useCase =
                ShouldRunAuthUseCase(
                    authRepository = FakeAuthRepository(availability),
                    authSessionCache = InMemoryAuthSessionCache(),
                )

            assertFalse(useCase(), "must not prompt when $availability")
        }
    }
}
