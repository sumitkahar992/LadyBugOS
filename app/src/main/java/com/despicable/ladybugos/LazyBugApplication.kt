package com.despicable.ladybugos

import android.app.Application
import com.despicable.core.data.di.dataModule
import com.despicable.core.database.di.databaseModule
import com.despicable.core.datastore.di.dataStoreModule
import com.despicable.feature.backup.di.backupModule
import com.despicable.feature.detail.di.detailKoinModule
import com.despicable.feature.home.di.homeModule
import com.despicable.feature.settings.di.settingsModule
import com.despicable.ladybugos.di.appModule
import com.despicable.notifications.di.notificationModule
import com.despicable.widgets.di.widgetModule
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
            get<com.despicable.widgets.data.NoteWidgetRepository>().restoreWidgets()
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
        dataModule,
        dataStoreModule,
        notificationModule,
        widgetModule,
        homeModule,
        detailKoinModule,
        backupModule,
        settingsModule
    )


}