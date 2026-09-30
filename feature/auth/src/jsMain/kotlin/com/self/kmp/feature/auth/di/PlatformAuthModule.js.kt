package com.self.kmp.feature.auth.di

import com.self.kmp.feature.auth.data.UnsupportedAuthRepository
import com.self.kmp.feature.auth.domain.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

// The browser equivalent is WebAuthn, which is server-verified authentication rather
// than a local gate, so it belongs with backend sign-in and not with this app lock.
internal actual fun platformAuthModule(): Module = module {
    single<AuthRepository> { UnsupportedAuthRepository() }
}
