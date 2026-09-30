package com.self.kmp.feature.auth.di

import com.self.kmp.feature.auth.data.InMemoryAuthSessionCache
import com.self.kmp.feature.auth.domain.AuthSessionCache
import com.self.kmp.feature.auth.domain.MarkAuthenticatedUseCase
import com.self.kmp.feature.auth.domain.ShouldRunAuthUseCase
import com.self.kmp.feature.auth.presentation.AuthGateViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Platform-specific bindings, principally [com.self.kmp.feature.auth.domain.AuthRepository],
 * whose Android implementation needs a `Context`.
 */
internal expect fun platformAuthModule(): Module

/** The feature's only public surface. */
public fun authModule(): Module = module {
    includes(platformAuthModule())

    // `single`, not `factory`: a fresh cache per resolution would always report
    // "not authenticated" and re-prompt forever.
    single<AuthSessionCache> { InMemoryAuthSessionCache() }

    factory { ShouldRunAuthUseCase(authRepository = get(), authSessionCache = get()) }
    factory { MarkAuthenticatedUseCase(authSessionCache = get()) }

    viewModel { AuthGateViewModel(shouldRunAuth = get(), markAuthenticated = get()) }
}
