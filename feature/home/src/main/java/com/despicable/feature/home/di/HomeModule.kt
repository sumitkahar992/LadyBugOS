package com.despicable.feature.home.di

import com.despicable.feature.home.NoteListViewModel
import com.despicable.feature.home.screens.trash.TrashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel {
        NoteListViewModel(
            get(),
            get(),
            get(),
            get(),
        )
    }

    viewModel { TrashViewModel(get(), get(), get()) }

}