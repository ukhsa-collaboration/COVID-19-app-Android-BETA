package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE

class DeviceDetection(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val packageManager: PackageManager
) {

    fun isUnsupported(): Boolean =
        bluetoothAdapter == null || !packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE) ||
            (bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported)
}
