package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE

class DeviceDetection(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val packageManager: PackageManager
) {

    fun isUnsupported(): Boolean =
        bluetoothAdapter == null || !packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE)
    // TODO: Re-enable this condition after we understand how devices that don't support
    // MultiAdvertisement feature (isMultipleAdvertisementSupported == false)
    // but still does not return a null for BluetoothLeAdvertiser, which allow them to
    // work without crashing but probably not scanning other devices as expected
    // See: https://stackoverflow.com/a/32096285/952041
    //  || (bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported)
    // Another possible condition is that one:
    //  || (bluetoothAdapter.isEnabled && bluetoothAdapter.bluetoothLeAdvertiser == null)
}
