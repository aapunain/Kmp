package com.self.kmp.feature.auth.data

import com.self.kmp.feature.auth.domain.AuthRepository
import com.self.kmp.feature.auth.domain.DeviceAuthAvailability
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

/**
 * `LAPolicyDeviceOwnerAuthentication` is the "whatever the user set" policy: Face ID or
 * Touch ID when enrolled, falling back to the passcode. The narrower
 * `deviceOwnerAuthenticationWithBiometrics` would refuse users who have a passcode but
 * no biometric enrolled.
 *
 * `canEvaluatePolicy` reports why it failed through an `NSError` out-parameter, which is
 * what the cinterop allocation below is for. `memScoped` frees it on exit.
 */
internal class IosAuthRepository : AuthRepository {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun availability(): DeviceAuthAvailability = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val canEvaluate = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error.ptr)

        if (canEvaluate) {
            DeviceAuthAvailability.AVAILABLE
        } else {
            when (error.value?.code) {
                LAErrorPasscodeNotSet,
                LAErrorBiometryNotEnrolled,
                -> DeviceAuthAvailability.NOT_ENROLLED

                LAErrorBiometryNotAvailable -> DeviceAuthAvailability.NO_HARDWARE

                else -> DeviceAuthAvailability.NOT_SUPPORTED
            }
        }
    }
}
