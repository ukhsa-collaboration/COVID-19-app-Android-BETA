package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.AppDatabase
import uk.nhs.nhsx.sonar.android.app.notifications.AcknowledgementsDao
import uk.nhs.nhsx.sonar.android.app.notifications.AndroidNotificationSender
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationSender

@Module
class NotificationsModule {

    @Provides
    fun provideNotificationsSender(context: Context): NotificationSender =
        AndroidNotificationSender(context)

    @Provides
    fun provideAcknowledgementsDao(database: AppDatabase): AcknowledgementsDao =
        database.acknowledgementsDao()
}
