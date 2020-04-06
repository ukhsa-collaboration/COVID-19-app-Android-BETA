package com.example.colocate.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.colocate.REQUEST_ENABLE_BT

fun isBluetoothEnabled() = BluetoothAdapter.getDefaultAdapter().isEnabled

fun AppCompatActivity.startBluetoothService() {
    ContextCompat.startForegroundService(this, Intent(this, BluetoothService::class.java))
}

fun AppCompatActivity.requestEnablingBluetooth() {
    if (!isBluetoothEnabled()) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }
}

enum class BluetoothResult {
    Enabled,
    Rejected,
    NotApplicable
}

fun checkBluetoothResult(requestCode: Int, resultCode: Int): BluetoothResult {
    if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK)
        return BluetoothResult.Enabled

    if (requestCode == REQUEST_ENABLE_BT)
        return BluetoothResult.Rejected

    return BluetoothResult.NotApplicable
}
