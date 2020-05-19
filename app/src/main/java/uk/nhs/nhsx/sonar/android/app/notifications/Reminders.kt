/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import org.joda.time.DateTime
import javax.inject.Inject

class Reminders @Inject constructor(
    private val alarmManager: AlarmManager,
    private val checkInReminderNotification: CheckInReminderNotification,
    private val reminderBroadcastFactory: ReminderBroadcastFactory
) {

    fun scheduleCheckInReminder(time: DateTime) {
        val broadcast = reminderBroadcastFactory.create(REQUEST_CODE_CHECK_IN_REMINDER)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.millis, broadcast)
    }

    fun handleReminderBroadcast(intent: Intent) {
        when (intent.getIntExtra(REMINDER_TYPE, -1)) {
            REQUEST_CODE_CHECK_IN_REMINDER -> checkInReminderNotification.show()
        }
    }

    companion object {
        const val REMINDER_TYPE = "REMINDER_TYPE"
        const val REQUEST_CODE_CHECK_IN_REMINDER = 2
    }
}

class ReminderBroadcastFactory @Inject constructor(private val context: Context) {

    fun create(reminderType: Int): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(Reminders.REMINDER_TYPE, reminderType)
        }

        return PendingIntent.getBroadcast(context, reminderType, intent, FLAG_UPDATE_CURRENT)
    }
}
