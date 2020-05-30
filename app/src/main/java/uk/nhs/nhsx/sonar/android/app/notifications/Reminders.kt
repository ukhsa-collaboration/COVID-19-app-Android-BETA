/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceProvider
import javax.inject.Inject

class Reminders @Inject constructor(
    private val alarmManager: AlarmManager,
    private val checkInReminder: CheckInReminder,
    private val checkInReminderNotification: CheckInReminderNotification,
    private val reminderBroadcastFactory: ReminderBroadcastFactory
) {

    fun scheduleCheckInReminder(time: DateTime) {
        newCheckInReminder(time.millis)
        checkInReminder.setPendingReminder(time.millis)
    }

    fun reschedulePendingCheckInReminder() {
        checkInReminder.getPendingReminder()?.let { triggerAtMillis ->
            newCheckInReminder(triggerAtMillis)
        }
    }

    fun handleReminderBroadcast(intent: Intent) {
        when (intent.getIntExtra(REMINDER_TYPE, -1)) {
            REQUEST_CODE_CHECK_IN_REMINDER -> {
                checkInReminderNotification.show()
                checkInReminder.clear()
            }
        }
    }

    private fun newCheckInReminder(triggerAtMillis: Long) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent())
    }

    private fun alarmIntent(): PendingIntent {
        return reminderBroadcastFactory.create(REQUEST_CODE_CHECK_IN_REMINDER)
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

class CheckInReminder @Inject constructor(context: Context) :
    SharedPreferenceProvider<Long>(
        context,
        preferenceName = "reminder_notification_storage",
        preferenceKey = "reminder_notification"
    ) {

    fun setPendingReminder(triggerAtMillis: Long) = this.set(triggerAtMillis)

    fun getPendingReminder(): Long? =
        get().let {
            if (it != NO_VALUE) it else null
        }

    override fun get(): Long =
        sharedPreferences.getLong(preferenceKey, NO_VALUE)

    override fun set(value: Long) {
        sharedPreferences.edit {
            putLong(preferenceKey, value)
        }
    }

    companion object {
        private const val NO_VALUE = -1L
    }
}
