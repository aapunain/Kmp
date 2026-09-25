import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Convention plugin for every non-UI multiplatform library module.
//
// It fixes the target list in one place so that :core:domain, :core:data,
// :core:network and :core:concurrency can never drift apart from :app:shared.
// Drift in the target list is the most common cause of "no matching variant"
// resolution failures in KMP.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlinx.kover")
}

// The generated `libs` accessor is not available inside precompiled script
// plugins, so the catalog is looked up through the extension instead.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    // Every public declaration must state its visibility and return type
    // explicitly, and this is an error rather than a warning.
    //
    // This is the rule that keeps `internal` implementations actually internal:
    // without it, a dropped modifier silently widens the module's public API and
    // nothing fails. With it, the compiler catches it before review does.
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        // Derived from the full Gradle path, not the leaf name: Android
        // namespaces must be unique across every module in an app, and leaf
        // names repeat once features arrive.
        //   :core:domain          -> com.self.kmp.core.domain
        //   :feature:items:domain -> com.self.kmp.feature.items.domain
        namespace = "com.self.kmp" + project.path.replace(":", ".")
        compileSdk =
            libs
                .findVersion("android-compileSdk")
                .get()
                .requiredVersion
                .toInt()
        minSdk =
            libs
                .findVersion("android-minSdk")
                .get()
                .requiredVersion
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest { }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
        }
    }
}
