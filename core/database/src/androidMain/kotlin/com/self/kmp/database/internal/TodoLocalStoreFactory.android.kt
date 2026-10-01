package com.self.kmp.database.internal

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.self.kmp.database.TodoLocalStore

internal actual fun createTodoLocalStore(): TodoLocalStore {
    val context =
        requireNotNull(DatabaseContextHolder.applicationContext) {
            "DatabaseContextInitializer did not run. Check that androidx.startup's " +
                "InitializationProvider is present in the merged manifest."
        }

    val database =
        Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = context.getDatabasePath(DATABASE_NAME).absolutePath,
            ).setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    return RoomTodoLocalStore(database.todoDao())
}
