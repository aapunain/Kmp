package com.self.kmp.database.internal

import com.self.kmp.database.TodoLocalStore

internal const val DATABASE_NAME: String = "kmp.db"

/**
 * The seam sits at the *store*, not at the Room builder, because not every platform
 * persists the same way.
 *
 * Android, iOS and desktop return a Room-backed store using `sqlite-bundled`. JS and
 * Wasm return an in-memory store: `androidx.sqlite:sqlite-web` does exist, but its
 * `WebWorkerSQLiteDriver` requires a JS `Worker`, which means an npm dependency on
 * `@sqlite.org/sqlite-wasm`, a worker entry script, webpack wiring, and COOP/COEP
 * response headers for OPFS. That is its own piece of work; see ADR-0016.
 *
 * Because the port is the same, nothing above this module knows which it got.
 */
internal expect fun createTodoLocalStore(): TodoLocalStore
