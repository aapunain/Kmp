package com.self.kmp.feature.auth.di

import com.self.kmp.feature.auth.data.UnsupportedAuthRepository
import com.self.kmp.feature.auth.domain.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

// Desktop has no cross-platform device authentication API. macOS could bridge to
// LocalAuthentication and Windows to Hello through native interop; neither is in scope.
internal actual fun platformAuthModule(): Module = module {
    single<AuthRepository> { UnsupportedAuthRepository() }
}
