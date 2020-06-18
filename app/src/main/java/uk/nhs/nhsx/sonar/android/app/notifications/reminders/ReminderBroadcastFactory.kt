package uk.nhs.nhsx.sonar.android.app.notifications.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import javax.inject.Inject

class ReminderBroadcastFactory @Inject constructor(private val context: Context) {

    fun create(reminderType: Int): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            .apply {
                putExtra(Reminder.REMINDER_TYPE, reminderType)
            }

        return PendingIntent.getBroadcast(
            context,
            reminderType,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
