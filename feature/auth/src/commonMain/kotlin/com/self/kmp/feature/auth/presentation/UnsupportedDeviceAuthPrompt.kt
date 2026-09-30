package com.self.kmp.feature.auth.presentation

import com.self.kmp.feature.auth.domain.DeviceAuthPrompt
import com.self.kmp.feature.auth.domain.DeviceAuthResult

/**
 * Used on desktop and web, and on Android inside `@Preview` where no Activity exists.
 *
 * In practice it is never called: `ShouldRunAuthUseCase` sees `NOT_SUPPORTED` from the
 * repository and unlocks without requesting a prompt. It exists so the `expect`
 * declaration has something to return.
 */
internal class UnsupportedDeviceAuthPrompt : DeviceAuthPrompt {
    override suspend fun authenticate(): DeviceAuthResult =
        DeviceAuthResult.Error("Device authentication is not available on this platform")
}
