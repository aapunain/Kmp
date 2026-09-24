plugins {
    id("kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Koin is the only dependency here, and only so that the module can
            // publish its own DI wiring. There is deliberately no coroutines
            // dependency: `suspend` is a language feature, not a library one.
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
