package com.despicable.core.data.di

import com.despicable.core.data.repository.BackupRepository
import com.despicable.core.data.repository.BackupRepositoryImpl
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.data.repository.NoteRepositoryImpl
import org.koin.dsl.module


val dataModule = module {
    // Use Cases
    single<NoteRepository> { NoteRepositoryImpl(get(), get(), get()) }

    single<BackupRepository> { BackupRepositoryImpl(get(), get(), get(), get()) }

}

