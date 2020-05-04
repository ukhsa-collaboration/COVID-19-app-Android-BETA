/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import uk.nhs.nhsx.sonar.android.app.BuildConfig
import java.util.UUID

val NOTIFY_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
val SONAR_SERVICE_UUID: UUID = UUID.fromString(BuildConfig.SONAR_SERVICE_UUID)
val SONAR_KEEPALIVE_CHARACTERISTIC_UUID: UUID = UUID.fromString(BuildConfig.SONAR_IDENTITY_CHARACTERISTIC_UUID)
val SONAR_IDENTITY_CHARACTERISTIC_UUID: UUID = UUID.fromString(BuildConfig.SONAR_KEEP_ALIVE_CHARACTERISTIC_UUID)

fun BluetoothGattCharacteristic.isDeviceIdentifier(): Boolean =
    this.uuid == SONAR_IDENTITY_CHARACTERISTIC_UUID

fun BluetoothGattCharacteristic.isKeepAlive(): Boolean =
    this.uuid == SONAR_KEEPALIVE_CHARACTERISTIC_UUID

fun BluetoothGattDescriptor.isNotifyDescriptor(): Boolean =
    this.uuid == NOTIFY_DESCRIPTOR_UUID
