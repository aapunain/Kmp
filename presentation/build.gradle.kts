plugins {
    id("kmp.library.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // :network is deliberately absent. ApiResult and ItemDto are not on
            // this module's compile classpath, so the layering is enforced by
            // the compiler rather than by code review.
            implementation(project(":domain"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
