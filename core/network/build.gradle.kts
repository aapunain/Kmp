plugins {
    id("kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: the wire models appear in the public
            // signature of ItemsRemoteDataSource, so :core:data needs to see them.
            // This module owns the wire *mechanics*; :contract owns the wire *shape*.
            api(project(":contract"))

            // Ktor stays an implementation detail: no Ktor type appears in this
            // module's public API, so it is not exposed to :core:data.
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.serializationKotlinxJson)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)

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
