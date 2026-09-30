package com.self.kmp.feature.auth.presentation

import androidx.compose.runtime.Composable
import com.self.kmp.feature.auth.domain.DeviceAuthPrompt

/**
 * Supplies the prompt from the composition rather than from DI.
 *
 * This is the whole reason the design looks like this. On Android `BiometricPrompt`
 * requires a `FragmentActivity`, and an Activity is UI-scoped. Resolving it here means
 * its lifetime is the composition's, so nothing long-lived holds a reference to it and
 * there is no global registry to keep in sync.
 */
@Composable
internal expect fun rememberDeviceAuthPrompt(): DeviceAuthPrompt
