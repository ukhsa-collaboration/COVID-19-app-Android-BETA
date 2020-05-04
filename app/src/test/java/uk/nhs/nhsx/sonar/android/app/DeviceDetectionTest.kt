/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeviceDetectionTest {

    @Test
    fun `test isUnsupported()`() {
        val adapter = mockk<BluetoothAdapter>()
        val manager = mockk<PackageManager>()

        every { adapter.isEnabled } returns false

        every { manager.hasSystemFeature(FEATURE_BLUETOOTH_LE) } returns true

        assertThat(DeviceDetection(adapter, manager).isUnsupported()).isFalse()
    }

    @Test
    fun `test isUnsupported() when bluetooth adapter is null`() {
        val manager = mockk<PackageManager>()

        assertThat(DeviceDetection(null, manager).isUnsupported()).isTrue()
    }

    @Test
    fun `test isUnsupported() when bluetooth low energy is not supported`() {
        val adapter = mockk<BluetoothAdapter>()
        val manager = mockk<PackageManager>()

        every { manager.hasSystemFeature(FEATURE_BLUETOOTH_LE) } returns false

        assertThat(DeviceDetection(adapter, manager).isUnsupported()).isTrue()
    }

    @Test
    fun `test isUnsupported() when bluetooth is enabled and bluetoothLeAdvertiser is null`() {
        val adapter = mockk<BluetoothAdapter>()
        val manager = mockk<PackageManager>()

        every { manager.hasSystemFeature(FEATURE_BLUETOOTH_LE) } returns true

        every { adapter.isEnabled } returns true
        every { adapter.bluetoothLeAdvertiser } returns null

        assertThat(DeviceDetection(adapter, manager).isUnsupported()).isTrue()
    }
}
