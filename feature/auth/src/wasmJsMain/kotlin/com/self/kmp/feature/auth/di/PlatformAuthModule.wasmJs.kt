package com.self.kmp.feature.auth.di

import com.self.kmp.feature.auth.data.UnsupportedAuthRepository
import com.self.kmp.feature.auth.domain.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

// Same as Kotlin/JS: WebAuthn is the browser story, and it is a different problem.
internal actual fun platformAuthModule(): Module = module {
    single<AuthRepository> { UnsupportedAuthRepository() }
}
