/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.*

class GattClientCallback(
    private val context: Context,
    private val devices: MutableSet<String>
) :
    BluetoothGattCallback() {
    private var rssi: Int? = null
    private var identifier: String? = null

    private fun storeIfReady(gatt: BluetoothGatt) {
        if (this.rssi != null && this.identifier != null) {
            Log.i(
                "Storing",
                "Identifier: $identifier - Rssi: $rssi"
            )
            val input = workDataOf(
                "identifier" to identifier,
                "rssi" to rssi
            )
            val request = OneTimeWorkRequestBuilder<SaveContactWorker>()
                .setInputData(input)
                .build()
            WorkManager.getInstance(context)
                .enqueue(request)
            gatt.disconnect()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.i("onServicesDiscovered", "status: $status")

        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect()
            return
        }

        gatt.getService(COLOCATE_SERVICE_UUID)
            ?.getCharacteristic(DEVICE_CHARACTERISTIC_UUID)
            ?.let {
                gatt.readCharacteristic(it)
                gatt.readRemoteRssi()
            }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect()
            return
        }

        this.rssi = rssi
        storeIfReady(gatt)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect()
            return
        }

        if (characteristic.isDeviceIdentifier() && characteristic.value != null) {
            this.identifier = UUID.nameUUIDFromBytes(characteristic.value)
                .toString()
            storeIfReady(gatt)
        }
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        Log.i(
            "onConnectionStateChange",
            "device: ${gatt.device} status: $status, state: $newState"
        )
        if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
            Log.i("onConnectionStateChange", "Disconnecting...")
            devices.remove(gatt.device.address)
            gatt.close()
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.i("onConnectionStateChange", "Failed, disconnecting")
            gatt.disconnect()
            return
        }

        if (newState == BluetoothAdapter.STATE_CONNECTED) {
            val bondState = gatt.device.bondState
            if (bondState == BluetoothDevice.BOND_BONDING) {
                Log.i("onConnectionStateChange", "Still bonding")
                return
            }

//            val delay = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N && bondState == BluetoothDevice.BOND_BONDED) 1000 else 0
//            val discoverServicesRunnable = Runnable {
//                Log.d("onConnectionStateChange", "discovering services of '%s' with delay of %d ms")
//                val result = gatt.discoverServices()
//                if (!result) {
//                    Log.e("onConnectionStateChange", "discoverServices failed to start")
//                }
//            }
//            bleHandler.postDelayed(discoverServicesRunnable, delay)

            Log.i("onConnectionStateChange", "Discovering services")
            gatt.discoverServices()
        }
    }
}