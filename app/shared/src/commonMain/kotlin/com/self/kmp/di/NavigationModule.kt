package com.self.kmp.di

import com.self.kmp.navigation.NavigationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Navigation lives in the composition root (ADR-0017), so its wiring does too.
 *
 * The deep link arrives as a Koin parameter because only the platform entry point knows
 * it. Koin only consults the parameter when the ViewModel is first created; on an
 * Android configuration change the existing instance is returned and the re-delivered
 * link is ignored, which is the point.
 */
fun navigationModule(): Module = module {
    viewModel { params -> NavigationViewModel(deepLink = params.getOrNull()) }
}
