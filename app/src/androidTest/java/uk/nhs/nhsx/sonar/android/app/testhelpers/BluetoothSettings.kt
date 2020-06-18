/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import uk.nhs.nhsx.sonar.android.app.SonarApplication

class BluetoothSettings(private val app: SonarApplication) {

    private fun bluetoothAdapter(): BluetoothAdapter {
        val context = app.applicationContext
        val manager = context.getSystemService(BluetoothManager::class.java) as BluetoothManager
        return manager.adapter
    }

    fun ensureBluetoothEnabled() {
        bluetoothAdapter().let {
            it.enable()
            await until { it.isEnabled }
        }
    }

    fun ensureBluetoothDisabled() {
        bluetoothAdapter().let {
            it.disable()
            await until { !it.isEnabled }
        }
    }

    fun verifyBluetoothIsEnabled() {
        bluetoothAdapter().let {
            await until { it.isEnabled }
        }
    }
}
