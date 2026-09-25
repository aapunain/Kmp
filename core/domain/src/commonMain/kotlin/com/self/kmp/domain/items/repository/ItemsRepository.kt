package com.self.kmp.domain.items.repository

import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.items.model.Item

/**
 * The port the domain needs in order to do its job.
 *
 * It is declared here, in the layer that consumes it, and implemented in
 * :core:data. That inversion is what lets :core:domain depend on nothing while
 * still being able to reach a network.
 */
public interface ItemsRepository {
    public suspend fun getItems(): AppResult<List<Item>>
}
