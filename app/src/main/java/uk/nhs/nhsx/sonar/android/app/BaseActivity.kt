/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.sonar.android.app.debug.TesterActivity
import uk.nhs.nhsx.sonar.android.app.edgecases.EnableBluetoothAfterRegistrationActivity
import uk.nhs.nhsx.sonar.android.app.util.ShakeListener
import uk.nhs.nhsx.sonar.android.app.util.isBluetoothEnabled

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var shakeListener: ShakeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shakeListener = ShakeListener(this) {
            TesterActivity.start(this@BaseActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        shakeListener.start()
        if (!isBluetoothEnabled()) {
            bluetoothHasBeenDisabled()
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateBroadcastReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        shakeListener.stop()
        unregisterReceiver(bluetoothStateBroadcastReceiver)
    }

    private fun bluetoothHasBeenDisabled() {
        EnableBluetoothAfterRegistrationActivity.start(this)
    }

    private val bluetoothStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                if (state == BluetoothAdapter.STATE_OFF) {
                    bluetoothHasBeenDisabled()
                }
            }
        }
    }
}
