package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.hideRegistrationNotFinishedNotification
import uk.nhs.nhsx.sonar.android.app.util.showRegistrationReminderNotification
import java.util.Calendar
import javax.inject.Inject

class ReminderManager @Inject constructor(
    private val context: Context,
    private val reminderTimeProvider: ReminderTimeProvider,
    private val alarmManager: AlarmManager,
    private val sonarIdProvider: SonarIdProvider
) {
    fun scheduleReminder() {
        Timber.d("setupReminder")
        val pendingIntent = generateReminderPendingIntent()

        val time = reminderTimeProvider.provideNextReminderTime()
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

    private fun generateReminderPendingIntent(): PendingIntent? {
        val intent = Intent(context, RegistrationReminderBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE_REGISTRATION_REMINDER,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun handleReminderBroadcast() {
        if (!sonarIdProvider.hasProperSonarId()) {
            showNotification()
            scheduleReminder()
        }
    }

    fun showNotification() {
        showRegistrationReminderNotification(context)
        reminderTimeProvider.setLastReminderNotificationTime(Calendar.getInstance())
    }

    fun hideReminderNotification() {
        hideRegistrationNotFinishedNotification(context)
    }

    fun handleBootComplete() {
        if (!sonarIdProvider.hasProperSonarId()) {
            scheduleReminder()
        }
    }

    companion object {
        const val REQUEST_CODE_REGISTRATION_REMINDER = 1
    }
}
