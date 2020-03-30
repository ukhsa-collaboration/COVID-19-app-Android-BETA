package com.example.colocate.ble.util

import android.bluetooth.BluetoothAdapter

fun isBluetoothEnabled() = BluetoothAdapter.getDefaultAdapter().isEnabled
