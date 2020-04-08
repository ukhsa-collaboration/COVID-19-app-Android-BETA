package com.example.colocate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import timber.log.Timber

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
