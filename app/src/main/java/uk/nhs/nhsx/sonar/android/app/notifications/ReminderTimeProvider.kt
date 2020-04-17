package uk.nhs.nhsx.sonar.android.app.notifications

import java.util.Calendar

interface ReminderTimeProvider {
    fun provideNextReminderTime(): Long
    fun setLastReminderNotificationTime(time: Calendar)
}
