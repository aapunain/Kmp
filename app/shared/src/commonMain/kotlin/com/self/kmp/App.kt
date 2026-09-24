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
import com.self.kmp.concurrency.di.concurrencyModule
import com.self.kmp.data.di.dataModule
import com.self.kmp.domain.di.domainModule
import com.self.kmp.network.di.networkModule
import com.self.kmp.presentation.di.presentationModule
import com.self.kmp.presentation.items.ItemsScreen
import org.koin.compose.KoinApplication

/**
 * The composition root.
 *
 * Starting Koin from a Composable rather than from a platform lifecycle callback
 * means there is exactly one startup path for Android, iOS, desktop, JS and Wasm.
 * No `Application.onCreate`, no `iOSApp.init`, no duplication in `main()`.
 */
@Composable
@Preview
fun App() {
    KoinApplication(
        application = {
            modules(
                concurrencyModule(),
                networkModule(),
                domainModule(),
                dataModule(),
                presentationModule(),
            )
        },
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.safeContentPadding()) {
                    Text(
                        text = "Items on ${Greeting().greet()}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    ItemsScreen()
                }
            }
        }
    }
}
