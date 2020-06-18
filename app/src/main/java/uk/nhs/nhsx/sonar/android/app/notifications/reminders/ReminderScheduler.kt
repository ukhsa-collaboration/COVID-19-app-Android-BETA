/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.notifications.CheckInReminderNotification
import uk.nhs.nhsx.sonar.android.app.notifications.ExpiredExposedReminderNotification
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder.Companion.REMINDER_TYPE
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder.Companion.REMINDER_TYPE_CHECK_IN
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.Reminder.Companion.REMINDER_TYPE_EXPIRED_EXPOSED
import javax.inject.Inject

class ReminderScheduler @Inject constructor(
    private val alarmManager: AlarmManager,
    private val reminderStorage: ReminderStorage,
    private val checkInReminderNotification: CheckInReminderNotification,
    private val expiredExposedReminderNotification: ExpiredExposedReminderNotification,
    private val reminderBroadcastFactory: ReminderBroadcastFactory
) {

    fun scheduleExpiredExposedReminder(time: DateTime) {
        val reminder = Reminder(
            time = time.millis,
            type = REMINDER_TYPE_EXPIRED_EXPOSED
        )
        newReminder(reminder)
        reminderStorage.setPendingReminder(reminder)
    }

    fun scheduleCheckInReminder(time: DateTime) {
        val reminder = Reminder(
            time = time.millis,
            type = REMINDER_TYPE_CHECK_IN
        )
        newReminder(reminder)
        reminderStorage.setPendingReminder(reminder)
    }

    fun reschedulePendingReminder() {
        reminderStorage.getPendingReminder()
            ?.let { reminder -> newReminder(reminder) }
    }

    fun cancelReminders() {
        alarmIntent(REMINDER_TYPE_CHECK_IN).cancel()
        alarmIntent(REMINDER_TYPE_EXPIRED_EXPOSED).cancel()
        reminderStorage.clear()
    }

    fun handleReminderBroadcast(intent: Intent) {
        when (intent.getIntExtra(REMINDER_TYPE, -1)) {
            REMINDER_TYPE_CHECK_IN -> {
                checkInReminderNotification.show()
                reminderStorage.clear()
            }
            REMINDER_TYPE_EXPIRED_EXPOSED -> {
                expiredExposedReminderNotification.show()
                reminderStorage.clear()
            }
        }
    }

    private fun newReminder(reminder: Reminder) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.time,
            alarmIntent(reminder.type)
        )
    }

    private fun alarmIntent(reminderType: Int): PendingIntent {
        return reminderBroadcastFactory.create(reminderType)
    }
}
