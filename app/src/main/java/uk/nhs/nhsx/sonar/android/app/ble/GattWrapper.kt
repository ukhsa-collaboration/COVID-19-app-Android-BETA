/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdProvider

class GattWrapper(
    private val server: BluetoothGattServer?,
    private val coroutineScope: CoroutineScope,
    private val bluetoothManager: BluetoothManager,
    private val bluetoothIdProvider: BluetoothIdProvider,
    private val keepAliveCharacteristic: BluetoothGattCharacteristic
) {
    var notifyJob: Job? = null

    // No semantic value, just to avoid caching.
    private var keepAliveValue: Byte = 0x00
    private val subscribedDevices = mutableListOf<BluetoothDevice>()
    private val lock = Mutex()

    private val payload: ByteArray
        get() = bluetoothIdProvider.provideBluetoothPayload().cryptogram.asBytes()

    private val payloadIsValid: Boolean
        get() = bluetoothIdProvider.canProvideCryptogram()

    fun respondToCharacteristicRead(
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
        if (characteristic.isKeepAlive()) {
            server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
            return
        }

        if (characteristic.isDeviceIdentifier() && payloadIsValid) {
            server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, payload)
        } else {
            server?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
        }
    }

    fun respondToDescriptorWrite(
        device: BluetoothDevice?,
        descriptor: BluetoothGattDescriptor?,
        responseNeeded: Boolean,
        requestId: Int
    ) {
        // TODO: Reject Indication requests
        if (device == null ||
            descriptor == null ||
            !descriptor.isNotifyDescriptor() ||
            !descriptor.characteristic.isKeepAlive()
        ) {
            if (responseNeeded)
                server?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    byteArrayOf()
                )
            return
        }
        Timber.d("Device $device has subscribed to keep alive.")
        coroutineScope.launch {
            Timber.d("Starting notify job")
            lock.withLock {
                if (subscribedDevices.isEmpty()) {
                    notifyJob = notifyKeepAliveSubscribersPeriodically(coroutineScope)
                }
                subscribedDevices.add(device)
            }
        }
    }

    private fun notifyKeepAliveSubscribersPeriodically(coroutineScope: CoroutineScope) =
        coroutineScope.launch {
            while (isActive) {
                delay(8_000)
                lock.withLock {
                    val connectedSubscribers =
                        bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                            .intersect(subscribedDevices)

                    keepAliveValue++
                    keepAliveCharacteristic.value = byteArrayOf(keepAliveValue)
                    connectedSubscribers.forEach {
                        Timber.d("Notifying $it of new value $keepAliveValue")
                        server?.notifyCharacteristicChanged(it, keepAliveCharacteristic, false)
                    }
                }
            }
        }

    fun deviceDisconnected(device: BluetoothDevice?) {
        if (device == null) return

        coroutineScope.launch {
            lock.withLock {
                subscribedDevices.remove(device)
                if (subscribedDevices.isEmpty()) {
                    Timber.d("Terminating notify job")
                    notifyJob?.cancel()
                }
            }
        }
    }
}
