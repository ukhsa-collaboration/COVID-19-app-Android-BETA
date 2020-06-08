/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE

class DeviceDetection(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val context: Context,
    private val simulateTablet: Boolean = false
) {

    // TODO: re-consider this condition after we understand how devices advertise
    // without supporting MultiAdvertisement feature (isMultipleAdvertisementSupported == false)
    // This allows for them to run without crashing but probably not advertise as expected.
    // See: https://stackoverflow.com/a/32096285/952041
    // Here is part of the previous device detection logic that blocked Huawei P Smart phone.
    // After commenting it out we are not sure if the phone's advertising works
    // with our Bluetooth scanning process:
    //  || (bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported)
    // TODO: We need a real device to test Bluetooth scanning if isMultipleAdvertisementSupported == false
    // TODO: We need analytics to identify number of devices that fall into this bucket
    // and if they scan correctly

    fun isUnsupported(): Boolean =
        bluetoothAdapter == null || !context.packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE) ||
            (bluetoothAdapter.isEnabled && bluetoothAdapter.bluetoothLeAdvertiser == null)

    fun isTablet(): Boolean =
        simulateTablet || context.smallestScreenWidth() >= TABLET_MINIMUM_SCREEN_WIDTH

    companion object {
        const val TABLET_MINIMUM_SCREEN_WIDTH = 600
    }
}
