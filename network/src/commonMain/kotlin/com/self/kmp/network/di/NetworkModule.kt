package com.self.kmp.network.di

import com.self.kmp.network.core.createHttpClient
import com.self.kmp.network.core.createMockEngine
import com.self.kmp.network.items.datasource.ItemsRemoteDataSource
import com.self.kmp.network.items.datasource.KtorItemsRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Everything Ktor-shaped is created and kept inside this module. Consumers only
 * ever resolve [ItemsRemoteDataSource]; the HttpClient is not part of the
 * module's public API.
 */
fun networkModule(): Module = module {
    single<HttpClientEngine> { createMockEngine() }
    single<HttpClient> { createHttpClient(engine = get()) }
    single<ItemsRemoteDataSource> { KtorItemsRemoteDataSource(httpClient = get()) }
}
