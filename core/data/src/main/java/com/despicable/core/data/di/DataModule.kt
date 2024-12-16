package com.despicable.core.data.di

import com.despicable.core.data.repository.NoteRepositoryImpl
import com.despicable.core.data.repository.NoteRepositoryInterface
import org.koin.dsl.module


val dataModule = module {
    // Use Cases
    single<NoteRepositoryInterface> { NoteRepositoryImpl(get(), get(), get()) }
}