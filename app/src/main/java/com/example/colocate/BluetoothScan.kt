package com.example.colocate


import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import java.util.*


fun scanFilter(): ScanFilter {
    return ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString(APP_UUID)))
        .build()
}

fun scanSettings(): ScanSettings {
    return ScanSettings.Builder()
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()
}