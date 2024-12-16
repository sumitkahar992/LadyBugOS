package com.despicable.notifications.di

import androidx.work.WorkerParameters
import org.koin.dsl.module


val notificationModule = module {
    single { com.despicable.notifications.NotificationHelper(get()) }
    factory { (params: WorkerParameters) ->
        com.despicable.notifications.NotificationWorker(get(), params)
    }
}