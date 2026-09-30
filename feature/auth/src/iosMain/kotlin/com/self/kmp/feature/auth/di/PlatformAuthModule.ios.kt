package com.self.kmp.feature.auth.di

import com.self.kmp.feature.auth.data.IosAuthRepository
import com.self.kmp.feature.auth.domain.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformAuthModule(): Module = module {
    single<AuthRepository> { IosAuthRepository() }
}
