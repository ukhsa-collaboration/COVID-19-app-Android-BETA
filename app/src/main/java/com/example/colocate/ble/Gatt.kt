/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt.GATT_FAILURE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.colocate.persistence.ResidentIdProvider
import javax.inject.Inject

class Gatt @Inject constructor(
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val residentIdProvider: ResidentIdProvider
) {
    private val identifier: Identifier
        get() = Identifier.fromString(residentIdProvider.getResidentId())

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

    private var server: BluetoothGattServer? = null

    fun start() {
        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.isDeviceIdentifier()) {
                    server?.sendResponse(
                        device,
                        requestId,
                        GATT_SUCCESS,
                        0,
                        identifier.asBytes
                    )
                } else {
                    server?.sendResponse(
                        device,
                        requestId,
                        GATT_FAILURE,
                        0,
                        byteArrayOf()
                    )
                }
            }
        }

        server = bluetoothManager.openGattServer(context, callback).also {
            it.addService(service)
        }
    }

    fun stop() {
        server?.close()
    }
}
