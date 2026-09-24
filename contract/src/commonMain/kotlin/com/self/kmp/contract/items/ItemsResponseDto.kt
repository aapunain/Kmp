package com.self.kmp.contract.items

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape for `GET /items`, and only the wire shape.
 *
 * Everything is nullable with a default so that an older client stays
 * functional against a newer server, and so a missing field can never crash the
 * app. Absorbing that tolerance here is precisely why the domain model in
 * :core:domain is allowed to be strict.
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
