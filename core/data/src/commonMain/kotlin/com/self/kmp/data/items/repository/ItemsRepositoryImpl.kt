package com.self.kmp.data.items.repository

import com.self.kmp.concurrency.DispatcherProvider
import com.self.kmp.data.common.toAppError
import com.self.kmp.data.items.mapper.toDomainOrNull
import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.items.model.Item
import com.self.kmp.domain.items.repository.ItemsRepository
import com.self.kmp.network.core.ApiResult
import com.self.kmp.network.items.datasource.ItemsRemoteDataSource
import kotlinx.coroutines.withContext

/**
 * `internal`, so nothing outside :data can reference it. The composition root
 * wires it through [com.self.kmp.data.di.dataModule] and only ever sees the
 * [ItemsRepository] interface that :domain owns.
 *
 * This class is where the two translations happen: DTO to domain model, and
 * ApiError to AppError. Both directions of the boundary, in one place.
 */
internal class ItemsRepositoryImpl(
    private val itemsRemoteDataSource: ItemsRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : ItemsRepository {
    override suspend fun getItems(): AppResult<List<Item>> = withContext(dispatchers.io) {
        // Ktor already suspends without blocking, so this withContext is not
        // strictly required for the call itself. It is here because the mapping
        // below is our work, not Ktor's, and because an injected dispatcher is
        // what makes this class testable without touching a real thread pool.
        when (val response = itemsRemoteDataSource.getItems()) {
            is ApiResult.Success -> {
                AppResult.Success(
                    response.data.items
                        .orEmpty()
                        .mapNotNull { it.toDomainOrNull() },
                )
            }

            is ApiResult.Failure -> {
                AppResult.Failure(response.error.toAppError())
            }
        }
    }
}
