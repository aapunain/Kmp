package com.self.kmp.domain.di

import com.self.kmp.domain.items.usecase.GetItemsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Every module publishes its own wiring. The composition root in :app:shared
 * then only has to list these functions, and never needs to see an
 * implementation class.
 */
fun domainModule(): Module = module {
    factory { GetItemsUseCase(itemsRepository = get()) }
}
