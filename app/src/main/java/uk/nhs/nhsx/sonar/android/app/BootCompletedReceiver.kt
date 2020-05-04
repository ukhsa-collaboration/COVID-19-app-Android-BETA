package uk.nhs.nhsx.sonar.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Inject

class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var reminders: Reminders

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var registrationManager: RegistrationManager

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        startBluetoothService(context)
        setReminder()
    }

    private fun setReminder() {
        userStateStorage.get().scheduleCheckInReminder(reminders)
    }

    private fun startBluetoothService(context: Context) {
        if (sonarIdProvider.hasProperSonarId())
            BluetoothService.start(context)
    }
}
