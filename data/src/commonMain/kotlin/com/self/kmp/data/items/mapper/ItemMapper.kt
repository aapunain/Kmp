package com.self.kmp.data.items.mapper

import com.self.kmp.domain.items.model.Item
import com.self.kmp.network.items.dto.ItemDto

/**
 * DTO to domain. Returns null for records the domain cannot represent, so the
 * caller drops them instead of inventing data or crashing.
 */
internal fun ItemDto.toDomainOrNull(): Item? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    return Item(
        id = id,
        name = name.orEmpty(),
    )
}
