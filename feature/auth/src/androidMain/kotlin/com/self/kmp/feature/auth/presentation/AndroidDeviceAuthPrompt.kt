package com.self.kmp.feature.auth.presentation

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.self.kmp.feature.auth.domain.DeviceAuthPrompt
import com.self.kmp.feature.auth.domain.DeviceAuthResult
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * The Activity arrives as a constructor parameter, read from the composition. Nothing
 * outlives the composition, so there is no registry and no weak reference.
 *
 * `LocalContext` is not a `FragmentActivity` inside `@Preview`, so previews fall back to
 * the unsupported prompt and render unlocked.
 */
@Composable
internal actual fun rememberDeviceAuthPrompt(): DeviceAuthPrompt {
    val activity = LocalContext.current as? FragmentActivity
    return remember(activity) {
        if (activity == null) UnsupportedDeviceAuthPrompt() else AndroidDeviceAuthPrompt(activity)
    }
}

internal class AndroidDeviceAuthPrompt(
    private val activity: FragmentActivity,
) : DeviceAuthPrompt {
    override suspend fun authenticate(): DeviceAuthResult =
        // BiometricPrompt must be built and shown on the main thread.
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val callback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(DeviceAuthResult.Success)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            if (!continuation.isActive) return
                            continuation.resume(
                                when (errorCode) {
                                    BiometricPrompt.ERROR_USER_CANCELED,
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                    BiometricPrompt.ERROR_CANCELED,
                                    -> DeviceAuthResult.Cancelled

                                    else -> DeviceAuthResult.Error(errString.toString())
                                },
                            )
                        }

                        // Fires per failed attempt while the prompt stays open. Resuming
                        // here would dismiss the UI after one mistyped digit.
                        override fun onAuthenticationFailed() = Unit
                    }

                val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
                prompt.authenticate(promptInfo())
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            }
        }

    private fun promptInfo(): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo
        .Builder()
        .setTitle("Unlock Kmp")
        .setSubtitle("Use your device screen lock to continue")
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            } else {
                @Suppress("DEPRECATION")
                setDeviceCredentialAllowed(true)
            }
        }
        // No negative button: the API forbids one when a device credential is an
        // accepted authenticator.
        .build()
}
