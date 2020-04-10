package com.example.colocate.di.module

import android.content.Context
import com.example.colocate.notifications.AcknowledgementsDao
import com.example.colocate.notifications.AndroidNotificationSender
import com.example.colocate.notifications.NotificationSender
import com.example.colocate.persistence.AppDatabase
import dagger.Module
import dagger.Provides

@Module
class NotificationsModule {

    @Provides
    fun provideNotificationsSender(context: Context): NotificationSender =
        AndroidNotificationSender(context)

    @Provides
    fun provideAcknowledgementsDao(database: AppDatabase): AcknowledgementsDao =
        database.acknowledgementsDao()
}
