package com.despicable.core.datastore.di

import com.despicable.core.datastore.DatastoreRepo
import com.despicable.core.datastore.SettingsRepo
import org.koin.dsl.module

val dataStoreModule = module {
    single<SettingsRepo> {
        DatastoreRepo(
            get()
        )
    }

}
