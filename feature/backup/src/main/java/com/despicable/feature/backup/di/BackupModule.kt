package com.despicable.feature.backup.di

import com.despicable.feature.backup.BackupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val backupModule = module {

    viewModel { BackupViewModel(get()) }
}
