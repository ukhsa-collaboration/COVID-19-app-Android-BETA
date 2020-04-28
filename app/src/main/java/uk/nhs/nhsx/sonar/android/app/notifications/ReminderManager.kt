/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.joda.time.DateTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.util.showCheckInReminderNotification
import javax.inject.Inject

class ReminderManager @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {

    fun scheduleCheckInReminder(time: DateTime) {
        Timber.d("Schedule check-in reminder for: $time")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time.millis,
            generateReminderPendingIntent(REQUEST_CODE_CHECK_IN_REMINDER)
        )
    }

    fun cancelReminder() {
        Timber.d("cancelReminder")
        alarmManager.cancel(generateReminderPendingIntent(REQUEST_CODE_CHECK_IN_REMINDER))
    }

    private fun generateReminderPendingIntent(requestCode: Int): PendingIntent? {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(REMINDER_TYPE, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun handleReminderBroadcast(intent: Intent) {
        when (intent.getIntExtra(REMINDER_TYPE, -1)) {
            REQUEST_CODE_CHECK_IN_REMINDER -> showCheckInReminderNotification(context)
        }
    }

    fun handleBootComplete() {
    }

    companion object {
        const val REMINDER_TYPE = "REMINDER_TYPE"
        const val REQUEST_CODE_CHECK_IN_REMINDER = 2
    }
}
