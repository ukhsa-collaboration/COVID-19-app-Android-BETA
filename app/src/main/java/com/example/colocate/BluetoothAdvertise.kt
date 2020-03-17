package com.example.colocate


import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid

fun advertiseData(): AdvertiseData {
    return AdvertiseData.Builder()
        .addServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .setIncludeDeviceName(false)
        .setIncludeTxPowerLevel(true)
        .build()
}

fun advertiseSettings(): AdvertiseSettings {
    return AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        .setConnectable(false)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()
}