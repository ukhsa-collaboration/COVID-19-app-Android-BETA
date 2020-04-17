package uk.nhs.nhsx.sonar.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import javax.inject.Inject

class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var reminderManager: ReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("CoLocate onReceive: $intent")
        context.appComponent.inject(this)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("CoLocate onReceive hasProperSonarId: ${sonarIdProvider.hasProperSonarId()}")
            if (sonarIdProvider.hasProperSonarId()) {
                BluetoothService.start(context)
            }
            reminderManager.handleBootComplete()
        }
    }
}
