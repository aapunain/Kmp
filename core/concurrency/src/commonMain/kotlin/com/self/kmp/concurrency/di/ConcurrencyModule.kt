package com.self.kmp.concurrency.di

import com.self.kmp.concurrency.DefaultDispatcherProvider
import com.self.kmp.concurrency.DispatcherProvider
import org.koin.core.module.Module
import org.koin.dsl.module

public fun concurrencyModule(): Module = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}
