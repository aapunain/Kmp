package com.self.kmp.feature.auth.di

import android.content.Context
import androidx.startup.Initializer
import com.self.kmp.feature.auth.data.AndroidAuthRepository
import com.self.kmp.feature.auth.domain.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Captures the Application Context so that [AndroidAuthRepository] can be constructed
 * by Koin.
 *
 * Koin is started from a Composable in common code (ADR-0009), so it has no Android
 * Context to hand out. `androidx.startup` runs this before `Application.onCreate`
 * completes, without needing an Application subclass or a manifest edit.
 *
 * This is process-scoped and holds only the Application Context, so unlike an Activity
 * reference it cannot leak and cannot go stale.
 */
internal object AuthContextHolder {
    @Volatile
    internal var applicationContext: Context? = null
}

public class AuthContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AuthContextHolder.applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

internal actual fun platformAuthModule(): Module = module {
    single<AuthRepository> {
        val context =
            requireNotNull(AuthContextHolder.applicationContext) {
                "AuthContextInitializer did not run. Check that androidx.startup's " +
                    "InitializationProvider is present in the merged manifest."
            }
        AndroidAuthRepository(context)
    }
}
