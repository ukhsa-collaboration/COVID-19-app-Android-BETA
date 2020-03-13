package com.example.colocate


import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import java.util.*

fun advertiseData(): AdvertiseData {
    val mBuilder = AdvertiseData.Builder()
    val uuid = UUID.fromString(APP_UUID)
    mBuilder.addServiceUuid(ParcelUuid(uuid))
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