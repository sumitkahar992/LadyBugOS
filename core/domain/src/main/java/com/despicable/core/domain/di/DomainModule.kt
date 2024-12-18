package com.despicable.core.domain.di

import com.despicable.core.domain.sample.AssetLoader
import com.despicable.core.domain.sample.AssetLoaderImpl
import com.despicable.core.domain.sample.LoadSampleDataUseCase
import com.despicable.core.domain.usecase.DeleteNoteUseCase
import com.despicable.core.domain.usecase.EmptyTrashWithTagsUseCase
import com.despicable.core.domain.usecase.GetAllNotesTagsUseCase
import com.despicable.core.domain.usecase.GetAllNotesUseCase
import com.despicable.core.domain.usecase.GetAllTagsUseCase
import com.despicable.core.domain.usecase.GetDeleteTagsUseCase
import com.despicable.core.domain.usecase.GetInsertTagsUseCase
import com.despicable.core.domain.usecase.GetNoteByIdUseCase
import com.despicable.core.domain.usecase.GetNoteWithTagsByIdUseCase
import com.despicable.core.domain.usecase.GetNoteWithTagsUseCase
import com.despicable.core.domain.usecase.GetUpcomingRemindersUseCase
import com.despicable.core.domain.usecase.GetUpdateTagsUseCase
import com.despicable.core.domain.usecase.InsertNoteWithTagsUseCase
import com.despicable.core.domain.usecase.NoteDetailUseCases
import com.despicable.core.domain.usecase.SaveNoteUseCase
import com.despicable.core.domain.usecase.UpdateNoteReminderUseCase
import com.despicable.core.domain.usecase.UpdateNoteTagsUseCase
import com.despicable.core.domain.usecase.UpdateNotesUseCase
import com.despicable.core.domain.usecase.backup.CreateBackupUseCase
import com.despicable.core.domain.usecase.backup.RestoreBackupUseCase
import org.koin.dsl.module

val domainModule = module {
    single { GetNoteWithTagsUseCase(get()) }
    single { SaveNoteUseCase(get()) }
    single { UpdateNoteReminderUseCase(get()) }
    single { DeleteNoteUseCase(get()) }
    single { GetAllTagsUseCase(get()) }
    single {
        NoteDetailUseCases(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
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

    // Sample Data
    single<AssetLoader> { AssetLoaderImpl(get()) }
    single { LoadSampleDataUseCase(get(), get(), get(), get(), get()) }

    single { CreateBackupUseCase(get()) }
    single { RestoreBackupUseCase(get()) }
}