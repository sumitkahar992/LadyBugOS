package com.example.ladybugos

import android.app.Application
import com.example.ladybugos.claude_note.kAppModule
import com.example.ladybugos.di.appModule
import com.example.ladybugos.di.dataStoreModule
import com.example.ladybugos.di.databaseModule
import com.example.ladybugos.di.notificationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import timber.log.Timber


class LazyBugApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        initializeKoin()
    }


    private fun initializeKoin() {
        startKoin {
            androidLogger()
            androidContext(this@LazyBugApplication)
            modules(getAllModules())
        }
    }

    private fun getAllModules(): List<Module> = listOf(
        appModule,
        kAppModule,
        databaseModule,
        dataStoreModule,
        notificationModule
    )


}