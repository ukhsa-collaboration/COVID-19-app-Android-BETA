/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.example.colocate.di.module.AppModule
import com.example.colocate.persistence.ContactEventDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class Scan @Inject constructor(
    private val context: Context,
    private val bluetoothLeScanner: BluetoothLeScanner,
    private val contactEventDao: ContactEventDao,
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher
) {
    private var coroutineScope: CoroutineScope? = null
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
                0x00 // 16
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

    private val scanCallBack = ScanningCallback(::handleScanResult, ::handleScanFailure)

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        bluetoothLeScanner.startScan(
            filters,
            settings,
            scanCallBack
        )
    }

    fun stop() {
        bluetoothLeScanner.stopScan(scanCallBack)
        coroutineScope = null
    }

    private val devices = mutableSetOf<String>()

    private fun handleScanResult(result: ScanResult) {
        Timber.v(
            "Scanning Received $result"
        )

        val address = result.device.address

        if (devices.contains(address)) {
            Timber.v(
                "Scanning Ignoring the already connected device: $address"
            )
            return
        }

        devices.add(address)

        result.device.connectGatt(
            context, false,
            GattClientCallback(devices, ::save), BluetoothDevice.TRANSPORT_LE
        )
    }

    private fun handleScanFailure(errorCode: Int) {
        Timber.e(
            "Scanning Scan failed $errorCode"
        )
    }

    private fun save(rssi: Int, identifier: String) {
        Timber.d("Scanning Saving: rssi = $rssi id = $identifier")
        coroutineScope?.let { coroutineScope ->
            SaveContactWorker(dispatcher, contactEventDao).saveContactEvent(
                coroutineScope,
                identifier,
                rssi
            )
        }
    }
}

private class ScanningCallback(
    private val onScanResult: (ScanResult) -> Unit,
    private val onScanError: (Int) -> Unit
) : ScanCallback() {

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        onResult(result)
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        results.distinctBy { it.device.address }.forEach { onResult(it) }
    }

    private fun onResult(result: ScanResult) {
        onScanResult(result)
    }

    override fun onScanFailed(errorCode: Int) {
        onScanError(errorCode)
    }
}
