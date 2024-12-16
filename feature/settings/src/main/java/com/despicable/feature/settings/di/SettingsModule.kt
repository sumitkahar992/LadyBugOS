package com.despicable.feature.settings.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { com.despicable.feature.settings.SettingsViewModel(get()) }
}