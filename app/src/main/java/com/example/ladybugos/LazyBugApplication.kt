package com.example.ladybugos

import android.app.Application
import com.example.ladybugos.di.appModule
import com.example.ladybugos.di.dataStoreModule
import com.example.ladybugos.di.databaseModule
import com.example.ladybugos.di.notificationModule
import com.example.ladybugos.widget.NoteWidgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import timber.log.Timber


class LazyBugApplication : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        initializeKoin()

        // Restore widgets after force stop
        CoroutineScope(Dispatchers.Main).launch {
            get<NoteWidgetRepository>().restoreWidgets()
        }
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
        databaseModule,
        dataStoreModule,
        notificationModule,
    )


}