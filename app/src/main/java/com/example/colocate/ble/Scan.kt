/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble


import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*

class Scan(
    context: Context,
    private val bluetoothLeScanner: BluetoothLeScanner
) {
    private val filters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
            .build()
    )

    private val settings = ScanSettings.Builder()
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    private val scanCallBack = ScanningCallback(context)

    fun start() {
        bluetoothLeScanner.startScan(
            filters,
            settings,
            scanCallBack
        )
    }

    fun stop() {
        bluetoothLeScanner.stopScan(scanCallBack)
    }
}


private class ScanningCallback(private val context: Context) : ScanCallback() {

    private val devices = mutableSetOf<String>()

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

        val address = result.device.address

        if (devices.contains(address)) {
            Log.i(
                "Scanning",
                "Ignoring the already connected device: $address"
            )
            return
        }

        devices.add(address)

        result.device.connectGatt(context, false, GattClientCallback(devices), TRANSPORT_LE)
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e(
            "Scanning",
            "Scan failed $errorCode"
        )
    }
}

private class GattClientCallback(private val devices: MutableSet<String>) : BluetoothGattCallback() {
    private var rssi: Int? = null
    private var identifier: String? = null

    private fun storeIfReady() {
        if (this.rssi != null && this.identifier != null) {
            Log.i("Storing", "Identifier: $identifier - Rssi: $rssi")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.i("onServicesDiscovered", "status: $status")

        if (status == GATT_SUCCESS) {
            gatt.getService(COLOCATE_SERVICE_UUID).getCharacteristic(DEVICE_CHARACTERISTIC_UUID)
                .let {
                    gatt.readCharacteristic(it)
                    gatt.readRemoteRssi()
                }
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        this.rssi = rssi
        storeIfReady()
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (characteristic.isDeviceIdentifier()) {
            this.identifier = UUID.nameUUIDFromBytes(characteristic.value).toString()
            storeIfReady()
        }
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
        } else if (newState == STATE_DISCONNECTING || newState == STATE_DISCONNECTED) {
            Log.i("onConnectionStateChange", "Disconnecting...")
            devices.remove(gatt.device.address)
        }
    }
}
