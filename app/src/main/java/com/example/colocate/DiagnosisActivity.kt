package com.example.colocate

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class DiagnosisActivity : AppCompatActivity() {

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isMultipleAdvertisementSupported ) {
            setContentView(R.layout.activity_activate_bluetooth)
            return
        }

        setContentView(R.layout.activity_diagnosis)

        bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner

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
}
