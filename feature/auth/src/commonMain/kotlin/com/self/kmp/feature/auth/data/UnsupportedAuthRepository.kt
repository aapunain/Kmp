package com.self.kmp.feature.auth.data

import com.self.kmp.feature.auth.domain.AuthRepository
import com.self.kmp.feature.auth.domain.DeviceAuthAvailability

/**
 * Desktop and web. Reporting `NOT_SUPPORTED` makes `ShouldRunAuthUseCase` unlock without
 * ever requesting a prompt, so those platforms need no special casing at the call site.
 *
 * Declared once in common rather than three times: the platforms behave identically and
 * the default source set hierarchy has no group spanning jvm, js and wasmJs.
 */
internal class UnsupportedAuthRepository : AuthRepository {
    override suspend fun availability(): DeviceAuthAvailability = DeviceAuthAvailability.NOT_SUPPORTED
}
