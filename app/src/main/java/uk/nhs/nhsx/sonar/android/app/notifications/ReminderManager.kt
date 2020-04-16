package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.util.hideRegistrationNotFinishedNotification
import javax.inject.Inject

class ReminderManager @Inject constructor(
    private val context: Context,
    private val reminderTimeProvider: ReminderTimeProvider,
    private val alarmManager: AlarmManager
) {
    fun setupReminder() {
        Timber.d("setupReminder")
        val pendingIntent = generateReminderPendingIntent()

        val time = reminderTimeProvider.provideTime()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }

    fun cancelReminder() {
        Timber.d("cancelReminder")
        alarmManager.cancel(generateReminderPendingIntent())
    }

    fun hideReminderNotification() {
        hideRegistrationNotFinishedNotification(context)
    }

    private fun generateReminderPendingIntent(): PendingIntent? {
        val intent = Intent(context, RegistrationReminderBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE_REGISTRATION_REMINDER,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val REQUEST_CODE_REGISTRATION_REMINDER = 1
    }
}
