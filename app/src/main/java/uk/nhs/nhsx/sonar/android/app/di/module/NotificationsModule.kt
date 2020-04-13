package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.notifications.AcknowledgementsDao
import uk.nhs.nhsx.sonar.android.app.notifications.AndroidNotificationSender
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationSender
import uk.nhs.nhsx.sonar.android.app.persistence.AppDatabase

@Module
class NotificationsModule {

    @Provides
    fun provideNotificationsSender(context: Context): NotificationSender =
        AndroidNotificationSender(context)

    @Provides
    fun provideAcknowledgementsDao(database: AppDatabase): AcknowledgementsDao =
        database.acknowledgementsDao()
}
