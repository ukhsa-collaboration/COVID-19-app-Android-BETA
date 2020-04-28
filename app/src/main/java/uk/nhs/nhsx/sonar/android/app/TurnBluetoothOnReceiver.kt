/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class TurnBluetoothOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")

        if (intent.action == ACTION_TURN_BLUETOOTH_ON) {
            BluetoothAdapter.getDefaultAdapter()?.enable()
        }
    }

    companion object {
        const val ACTION_TURN_BLUETOOTH_ON = "ACTION_TURN_BLUETOOTH_ON"
    }
}
