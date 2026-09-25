package com.self.kmp.presentation.di

import com.self.kmp.presentation.items.ItemsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public fun presentationModule(): Module = module {
    viewModel { ItemsViewModel(getItems = get()) }
}
