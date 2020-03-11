package com.example.samplebluetooth

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import java.nio.ByteBuffer
import java.util.*

fun advertiseData(): AdvertiseData {
    val mBuilder = AdvertiseData.Builder()
    val mManufacturerData = ByteBuffer.allocate(24)
    val uuid = getIdAsByte(UUID.fromString(APP_UUID))
    
    // Apple https://stackoverflow.com/a/48444693
    mManufacturerData.put(0, 0x02.toByte()) // Beacon Identifier
    mManufacturerData.put(1, 0x15.toByte()) // Beacon Identifier
    for (i in 2..17) {
        mManufacturerData.put(i, uuid[i - 2]) // adding the UUID
    }
    mManufacturerData.put(18, 0x00.toByte()) // first byte of Major
    mManufacturerData.put(19, 0x01.toByte()) // second byte of Major
    mManufacturerData.put(20, 0x00.toByte()) // first minor
    mManufacturerData.put(21, 0x00.toByte()) // second minor
    mManufacturerData.put(22, 0xB5.toByte()) // txPower
    mBuilder.addManufacturerData(76, mManufacturerData.array()) // using google's company ID
    return mBuilder.build()
}

fun advertiseSettings(): AdvertiseSettings {
    val mBuilder = AdvertiseSettings.Builder()
    mBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
    mBuilder.setConnectable(false)
    mBuilder.setTimeout(0)
    mBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
    return mBuilder.build()
}