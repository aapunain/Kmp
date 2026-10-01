package com.self.kmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.self.kmp.concurrency.di.concurrencyModule
import com.self.kmp.data.di.dataModule
import com.self.kmp.database.di.databaseModule
import com.self.kmp.di.domainModule
import com.self.kmp.feature.auth.di.authModule
import com.self.kmp.feature.auth.presentation.AuthGate
import com.self.kmp.navigation.SampleRoute
import com.self.kmp.navigation.TodoRoute
import com.self.kmp.navigation.backStackFor
import com.self.kmp.network.di.networkModule
import com.self.kmp.presentation.di.presentationModule
import com.self.kmp.presentation.items.ItemsScreen
import com.self.kmp.presentation.todo.TodoScreen
import org.koin.compose.KoinApplication

/**
 * The composition root.
 *
 * Starting Koin from a Composable rather than from a platform lifecycle callback means
 * there is exactly one startup path for Android, iOS, desktop, JS and Wasm.
 *
 * @param deepLink an incoming URL, if the platform delivered one. Each entry point reads
 * it in its own way — an Android intent, an iOS URL scheme, the browser location — and
 * hands the string in here so the parsing itself stays common.
 */
@Composable
@Preview
fun App(deepLink: String? = null) {
    KoinApplication(
        application = {
            modules(
                concurrencyModule(),
                databaseModule(),
                networkModule(),
                dataModule(),
                domainModule(),
                authModule(),
                presentationModule(),
            )
        },
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                // Gates everything behind the device screen lock on Android and iOS.
                // On desktop and web the gate opens immediately: see ADR-0015.
                AuthGate {
                    AppNavDisplay(deepLink = deepLink)
                }
            }
        }
    }
}

@Composable
private fun AppNavDisplay(deepLink: String?) {
    // rememberNavBackStack would persist across process death; a plain remember is
    // enough here because the deep link is re-delivered on a cold start anyway.
    val backStack = androidx.compose.runtime.remember { backStackFor(deepLink).toMutableList() }
    val stack =
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateListOf(*backStack.toTypedArray())
        }

    NavDisplay(
        backStack = stack,
        onBack = { stack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is TodoRoute -> {
                    NavEntry(key) {
                        Column(modifier = Modifier.safeContentPadding()) {
                            Text(
                                text = "Todos on ${getPlatform().name}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                            TodoScreen(onOpenSample = { stack.add(SampleRoute) })
                        }
                    }
                }

                is SampleRoute -> {
                    NavEntry(key) {
                        Column(modifier = Modifier.safeContentPadding()) {
                            Text(
                                text = "Items on ${getPlatform().name}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                            ItemsScreen()
                        }
                    }
                }

                else -> {
                    error("Unknown route: $key")
                }
            }
        },
    )
}
