package com.self.kmp.feature.auth.data

import com.self.kmp.feature.auth.domain.AuthSessionCache

/**
 * Process-scoped, never written to disk.
 *
 * Consequences of that choice, all intended: an Android configuration change keeps the
 * unlocked state because the process survives; process death re-locks; and nothing
 * outside the running app can set the flag.
 *
 * Must be bound as a Koin `single`. A `factory` would hand out a fresh unauthenticated
 * cache on every resolution and re-prompt forever.
 */
internal class InMemoryAuthSessionCache : AuthSessionCache {
    private var authenticated = false

    override fun isAuthenticated(): Boolean = authenticated

    override fun markAuthenticated() {
        authenticated = true
    }
}
