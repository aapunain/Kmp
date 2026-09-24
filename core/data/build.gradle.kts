plugins {
    id("kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Note what is not here: no platform source sets exist in this module
            // at all. Anything platform specific arrives through an injected
            // interface, which is why :data is 100% common code.
            implementation(project(":core:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:concurrency"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
