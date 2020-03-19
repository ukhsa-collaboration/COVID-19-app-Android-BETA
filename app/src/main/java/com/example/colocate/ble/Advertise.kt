package com.example.colocate.ble


import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log

class Advertise(private val bluetoothLeAdvertiser: BluetoothLeAdvertiser) {
    private val advertiseData: AdvertiseData =
        AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true)
            .build()

    private val advertiseSettings: AdvertiseSettings =
        AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

    private val advertisingCallback = AdvertisingCallback()

    fun start() {
        bluetoothLeAdvertiser.startAdvertising(
            advertiseSettings,
            advertiseData,
            advertisingCallback
        )
    }

    fun stop() {
        bluetoothLeAdvertiser.stopAdvertising(advertisingCallback)
    }
}

private class AdvertisingCallback : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
        Log.i(
            "Advertising",
            "Started advertising with settings ${settingsInEffect.toString()}"
        )
    }

    override fun onStartFailure(errorCode: Int) {
        Log.e(
            "Advertising",
            "Failed to start with error code $errorCode"
        )
    }
}
