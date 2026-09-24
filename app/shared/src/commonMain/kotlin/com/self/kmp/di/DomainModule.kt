package com.self.kmp.di

import com.self.kmp.domain.items.usecase.GetItemsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Bindings for the domain layer's use cases.
 *
 * Note the location: this is in :app:shared, not in :core:domain. It lives here
 * so that :core:domain depends on nothing at all, Koin included. If you came
 * looking for this file inside the domain module, that is why it is not there.
 *
 * Why the domain layer can be wired from outside while :core:data, :core:network
 * and :presentation cannot: those three keep their implementations `internal`, so
 * no other module is able to name the classes that need constructing and they
 * must publish their own wiring. Use cases are public API of :core:domain, so the
 * composition root can assemble them directly. Ownership of wiring follows
 * visibility.
 */
fun domainModule(): Module = module {
    factory { GetItemsUseCase(itemsRepository = get()) }
}
