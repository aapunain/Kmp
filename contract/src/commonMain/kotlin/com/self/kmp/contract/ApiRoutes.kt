package com.self.kmp.contract

/**
 * Endpoint paths, declared once and used by both sides.
 *
 * The server registers its route from these constants and the client builds its
 * request from them, so a path can no longer drift between the two.
 */
public object ApiRoutes {
    /** Collection of items. GET returns [com.self.kmp.contract.items.ItemsResponseDto]. */
    public const val ITEMS: String = "items"
}
