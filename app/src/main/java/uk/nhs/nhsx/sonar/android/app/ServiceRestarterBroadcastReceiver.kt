/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService

class ServiceRestarterBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        if (intent.action == ACTION_RESTART_BLUETOOTH_SERVICE) {
            BluetoothService.start(context)
        }
    }

    companion object {
        const val ACTION_RESTART_BLUETOOTH_SERVICE = "ACTION_RESTART_BLUETOOTH_SERVICE"

        fun sendBroadcast(context: Context) {
            Intent(context, ServiceRestarterBroadcastReceiver::class.java).let {
                it.action = ACTION_RESTART_BLUETOOTH_SERVICE
                context.sendBroadcast(it)
            }
        }
    }
}
