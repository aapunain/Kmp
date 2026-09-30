plugins {
    id("kmp.library.compose")
}

// Charter: device authentication, end to end. Anything not about authentication does
// not belong here.
//
// This is a *feature* module, not a layer: it contains its own domain, data,
// presentation and di packages. The rule for choosing is in ADR-0002 —
// core/<capability> for cross-cutting infrastructure with no UI, feature/<name> for a
// self-contained user-facing capability.
//
// Layers are packages rather than sub-modules deliberately. `internal` plus
// explicitApi() gives the same enforcement at a quarter of the Gradle; split it up if
// it ever grows enough to hurt.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.biometric)
            // Supplies the Application Context to AuthRepository without an
            // Application subclass or a manifest edit.
            implementation(libs.androidx.startup.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
