/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class DiagnoseActivity : AppCompatActivity() {
    @Inject
    protected lateinit var statusStorage: StatusStorage

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ColocateApplication).applicationComponent.inject(this)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter
            ?.takeIf { it.isDisabled }
            ?.apply {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        if (bluetoothAdapter == null || !bluetoothAdapter.isMultipleAdvertisementSupported) {
            setContentView(R.layout.activity_activate_bluetooth)
            return
        }

        setContentView(R.layout.activity_diagnosis)

        ContextCompat.startForegroundService(this, Intent(this, BluetoothService::class.java))

        val radioGroup = findViewById<RadioGroup>(R.id.diagnosis_answer)

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {
            val selected = radioGroup.checkedRadioButtonId
            if (selected == -1)
                return@setOnClickListener

            val intent = if (selected == R.id.yes) {
                statusStorage.update(CovidStatus.RED)
                Intent(this, IsolateActivity::class.java)
            } else {
                statusStorage.update(CovidStatus.OK)
                Intent(this, OkActivity::class.java)
            }

            startActivity(intent)
        }
    }
}
