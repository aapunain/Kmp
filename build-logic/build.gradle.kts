plugins {
    `kotlin-dsl`
}

// The Gradle plugins configured by our convention plugins must be on this
// project's compile classpath for `plugins { id("...") }` to resolve inside
// the precompiled script plugins under src/main/kotlin.
dependencies {
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.composeMultiplatform)
    implementation(libs.gradlePlugin.composeCompiler)
}
