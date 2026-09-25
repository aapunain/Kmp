package com.self.kmp.domain.items.usecase

import com.self.kmp.domain.common.AppResult
import com.self.kmp.domain.common.map
import com.self.kmp.domain.items.model.Item
import com.self.kmp.domain.items.repository.ItemsRepository

/**
 * Business rules for reading the item list:
 * items without a usable name are not shown, and the list is presented
 * alphabetically regardless of the order the backend happened to return.
 *
 * Keeping these rules here rather than in the ViewModel is the point of the
 * layer. They are testable without Compose, without Ktor, and without a device.
 */
public class GetItemsUseCase(
    private val itemsRepository: ItemsRepository,
) {
    public suspend operator fun invoke(): AppResult<List<Item>> = itemsRepository.getItems().map { items ->
        items
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }
}
