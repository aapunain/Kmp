package com.self.kmp.data.di

import com.self.kmp.data.items.repository.ItemsRepositoryImpl
import com.self.kmp.domain.items.repository.ItemsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The binding of a domain interface to a data implementation happens here, which
 * is the only reason :app:shared does not need to see ItemsRepositoryImpl.
 */
public fun dataModule(): Module = module {
    single<ItemsRepository> {
        ItemsRepositoryImpl(
            itemsRemoteDataSource = get(),
            dispatchers = get(),
        )
    }
}
