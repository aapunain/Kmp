package com.self.kmp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity

/**
 * Extends `FragmentActivity` rather than `ComponentActivity` because `BiometricPrompt`
 * requires one. `FragmentActivity` is a `ComponentActivity`, so `setContent` and
 * edge-to-edge work unchanged.
 *
 * Nothing else is needed: the auth prompt reads this Activity from the composition via
 * `LocalContext`, so there is no registry to populate and no lifecycle callback to pair.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
