package com.despicable.widgets.di

import com.despicable.widgets.data.NoteWidgetRepository
import com.despicable.widgets.data.WidgetUpdater
import com.despicable.widgets.ui.NoteSelectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val widgetModule = module {

    viewModel { NoteSelectionViewModel(get(), get(), get()) }

    single { NoteWidgetRepository(get(), get(), get(), get()) }

    factory { WidgetUpdater(get()) }

}