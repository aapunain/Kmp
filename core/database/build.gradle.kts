plugins {
    id("kmp.library")
    alias(libs.plugins.ksp)
}

kotlin {
    // Room generates an `expect object` for the database constructor, and expect/actual
    // classes are still Beta in Kotlin. Without this flag every build warns about
    // generated code we do not control.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }

        // sqlite-bundled has no js/wasm variant, so the driver dependency is split.
        // JS and Wasm fall back to an in-memory store; see TodoLocalStoreFactory.
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            // Supplies the Application Context that Room needs for its file path.
            implementation(libs.androidx.startup.runtime)
        }
        jvmMain.dependencies { implementation(libs.androidx.sqlite.bundled) }
        iosMain.dependencies { implementation(libs.androidx.sqlite.bundled) }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        // RoomTodoLocalStoreTest runs the generated Room code against a real in-memory
        // SQLite database. Declared explicitly rather than relying on the main
        // compilation's `implementation` leaking onto the test classpath.
        jvmTest.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

// Room's KSP processor has to be registered per target; there is no single
// `ksp(...)` configuration for a multiplatform project.
dependencies {
    listOf(
        "kspAndroid",
        "kspJvm",
        "kspIosArm64",
        "kspIosSimulatorArm64",
        "kspJs",
        "kspWasmJs",
    ).forEach { configuration ->
        add(configuration, libs.room.compiler)
    }
}
