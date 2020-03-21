/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble


import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log


class Scan(
    context: Context,
    private val bluetoothLeScanner: BluetoothLeScanner
) {
    private val filters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
            .build(),
        ScanFilter.Builder()
            .setServiceUuid(null) // For detecting iPhone when in background
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
        Log.v(
            "Scanning",
            "Received $result"
        )

        val address = result.device.address

        if (devices.contains(address)) {
            Log.v(
                "Scanning",
                "Ignoring the already connected device: $address"
            )
            return
        }

        devices.add(address)

        result.device.connectGatt(
            context, false,
            GattClientCallback(devices), TRANSPORT_LE
        )
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e(
            "Scanning",
            "Scan failed $errorCode"
        )
    }
}

