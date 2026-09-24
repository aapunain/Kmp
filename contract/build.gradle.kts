plugins {
    id("kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

/**
 * The HTTP contract shared by every client and the server.
 *
 * Charter: `@Serializable` request/response models and endpoint path constants.
 * No logic, no platform code, no dependency beyond kotlinx-serialization. If a
 * type in here is not part of the wire format, it is in the wrong module.
 *
 * The payoff is that renaming a field breaks the compile on both sides at once
 * instead of failing at runtime against a stale client.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: consumers reference these models
            // directly and need @Serializable on their compile classpath.
            api(libs.kotlinx.serialization.json)
        }
    }
}
