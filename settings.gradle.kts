rootProject.name = "Kmp"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Platform entry points
include(":app:androidApp")
include(":app:desktopApp")
include(":app:shared")
include(":app:webApp")

// Architecture layers.
// `core` is a grouping directory, not a module: there is no build.gradle.kts at
// core/ and no :core project. Each child owns a precise charter.
//
// Dependency direction is strictly inward:
//   :presentation      -> :core:domain
//   :core:data         -> :core:domain, :core:network, :core:concurrency
//   :core:network      -> :contract
//   :core:domain       -> (nothing)
//   :core:concurrency  -> (nothing)
include(":presentation")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:concurrency")

// The HTTP contract shared by the clients and the server. Serializable wire
// models and endpoint paths only, so a change to the wire format is a compile
// error on both sides.
include(":contract")

include(":server")
