package com.example.ladybugos.di

import androidx.room.Room
import androidx.work.WorkerParameters
import com.example.ladybugos.MainViewModel
import com.example.ladybugos.datastore.DatastoreRepo
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.AssetLoader
import com.example.ladybugos.model.AssetLoaderImpl
import com.example.ladybugos.model.LoadSampleDataUseCase
import com.example.ladybugos.model.NoteDatabase
import com.example.ladybugos.notification.NotificationHelper
import com.example.ladybugos.notification.NotificationWorker
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.backup.NativeBackupManager
import com.example.ladybugos.ui.backup.NativeBackupViewModel
import com.example.ladybugos.ui.drawer.home.NoteListViewModel
import com.example.ladybugos.ui.drawer.note_detail.NoteDetailViewModel
import com.example.ladybugos.ui.drawer.settings.SettingsViewModel
import com.example.ladybugos.ui.drawer.trash.TrashViewModel
import com.example.ladybugos.usecase.BatchUpdateNoteUseCase
import com.example.ladybugos.usecase.DeleteNoteUseCase
import com.example.ladybugos.usecase.EmptyTrashUseCase
import com.example.ladybugos.usecase.UpdateNoteUseCase
import com.example.ladybugos.widget.CoroutineDispatchers
import com.example.ladybugos.widget.DefaultCoroutineDispatchers
import com.example.ladybugos.widget.NoteSelectionViewModel
import com.example.ladybugos.widget.NoteWidgetRepository
import com.example.ladybugos.widget.WidgetUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module




val appModule = module {


    // Use Cases
    single { UpdateNoteUseCase(get(), get(), get()) }
    single { DeleteNoteUseCase(get(), get(), get()) }
    single { EmptyTrashUseCase(get(), get(), get()) }
    single { BatchUpdateNoteUseCase(get(), get(), get()) }
    // single<NotesUseCasesInterface> { NotesUseCasesImpl(get()) }

    single { NoteRepository(get(), get(), get(), get()) }

    factory { WidgetUpdater(get()) }
    single<CoroutineDispatchers> { DefaultCoroutineDispatchers() }


    viewModel { NoteListViewModel(get(), get(), get(), get()) }
    viewModel { NoteDetailViewModel(get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { TrashViewModel(get(), get(), get()) }


    single { NativeBackupManager(get(), get()) }
    viewModel { NativeBackupViewModel(get()) }

    // Widgets
    viewModel { NoteSelectionViewModel(get(), get(), get()) }

    single { NoteWidgetRepository(get(), get(), get()) }


    // Setting
    viewModel { SettingsViewModel(get()) }

    // Sample Data
    single<AssetLoader> { AssetLoaderImpl(get()) }
    single { LoadSampleDataUseCase(get(), get()) }
}


val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            NoteDatabase::class.java,
            "note_database"
        ).build()
    }

    single { get<NoteDatabase>().noteDao() }
    single { get<NoteDatabase>().tagDao() }
    single { get<NoteDatabase>().noteTagCrossRefDao() }
    single { get<NoteDatabase>().checklistDao() }
}


val dataStoreModule = module {
    single<SettingsRepo> { DatastoreRepo(get()) }
}


val notificationModule = module {
    single { NotificationHelper(get()) }
    factory { (params: WorkerParameters) ->
        NotificationWorker(get(), params)
    }
}