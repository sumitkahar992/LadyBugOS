package com.despicable.notifications.di

import androidx.work.WorkerParameters
import com.despicable.notifications.NotificationHelper
import com.despicable.notifications.NotificationWorker
import org.koin.dsl.module


val notificationModule = module {
    single { NotificationHelper(get()) }
    factory { (params: WorkerParameters) ->
        NotificationWorker(get(), params)
    }
}