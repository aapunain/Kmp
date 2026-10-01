package com.self.kmp.database.internal

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.self.kmp.database.TodoLocalStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun createTodoLocalStore(): TodoLocalStore {
    val documents: NSURL =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("Could not resolve the iOS documents directory")

    val path =
        requireNotNull(documents.URLByAppendingPathComponent(DATABASE_NAME)?.path) {
            "Could not build the database path"
        }

    val database =
        Room
            .databaseBuilder<AppDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    return RoomTodoLocalStore(database.todoDao())
}
