package com.example.ladybugos.di

import androidx.room.Room
import androidx.work.WorkerParameters
import com.example.ladybugos.MainViewModel
import com.example.ladybugos.datastore.DatastoreSettingsRepo
import com.example.ladybugos.datastore.SettingsRepo
import com.example.ladybugos.model.NoteDatabase
import com.example.ladybugos.notification.NotificationHelper
import com.example.ladybugos.notification.NotificationWorker
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.ui.drawer.TrashViewModel
import com.example.ladybugos.ui.presentation.EditNoteViewModel
import com.example.ladybugos.ui.presentation.NotesViewModel
import com.example.ladybugos.ui.presentation.WidgetUpdater
import com.example.ladybugos.widget.NoteSelectionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { get<NoteDatabase>().noteDao() }


    // Use Cases
//    single<NotesUseCasesInterface> { NotesUseCasesImpl(get()) }

    single { NoteRepository(get(), get(), get()) }

    factory { WidgetUpdater(get()) }

    viewModel { NotesViewModel(get(), get(), get(), get()) }
    viewModel { EditNoteViewModel(get(), get(), get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { TrashViewModel(get(), get(), get()) }

    // Widgets
    viewModel { NoteSelectionViewModel(get(), get()) }
}


val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            NoteDatabase::class.java,
            "note_database"
        )
            /*    .addMigrations(
                    NoteDatabase.MIGRATION_1_2,
                    NoteDatabase.MIGRATION_2_3,
                    NoteDatabase.MIGRATION_3_4
                )*/
            .build()
    }

    single { get<NoteDatabase>().noteDao() }
    single { get<NoteDatabase>().tagDao() }
}

val dataStoreModule = module {
    single<SettingsRepo> { DatastoreSettingsRepo(get()) }
}

val notificationModule = module {
    single { NotificationHelper(get()) }
    factory { (params: WorkerParameters) ->
        NotificationWorker(get(), params)
    }
}