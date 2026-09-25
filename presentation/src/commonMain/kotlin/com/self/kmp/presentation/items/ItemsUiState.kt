package com.self.kmp.presentation.items

/**
 * One immutable snapshot describing everything the screen needs to draw itself.
 *
 * The list is `List<String>` rather than `List<Item>` on purpose: these are
 * display-ready values. Formatting decisions belong to this layer, so the domain
 * model never has to grow fields that only exist to satisfy a layout.
 */
public data class ItemsUiState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    public val isEmpty: Boolean get() = !isLoading && errorMessage == null && items.isEmpty()
}
