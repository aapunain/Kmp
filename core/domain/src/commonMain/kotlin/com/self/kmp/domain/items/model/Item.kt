package com.self.kmp.domain.items.model

/**
 * The business model for an item.
 *
 * This is deliberately not the wire model. It has no `@Serializable`, no
 * nullable fields that only exist because the backend might omit them, and no
 * field names dictated by the JSON. The data layer absorbs all of that.
 */
public data class Item(
    val id: String,
    val name: String,
)
