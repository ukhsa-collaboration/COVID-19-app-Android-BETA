package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.util.showRegistrationReminderNotification

class RegistrationReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        showRegistrationReminderNotification(context)
    }
}
