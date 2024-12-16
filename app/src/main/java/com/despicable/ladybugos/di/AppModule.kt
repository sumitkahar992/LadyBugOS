package com.despicable.ladybugos.di

import com.despicable.ladybugos.MainViewModel
import com.despicable.ladybugos.ui.backup.NativeBackupManager
import com.despicable.ladybugos.ui.backup.NativeBackupViewModel
import com.despicable.ladybugos.ui.screens.trash.TrashViewModel
import com.despicable.widgets.data.CoroutineDispatchers
import com.despicable.widgets.data.DefaultCoroutineDispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {


    single<CoroutineDispatchers> { DefaultCoroutineDispatchers() }



    viewModel { MainViewModel(get()) }
    viewModel { TrashViewModel(get(), get(), get(), get(), get(), get()) }


    single { NativeBackupManager(get(), get()) }
    viewModel { NativeBackupViewModel(get()) }


}




