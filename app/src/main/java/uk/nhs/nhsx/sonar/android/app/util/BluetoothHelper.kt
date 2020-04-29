package uk.nhs.nhsx.sonar.android.app.util

import android.bluetooth.BluetoothAdapter

fun isBluetoothEnabled() = BluetoothAdapter.getDefaultAdapter().isEnabled
fun isBluetoothDisabled() = !isBluetoothEnabled()
