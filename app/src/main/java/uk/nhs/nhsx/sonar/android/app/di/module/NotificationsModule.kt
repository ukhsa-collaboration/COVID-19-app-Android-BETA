/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.notifications.FixedReminderTimeProvider
import uk.nhs.nhsx.sonar.android.app.notifications.LastReminderNotificationDateProvider
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderTimeProvider
import uk.nhs.nhsx.sonar.android.app.registration.FirebaseTokenRetriever
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever

@Module
class NotificationsModule {

    @Provides
    fun provideTokenRetriever(implementation: FirebaseTokenRetriever): TokenRetriever =
        implementation

    @Provides
    fun provideReminderTimeProvider(lastReminderNotificationDateProvider: LastReminderNotificationDateProvider): ReminderTimeProvider =
        FixedReminderTimeProvider(lastReminderNotificationDateProvider)
}
