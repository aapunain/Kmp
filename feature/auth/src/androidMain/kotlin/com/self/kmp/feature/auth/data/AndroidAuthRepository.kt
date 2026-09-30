package com.self.kmp.feature.auth.data

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.self.kmp.feature.auth.domain.AuthRepository
import com.self.kmp.feature.auth.domain.DeviceAuthAvailability

/**
 * Capability query only. Needs a `Context`, not an Activity, which is why it can be an
 * ordinary injected dependency.
 *
 * `minSdk` is 24 and the API levels differ: combining
 * `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is only supported from API 30, and below that
 * `BiometricManager.canAuthenticate` cannot report on device credentials at all. Hence
 * the `KeyguardManager` fallback, which is what lets a PIN-only device count as
 * available.
 */
internal class AndroidAuthRepository(
    private val context: Context,
) : AuthRepository {
    override suspend fun availability(): DeviceAuthAvailability =
        when (BiometricManager.from(context).canAuthenticate(allowedAuthenticators())) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                DeviceAuthAvailability.AVAILABLE
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (hasDeviceCredential()) {
                    DeviceAuthAvailability.AVAILABLE
                } else {
                    DeviceAuthAvailability.NOT_ENROLLED
                }
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> {
                if (hasDeviceCredential()) {
                    DeviceAuthAvailability.AVAILABLE
                } else {
                    DeviceAuthAvailability.NO_HARDWARE
                }
            }

            else -> {
                DeviceAuthAvailability.NOT_SUPPORTED
            }
        }

    /** A PIN, pattern or password counts even with no biometric enrolled. */
    private fun hasDeviceCredential(): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguard?.isDeviceSecure == true
    }

    private fun allowedAuthenticators(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    } else {
        BIOMETRIC_WEAK
    }
}
