plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "com.self.kmp"
version = "1.0.0"
application {
    mainClass = "com.self.kmp.ApplicationKt"
}

dependencies {
    // The same wire models the clients compile against. A JVM-only module can
    // consume a multiplatform library: Gradle resolves the `jvm` variant.
    implementation(project(":contract"))

    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientContentNegotiation)
    testImplementation(libs.kotlin.testJunit)
}
