plugins {
    id("kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Ktor stays an implementation detail: no Ktor type appears in this
            // module's public API, so it is not exposed to :data.
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.serializationKotlinxJson)
            implementation(libs.koin.core)

            // `api` only because the public DTOs carry @Serializable, and a
            // consumer compiling against them needs that annotation on its path.
            api(libs.kotlinx.serialization.json)

            // The engine for this sample. MockEngine is multiplatform, so the
            // exact same code runs on Android, iOS, JVM, JS and Wasm with no
            // per-platform engine wiring. Swapping in real engines means one
            // `expect fun` plus the OkHttp/Darwin/CIO/Js artifacts.
            implementation(libs.ktor.clientMock)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
