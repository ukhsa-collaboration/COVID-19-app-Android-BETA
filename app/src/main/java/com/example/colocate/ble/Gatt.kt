/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattService.*
import android.content.Context
import android.util.Log

class Gatt(private val context: Context, private val bluetoothManager: BluetoothManager) {
    private val value = "AB12CD45".toByteArray()

    private val service: BluetoothGattService = BluetoothGattService(
        COLOCATE_SERVICE_UUID,
        SERVICE_TYPE_PRIMARY
    )
        .also {
            it.addCharacteristic(
                BluetoothGattCharacteristic(
                    DEVICE_CHARACTERISTIC_UUID,
                    PROPERTY_READ,
                    PERMISSION_READ
                )
            )
        }

    private lateinit var server: BluetoothGattServer

    fun start() {
        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.i("onCharacteristicReadRequest", "UUID: ${characteristic.uuid}")

                if (characteristic.isDeviceIdentifier()) {
                    server.sendResponse(
                        device,
                        requestId,
                        GATT_SUCCESS,
                        0,
                        value
                    )
                }
            }
        }

        server = bluetoothManager.openGattServer(context, callback).also {
            it.addService(service)
        }
    }

    fun stop() {
        server.close()
    }
}