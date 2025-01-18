package com.despicable.feature.detail.di

import com.despicable.core.data.di.dataModule
import com.despicable.core.domain.di.domainModule
import com.despicable.feature.detail.NoteDetailViewModel
import com.despicable.feature.detail.checklist.PreviewChecklistViewModel
import com.despicable.feature.detail.checklist.PreviewChecklistViewModel2
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val detailKoinModule = module {
    includes(domainModule, dataModule)
    viewModel { NoteDetailViewModel(get(), get(), get(), get()) }

    viewModel { PreviewChecklistViewModel()  }
    viewModel { PreviewChecklistViewModel2()  }

}