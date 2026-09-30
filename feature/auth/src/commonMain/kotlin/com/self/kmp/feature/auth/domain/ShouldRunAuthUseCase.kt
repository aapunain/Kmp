package com.self.kmp.feature.auth.domain

/**
 * The launch policy: prompt only if the user has not already authenticated in this
 * process *and* the device can actually authenticate.
 *
 * This **fails open**. A device with no screen lock configured has nothing to prove,
 * so refusing entry would make the app unusable rather than more secure. Correct for
 * an app lock; wrong for protecting secrets at rest. See ADR-0015.
 */
public class ShouldRunAuthUseCase(
    private val authRepository: AuthRepository,
    private val authSessionCache: AuthSessionCache,
) {
    public suspend operator fun invoke(): Boolean {
        if (authSessionCache.isAuthenticated()) return false
        return authRepository.availability() == DeviceAuthAvailability.AVAILABLE
    }
}

/** Records a successful authentication so the gate does not re-prompt this process. */
public class MarkAuthenticatedUseCase(
    private val authSessionCache: AuthSessionCache,
) {
    public operator fun invoke() {
        authSessionCache.markAuthenticated()
    }
}
