package com.example.colocate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BluetoothService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(COLOCATE_SERVICE_ID, notification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !isPermissionGranted()) {
            return START_NOT_STICKY
        }

        startAdvertising(bluetoothAdapter.bluetoothLeAdvertiser)
        startScanning(bluetoothAdapter.bluetoothLeScanner)


        return START_STICKY
    }

    private fun isPermissionGranted() = true

    private fun notification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                COLOCATE_NOTIFICATION_ID,
                "NHS Colocate",
                NotificationManager.IMPORTANCE_DEFAULT
            ).let {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(it)
            }
            NotificationCompat.Builder(this, COLOCATE_NOTIFICATION_ID).build()
        } else {
            NotificationCompat.Builder(this).build()
        }
    }

    private fun startScanning(bluetoothLeScanner: BluetoothLeScanner) {
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

    private fun startAdvertising(bluetoothLeAdvertiser: BluetoothLeAdvertiser) {
        val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.i(
                    "Advertising",
                    "Started advertising with settings ${settingsInEffect.toString()}"
                )
            }


            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e(
                    "Advertising",
                    "Failed to start with error code $errorCode"
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