/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt.GATT_FAILURE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGatt.STATE_DISCONNECTED
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.GATT
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothCryptogramProvider
import javax.inject.Inject

class Gatt @Inject constructor(
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val bluetoothCryptogramProvider: BluetoothCryptogramProvider
) {
    // No semantic value, just to avoid caching.
    private var keepAliveValue: Byte = 0x00
    private var running: Boolean = true

    private val payload: ByteArray
        get() = bluetoothCryptogramProvider.provideBluetoothCryptogram().asBytes()

    private val payloadIsValid: Boolean
        get() = bluetoothCryptogramProvider.canProvideCryptogram()

    private val keepAliveCharacteristic = BluetoothGattCharacteristic(
        COLOCATE_KEEPALIVE_CHARACTERISTIC_UUID,
        PROPERTY_READ + PROPERTY_WRITE + PROPERTY_WRITE_NO_RESPONSE + PROPERTY_NOTIFY,
        PERMISSION_READ + PERMISSION_WRITE
    ).also {
        it.addDescriptor(
            BluetoothGattDescriptor(
                UPDATE_DESCRIPTOR_UUID,
                PERMISSION_READ + PERMISSION_WRITE
            )
        )
    }

    private val identityCharacteristic = BluetoothGattCharacteristic(
        DEVICE_CHARACTERISTIC_UUID,
        PROPERTY_READ,
        PERMISSION_READ
    )

    private val service: BluetoothGattService =
        BluetoothGattService(COLOCATE_SERVICE_UUID, SERVICE_TYPE_PRIMARY)
            .also {
                it.addCharacteristic(
                    identityCharacteristic
                )
                it.addCharacteristic(
                    keepAliveCharacteristic
                )
            }

    private var server: BluetoothGattServer? = null
    private val subscribedDevices = mutableListOf<BluetoothDevice>()
    private val lock = Object()

    fun start(coroutineScope: CoroutineScope) {
        Timber.d("Bluetooth Gatt start")
        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.isKeepAlive()) {
                    server?.sendResponse(device, requestId, GATT_SUCCESS, 0, byteArrayOf())
                    return
                }
                if (characteristic.isDeviceIdentifier() && payloadIsValid) {
                    server?.sendResponse(device, requestId, GATT_SUCCESS, 0, payload)
                } else {
                    server?.sendResponse(device, requestId, GATT_FAILURE, 0, byteArrayOf())
                }
            }

            override fun onConnectionStateChange(
                device: BluetoothDevice?,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    subscribedDevices.remove(device)
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                // TODO: Reject Indication requests
                if (device == null ||
                    descriptor == null ||
                    !descriptor.isNotifyDescriptor() ||
                    !descriptor.characteristic.isKeepAlive()
                ) {
                    if (responseNeeded)
                        server?.sendResponse(device, requestId, GATT_FAILURE, 0, byteArrayOf())
                    return
                }
                Timber.d("Device $device has subscribed to keep alive.")
                synchronized(lock) {
                    subscribedDevices.add(device)
                }
            }
        }

        server = bluetoothManager.openGattServer(context, callback).also {
            it.addService(service)
        }

        coroutineScope.launch {
            while (running) {
                delay(8_000)
                var connectedSubscribers: Set<BluetoothDevice>
                synchronized(lock) {
                    connectedSubscribers =
                        bluetoothManager.getConnectedDevices(GATT)
                            .intersect(subscribedDevices)
                }

                if (connectedSubscribers.isEmpty()) {
                    continue
                }

                keepAliveValue++
                keepAliveCharacteristic.value = byteArrayOf(keepAliveValue)
                connectedSubscribers.forEach {
                    Timber.d("Notifying $it of new value $keepAliveValue")
                    server?.notifyCharacteristicChanged(it, keepAliveCharacteristic, false)
                }
            }
        }
    }

    fun stop() {
        Timber.d("Bluetooth Gatt stop")
        running = false
        server?.close()
    }
}
