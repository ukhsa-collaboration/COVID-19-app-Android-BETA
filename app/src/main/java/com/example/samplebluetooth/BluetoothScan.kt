package com.example.samplebluetooth

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.util.*


fun scanFilter(): ScanFilter {
    val mBuilder = ScanFilter.Builder()
    val uuid = UUID.fromString(APP_UUID)
    mBuilder.setServiceUuid(ParcelUuid(uuid))
    return mBuilder.build()
}

fun scanSettings(): ScanSettings {
    val mBuilder = ScanSettings.Builder()
    mBuilder.setReportDelay(0)
    mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
    return mBuilder.build()
}