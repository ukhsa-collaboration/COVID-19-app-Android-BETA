package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.appComponent
import javax.inject.Inject

class RegistrationReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderManager: ReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        context.appComponent.inject(this)

        reminderManager.handleReminderBroadcast()
    }
}
