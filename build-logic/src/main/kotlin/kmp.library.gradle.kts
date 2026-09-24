import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention plugin for every non-UI multiplatform library module.
 *
 * It fixes the target list in one place so that :domain, :data, :network and
 * :concurrency can never drift apart from :app:shared. Drift in the target list
 * is the most common cause of "no matching variant" resolution failures in KMP.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

// The generated `libs` accessor is not available inside precompiled script
// plugins, so the catalog is looked up through the extension instead.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
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
        // :domain -> com.self.kmp.domain, :network -> com.self.kmp.network, ...
        namespace = "com.self.kmp." + project.name
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

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
