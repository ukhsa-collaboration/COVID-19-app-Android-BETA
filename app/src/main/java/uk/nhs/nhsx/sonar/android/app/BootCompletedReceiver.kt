package uk.nhs.nhsx.sonar.android.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import javax.inject.Inject

class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var stateStorage: StateStorage

    @Inject
    lateinit var reminders: Reminders

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var registrationManager: RegistrationManager

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)
        handle(context, intent)
    }

    fun handle(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        startBluetoothService(context)
        setReminder()
    }

    private fun setReminder() {
        val state = stateStorage.get()
        if (state is RedState) {
            reminders.scheduleCheckInReminder(state.until)
        }
    }

    private fun startBluetoothService(context: Context) {
        if (sonarIdProvider.hasProperSonarId())
            BluetoothService.start(context)
    }
}
