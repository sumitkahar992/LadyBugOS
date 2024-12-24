package com.despicable.core.domain.di

import com.despicable.core.data.repository.ReminderScheduler
import com.despicable.core.data.sample.AssetLoader
import com.despicable.core.data.sample.AssetLoaderImpl
import com.despicable.core.data.sample.LoadSampleDataUseCase
import com.despicable.core.domain.usecase.NotificationReminderScheduler
import org.koin.dsl.module

val domainModule = module {


    // Sample Data
    single<AssetLoader> { AssetLoaderImpl(get()) }
    single { LoadSampleDataUseCase(get(), get()) }

    single<ReminderScheduler> { NotificationReminderScheduler(get(), get()) }


}


/*
    single { GetNoteWithTagsUseCase(get()) }
    single { SaveNoteUseCase(get()) }
    single { UpdateNoteReminderUseCase(get()) }
    single { DeleteNoteUseCase(get()) }
    single { GetAllTagsUseCase(get()) }
    single { EmptyTrashWithTagsUseCase(get()) }
    single { GetUpcomingRemindersUseCase(get()) }
    single { GetNoteByIdUseCase(get()) }
    single { GetUpdateTagsUseCase(get()) }
    single { UpdateNotesUseCase(get()) }
    single { GetAllNotesTagsUseCase(get()) }
    single { GetAllNotesUseCase(get()) }
    single { GetNoteWithTagsByIdUseCase(get()) }
    single { GetNoteWithTagsUseCase(get()) }
    single { GetInsertTagsUseCase(get()) }
    single { InsertNoteWithTagsUseCase(get()) }
    single { GetDeleteTagsUseCase(get()) }
    single { UpdateNoteTagsUseCase(get()) }
    single { CreateBackupUseCase(get()) }
    single { RestoreBackupUseCase(get()) }
    */