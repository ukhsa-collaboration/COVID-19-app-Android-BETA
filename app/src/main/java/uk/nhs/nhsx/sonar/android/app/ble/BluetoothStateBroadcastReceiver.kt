package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothAdapter.ERROR
import android.bluetooth.BluetoothAdapter.EXTRA_STATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothStateBroadcastReceiver(val stateAction: (Int) -> Unit) : BroadcastReceiver() {

    fun register(context: Context) {
        context.registerReceiver(this, IntentFilter(ACTION_STATE_CHANGED))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STATE_CHANGED)
            return

        val state = intent.getIntExtra(EXTRA_STATE, ERROR)

        stateAction(state)
    }
}
