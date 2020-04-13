package uk.nhs.nhsx.sonar.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService

class ServiceRestarterBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        if (intent.action == ACTION_RESTART_BLUETOOTH_SERVICE) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BluetoothService::class.java)
            )
        }
    }

    companion object {
        const val ACTION_RESTART_BLUETOOTH_SERVICE = "ACTION_RESTART_BLUETOOTH_SERVICE"
    }
}
