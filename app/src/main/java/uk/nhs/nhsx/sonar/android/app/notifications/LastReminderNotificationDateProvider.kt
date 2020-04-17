/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.Context
import androidx.core.content.edit
import java.util.Calendar
import javax.inject.Inject

class LastReminderNotificationDateProvider @Inject constructor(context: Context) {
    private val sharedPreferences by lazy {
        context.getSharedPreferences("lastReminderNotification", Context.MODE_PRIVATE)
    }

    fun getLastReminderNotificationDate(): Calendar {
        val lastTimeInMillis = sharedPreferences.getLong(KEY, NEVER)
        return Calendar.getInstance().apply {
            timeInMillis = lastTimeInMillis
        }
    }

    fun setLastReminderNotificationDate(lastReminderDate: Calendar) =
        sharedPreferences.edit { putLong(KEY, lastReminderDate.timeInMillis) }

    companion object {
        private const val KEY = "REMINDER_LAST_DATE"
        private val NEVER = Calendar.getInstance().apply {
            set(1970, 1, 1)
        }.timeInMillis
    }
}
