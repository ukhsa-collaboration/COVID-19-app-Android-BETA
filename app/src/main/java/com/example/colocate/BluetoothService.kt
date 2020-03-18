package com.example.colocate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class BluetoothService : Service() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var gattServer: BluetoothGattServer

    private lateinit var context: Context

    override fun onCreate() {
        super.onCreate()
        startForeground(COLOCATE_SERVICE_ID, notification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?

        if (manager?.adapter == null || !isPermissionGranted()) {
            return START_NOT_STICKY
        }

        bluetoothManager = manager
        bluetoothAdapter = manager.adapter
        context = this

        openGattServer()
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
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(it)
            }
            NotificationCompat.Builder(this, COLOCATE_NOTIFICATION_ID).build()
        } else {
            NotificationCompat.Builder(this, "").build()
        }
    }

    private fun startScanning(bluetoothLeScanner: BluetoothLeScanner) {
        val gattCallBack = object : BluetoothGattCallback() {
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.i("onServicesDiscovered", "status: $status")

                if (status == GATT_SUCCESS) {
                    val characteristic = gatt.getService(COLOCATE_SERVICE_UUID)
                        .getCharacteristic(DEVICE_CHARACTERISTIC_UUID)

                    gatt.readCharacteristic(characteristic)
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.i(
                    "onCharacteristicRead",
                    "UUID: ${characteristic.uuid} - Value: ${String(characteristic.value)}"
                )
            }

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                Log.i(
                    "onConnectionStateChange",
                    "status: $status, state: $newState"
                )

                if (newState == STATE_CONNECTED) {
                    gatt.discoverServices()
                }
            }

        }

        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                onResult(result)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { onResult(it) }
            }

            private fun onResult(result: ScanResult) {
                Log.i(
                    "Scanning",
                    "Received $result"
                )

                result.device.connectGatt(context, false, gattCallBack, TRANSPORT_LE)
            }

            override fun onScanFailed(errorCode: Int) {
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
                Log.i(
                    "Advertising",
                    "Started advertising with settings ${settingsInEffect.toString()}"
                )
            }

            override fun onStartFailure(errorCode: Int) {
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

    private fun openGattServer() {
        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.i("onCharacteristicReadRequest", "UUID: ${characteristic.uuid}")

                gattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, "ABC".toByteArray())
            }
        }

        gattServer = bluetoothManager.openGattServer(this, callback)

        gattServer.addService(createGattService())
    }

    private fun createGattService(): BluetoothGattService {
        val characteristic = BluetoothGattCharacteristic(
            DEVICE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        return BluetoothGattService(
            COLOCATE_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
            .apply {
                addCharacteristic(characteristic)
            }
    }
}

