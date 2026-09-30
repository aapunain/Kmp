package com.self.kmp.feature.auth.domain

/** Whether this device can ask the user to prove who they are with their screen lock. */
public enum class DeviceAuthAvailability {
    /** A biometric or a device credential is enrolled and usable now. */
    AVAILABLE,

    /** Support exists, but the user has not set a screen lock. */
    NOT_ENROLLED,

    /** No usable authentication hardware. */
    NO_HARDWARE,

    /** The platform has no device authentication at all: desktop and web. */
    NOT_SUPPORTED,
}

/** Outcome of one authentication attempt. */
public sealed interface DeviceAuthResult {
    public data object Success : DeviceAuthResult

    /** The user dismissed the prompt. */
    public data object Cancelled : DeviceAuthResult

    /** The user tried and did not match. Retrying is reasonable. */
    public data object Failed : DeviceAuthResult

    public data class Error(
        val message: String? = null,
    ) : DeviceAuthResult
}

/**
 * Asks the platform what it is capable of. Needs only a `Context` on Android, so it is
 * an ordinary injected dependency.
 */
public interface AuthRepository {
    public suspend fun availability(): DeviceAuthAvailability
}

/**
 * Remembers that the user already authenticated.
 *
 * **In memory only, for the lifetime of the process.** Persisting this would turn the
 * lock into something an attacker can defeat by editing a file, and the whole point of
 * the gate is that it cannot be set from outside the running app.
 */
public interface AuthSessionCache {
    public fun isAuthenticated(): Boolean

    public fun markAuthenticated()
}

/**
 * Shows the system prompt.
 *
 * Deliberately **not** an injected dependency: on Android this needs a
 * `FragmentActivity`, which is UI-scoped. It is obtained from the composition via
 * `rememberDeviceAuthPrompt()` and passed in per call, so no long-lived object ever
 * holds a reference to an Activity.
 */
public interface DeviceAuthPrompt {
    public suspend fun authenticate(): DeviceAuthResult
}
