package com.despicable.core.database.di

import androidx.room.Room
import com.despicable.core.database.NoteDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

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
