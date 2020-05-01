package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE

class DeviceDetection(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val packageManager: PackageManager
) {

    // TODO: re-consider this condition after we understand how devices advertise
    // without supporting MultiAdvertisement feature (isMultipleAdvertisementSupported == false)
    // This allows for them to run without crashing but probably not advertise as expected.
    // See: https://stackoverflow.com/a/32096285/952041
    // Here is the previous logic:
    //  || (bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported)
    fun isUnsupported(): Boolean =
        bluetoothAdapter == null || !packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE) ||
            (bluetoothAdapter.isEnabled && bluetoothAdapter.bluetoothLeAdvertiser == null)
}
