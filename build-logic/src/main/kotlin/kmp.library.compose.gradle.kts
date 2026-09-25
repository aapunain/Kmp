import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Convention plugin for multiplatform library modules that contain Compose UI.
// Only :presentation uses this today.
plugins {
    id("kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<KotlinMultiplatformExtension> {
    // The Compose plugin refuses to run web tests in a module that has no
    // webpack bundle, because the Skiko runtime would have nowhere to load from.
    // Declaring executable binaries satisfies that check and keeps the door open
    // for Compose UI tests on the web targets later.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
    }
    js {
        binaries.executable()
    }
}
