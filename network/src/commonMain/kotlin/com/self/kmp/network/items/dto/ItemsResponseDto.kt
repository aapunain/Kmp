package com.self.kmp.network.items.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape, and only the wire shape.
 *
 * Everything is nullable with a default because the backend is not under our
 * control and a missing field must not crash the app. Absorbing that
 * sloppiness here is exactly why the domain model can be strict.
 */
@Serializable
data class ItemsResponseDto(
    @SerialName("items") val items: List<ItemDto>? = null,
)

@Serializable
data class ItemDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
)
