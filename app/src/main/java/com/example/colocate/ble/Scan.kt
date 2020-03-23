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
    private val coLocateServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .build()

    /*
     When the iPhone app goes into the background iOS changes how services are advertised:
  
         1) The service uuid is now null
         2) The information to identify the service is encoded into the manufacturing data in a
         unspecified/undocumented way.
  
        The below filter is based on observation of the advertising packets produced by an iPhone running
        the app in the background.
       */
    private val coLocateBackgroundedIPhoneFilter = ScanFilter.Builder()
        .setServiceUuid(null)
        .setManufacturerData(
            76,
            byteArrayOf(
                0x01, // 0
                0x00, // 1
                0x00, // 2
                0x00, // 3
                0x00, // 4
                0x00, // 5
                0x00, // 6
                0x00, // 7
                0x00, // 8
                0x00, // 9
                0x40, // 10
                0x00, // 11
                0x00, // 12
                0x00, // 13
                0x00, // 14
                0x00, // 15
                0x00  // 16
            )
        )
        .build()


    private val filters = listOf(
        coLocateServiceUuidFilter,
        coLocateBackgroundedIPhoneFilter
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
        results.distinctBy { it.device.address }.forEach { onResult(it) }
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

