package com.self.kmp.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.self.kmp.feature.auth.domain.DeviceAuthPrompt

@Composable
internal actual fun rememberDeviceAuthPrompt(): DeviceAuthPrompt = remember { UnsupportedDeviceAuthPrompt() }
