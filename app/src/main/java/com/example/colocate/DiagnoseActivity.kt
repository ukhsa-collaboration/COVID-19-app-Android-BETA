package com.example.colocate

import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.le.*
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class DiagnoseActivity : AppCompatActivity() {

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isMultipleAdvertisementSupported) {
            setContentView(R.layout.activity_activate_bluetooth)
            return
        }

        setContentView(R.layout.activity_diagnosis)

        bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner

        startAdvertising()
        startScanning()

        val radioGroup = findViewById<RadioGroup>(R.id.diagnosis_answer)

        findViewById<Button>(R.id.confirm_diagnosis).setOnClickListener {
            val selected = radioGroup.checkedRadioButtonId
            if (selected == -1)
                return@setOnClickListener

            val intent = if (selected == R.id.yes) {
                Intent(this, IsolateActivity::class.java)
            } else {
                Intent(this, OkActivity::class.java)
            }

            startActivity(intent)
        }
    }

    private fun startScanning() {
        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                Log.i(
                    "Scanning",
                    "Received ${result.toString()}"
                )
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                Log.i(
                    "Scanning",
                    "Received ${results.toString()}"
                )
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e(
                    "Scanning",
                    "Scan failed $errorCode"
                )
            }
        }

        bluetoothLeScanner.startScan(
            listOf(scanFilter()),
            scanSettings(),
            scanCallback
        )
    }

    private fun startAdvertising() {
        val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.i(
                    "Advertising",
                    "Started advertising with settings ${settingsInEffect.toString()}"
                )
            }
        }

        bluetoothLeAdvertiser.startAdvertising(
            advertiseSettings(),
            advertiseData(),
            advertiseCallback
        )
    }
}
