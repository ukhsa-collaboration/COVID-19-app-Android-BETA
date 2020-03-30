/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import com.example.colocate.ble.util.isBluetoothEnabled
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class DiagnoseActivity : AppCompatActivity() {
    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ColocateApplication).applicationComponent.inject(this)

        setContentView(R.layout.activity_diagnosis)

        val radioGroup = findViewById<RadioGroup>(R.id.diagnosis_answer)

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {
            val selected = radioGroup.checkedRadioButtonId
            if (selected == -1)
                return@setOnClickListener

            if (selected == R.id.yes) {
                statusStorage.update(CovidStatus.RED)
                IsolateActivity.start(this)
            } else {
                statusStorage.update(CovidStatus.OK)
                OkActivity.start(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tryStartService()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothBroadcastReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothBroadcastReceiver)
    }

    private fun tryStartService() {
        val bluetoothEnabled = isBluetoothEnabled()
        if (hasLocationPermission(this) && bluetoothEnabled) {
            ContextCompat.startForegroundService(this, Intent(this, BluetoothService::class.java))
        } else if (!bluetoothEnabled) {
            requestEnablingBluetooth()
        }
    }

    private fun requestEnablingBluetooth() {
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private val bluetoothBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                if (state == BluetoothAdapter.STATE_ON) {
                    tryStartService()
                }
                if (state == BluetoothAdapter.STATE_OFF) {
                    requestEnablingBluetooth()
                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        private fun getIntent(context: Context) =
            Intent(context, DiagnoseActivity::class.java)
    }
}
