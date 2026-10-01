package com.self.kmp.database.internal

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.self.kmp.database.TodoLocalStore
import java.io.File

internal actual fun createTodoLocalStore(): TodoLocalStore {
    // A per-user application directory rather than the working directory, so running
    // from an IDE and from a packaged build share one database.
    val directory = File(System.getProperty("user.home"), ".kmp").apply { mkdirs() }

    val database =
        Room
            .databaseBuilder<AppDatabase>(
                name = File(directory, DATABASE_NAME).absolutePath,
            ).setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    return RoomTodoLocalStore(database.todoDao())
}
