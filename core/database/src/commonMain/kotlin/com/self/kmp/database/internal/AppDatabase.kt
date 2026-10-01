package com.self.kmp.database.internal

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Room requires every entity to be declared on one `@Database` class, so this module
 * inevitably knows about every table in the app. That is inherent to Room, not a
 * layering mistake — see ADR-0016.
 *
 * `exportSchema = false` because the Room Gradle plugin (`androidx.room3`) that provides
 * `room.schemaLocation` is only published as 3.1.0-alpha01. Turn it on and add the plugin
 * when migration tests are needed.
 */
@Database(entities = [TodoEntity::class], version = 1, exportSchema = false)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}

/**
 * Room generates the `actual` for this on every target. The `expect object` is the
 * mechanism Room KMP uses in place of reflection.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_IRRELEVANT_VISIBILITY")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
