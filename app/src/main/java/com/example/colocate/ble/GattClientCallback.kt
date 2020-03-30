/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.UUID

class GattClientCallback(
    private val devices: MutableSet<String>,
    private val onReady: (Int, String) -> Unit
) :
    BluetoothGattCallback() {
    private var rssi: Int? = null
    private var identifier: String? = null

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        Timber.i(
            "onConnectionStateChange device: ${gatt.device} status: $status, state: $newState"
        )
        if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
            Timber.i("onConnectionStateChange Disconnecting...")
            devices.remove(gatt.device.address)
            gatt.close()
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Timber.i("onConnectionState ChangeFailed, disconnecting")
            gatt.disconnect()
            return
        }

        if (newState == BluetoothAdapter.STATE_CONNECTED) {
            val bondState = gatt.device.bondState
            if (bondState == BluetoothDevice.BOND_BONDING) {
                Timber.i("onConnectionState ChangeStill bonding")
                return
            }

//            val delay = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N && bondState == BluetoothDevice.BOND_BONDED) 1000 else 0
//            val discoverServicesRunnable = Runnable {
//                Timber.d("onConnectionStateChange discovering services of '%s' with delay of %d ms")
//                val result = gatt.discoverServices()
//                if (!result) {
//                    Timber.e("onConnectionStateChange discoverServices failed to start")
//                }
//            }
//            bleHandler.postDelayed(discoverServicesRunnable, delay)

            Timber.i("onConnectionStateChange Discovering services")
            gatt.discoverServices()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Timber.i("onServicesDiscovered status: $status")

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
            val buffer = ByteBuffer.wrap(characteristic.value)
            val high = buffer.long
            val low = buffer.long
            this.identifier = UUID(high, low).toString()
            storeIfReady(gatt)
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

    private fun storeIfReady(gatt: BluetoothGatt) {
        val currentRssi = rssi
        val currentIdentifier = identifier
        if (currentRssi != null && currentIdentifier != null) {
            onReady(currentRssi, currentIdentifier)
            gatt.disconnect()
        }
    }
}
