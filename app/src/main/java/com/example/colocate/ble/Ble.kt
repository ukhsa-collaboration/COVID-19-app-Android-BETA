/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID

val COLOCATE_SERVICE_UUID: UUID = UUID.fromString("c1f5983c-fa94-4ac8-8e2e-bb86d6de9b21")
val DEVICE_CHARACTERISTIC_UUID: UUID = UUID.fromString("85BF337C-5B64-48EB-A5F7-A9FED135C972")

fun BluetoothGattCharacteristic.isDeviceIdentifier(): Boolean =
    this.uuid == DEVICE_CHARACTERISTIC_UUID
