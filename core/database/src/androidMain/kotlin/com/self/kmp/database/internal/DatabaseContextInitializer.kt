package com.self.kmp.database.internal

import android.content.Context
import androidx.startup.Initializer

/**
 * Supplies the Application Context that Room needs for its file path.
 *
 * Koin is started from a Composable in common code (ADR-0009), so it has no Android
 * Context to hand out. `androidx.startup` runs this during application startup without
 * requiring an Application subclass.
 *
 * Note: `:feature:auth` has an equivalent holder for the same reason. Two copies of
 * ~15 lines is cheaper than a module that exists only to share them; extract a
 * `:core:appcontext` if a third consumer appears.
 */
internal object DatabaseContextHolder {
    @Volatile
    internal var applicationContext: Context? = null
}

public class DatabaseContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        DatabaseContextHolder.applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
