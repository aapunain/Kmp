import org.gradle.internal.os.OperatingSystem

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.androidLint) apply false

    // Quality gates live at the root so that a newly created module is covered
    // automatically instead of depending on someone remembering to opt in.
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)

    // Executable architecture rules. See build-logic/src/main/kotlin/kmp.architecture.gradle.kts
    id("kmp.architecture")
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

// Coverage is measured on the JVM target. For code that lives in commonMain that
// is representative, and it is the only place JaCoCo-style instrumentation works.
// Treat the floor as a ratchet against regression, not as a quality score.
dependencies {
    kover(project(":contract"))
    kover(project(":core:concurrency"))
    kover(project(":core:data"))
    kover(project(":core:domain"))
    kover(project(":core:network"))
    kover(project(":presentation"))
}

// Static analysis. Runs from the root over every source directory so that a new
// module is covered without needing to opt in.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    // Matching on `**/src/**` rather than listing module directories means a new
    // module is analysed automatically, and generated code under build/ is never
    // picked up. Analysing generated Compose resource accessors produces findings
    // nobody can act on.
    source.setFrom(
        fileTree(layout.projectDirectory) {
            include("**/src/**/*.kt", "**/src/**/*.kts")
            exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
        },
    )
}

kover {
    reports {
        filters {
            excludes {
                // Composables are exercised by UI tests, not unit tests, and DI
                // wiring is already verified end to end by CompositionRootTest.
                // Counting them would only make the number harder to interpret.
                annotatedBy("androidx.compose.runtime.Composable")
                classes("*.di.*")
            }
        }
        verify {
            // A ratchet, not a target. Measured baseline when this was introduced
            // was 81.6% line coverage with the filters above applied. The floor
            // sits below that so a real regression fails the build, without an
            // arbitrary number becoming a goal in itself. Raise it as it climbs.
            rule {
                minBound(75)
            }
        }
    }
}

/** Modules built with the kmp.library convention plugin, plus the shared app module. */
val multiplatformModules =
    listOf(
        ":contract",
        ":core:concurrency",
        ":core:data",
        ":core:domain",
        ":core:network",
        ":presentation",
        ":app:shared",
    )

/** Test tasks that run on any host. */
val hostAgnosticTestTasks = listOf("jvmTest", "testAndroidHostTest", "jsTest", "wasmJsTest")

val qualityCheck =
    tasks.register("qualityCheck") {
        group = "verification"
        description = "Formatting, static analysis, architecture and conventions. Fast; no tests."
        dependsOn("spotlessCheck")
        dependsOn("detekt")
        dependsOn("architectureCheck")
        dependsOn("architectureDocsCheck")
        dependsOn("conventionsCheck")
    }

tasks.register("verify") {
    group = "verification"
    description = "The single command that means 'safe to commit'. Runs every gate on every target."

    dependsOn(qualityCheck)

    multiplatformModules.forEach { module ->
        hostAgnosticTestTasks.forEach { task -> dependsOn("$module:$task") }
    }

    dependsOn(":server:test")

    // Coverage is measured on the JVM target only; see the kover block above.
    dependsOn("koverVerify")

    // Proof that each platform entry point still builds, not just that the
    // libraries compile.
    dependsOn(":app:androidApp:assembleDebug")
    dependsOn(":app:desktopApp:compileKotlin")
    dependsOn(":app:webApp:jsBrowserDistribution")
    dependsOn(":app:webApp:wasmJsBrowserDistribution")

    // Apple targets need Xcode, so they are host-gated rather than skipped
    // silently. CI runs a macOS job to cover them.
    if (OperatingSystem.current().isMacOsX) {
        multiplatformModules.forEach { module -> dependsOn("$module:iosSimulatorArm64Test") }
        dependsOn(":app:shared:linkDebugFrameworkIosArm64")
    } else {
        doFirst {
            logger.lifecycle(
                "Skipping Apple targets: not running on macOS. CI covers them in the macos job.",
            )
        }
    }
}
