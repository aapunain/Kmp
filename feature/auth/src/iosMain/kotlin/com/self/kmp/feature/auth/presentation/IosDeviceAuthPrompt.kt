package com.self.kmp.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.self.kmp.feature.auth.domain.DeviceAuthPrompt
import com.self.kmp.feature.auth.domain.DeviceAuthResult
import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAppCancel
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

/**
 * iOS needs no Activity, so this could have been injected. It comes from the composition
 * anyway so that both platforms follow one shape.
 */
@Composable
internal actual fun rememberDeviceAuthPrompt(): DeviceAuthPrompt = remember { IosDeviceAuthPrompt() }

internal class IosDeviceAuthPrompt : DeviceAuthPrompt {
    // The completion handler crosses into Objective-C, which is what needs the opt-in.
    @OptIn(BetaInteropApi::class)
    override suspend fun authenticate(): DeviceAuthResult = suspendCancellableCoroutine { continuation ->
        // A fresh LAContext per attempt: reusing one can return a cached result instead
        // of prompting again.
        LAContext().evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthentication,
            localizedReason = "Unlock Kmp to continue",
        ) { success, error ->
            if (!continuation.isActive) return@evaluatePolicy

            val result =
                when {
                    success -> {
                        DeviceAuthResult.Success
                    }

                    else -> {
                        when (error?.code) {
                            LAErrorUserCancel,
                            LAErrorSystemCancel,
                            LAErrorAppCancel,
                            -> DeviceAuthResult.Cancelled

                            LAErrorAuthenticationFailed -> DeviceAuthResult.Failed

                            else -> DeviceAuthResult.Error(error?.localizedDescription)
                        }
                    }
                }

            continuation.resume(result)
        }
    }
}
