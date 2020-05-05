/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.util.smallestScreenWidth

class DeviceDetectionTest {

    private val adapter = mockk<BluetoothAdapter>()
    private val context = mockk<Context>()
    private val manager = mockk<PackageManager>()

    @Before
    fun setUp() {
        every { context.packageManager } returns manager
        mockkStatic("uk.nhs.nhsx.sonar.android.app.util.AccessibilityHelperKt")
    }

    @Test
    fun `test isUnsupported()`() {
        every { adapter.isEnabled } returns false

        every { manager.hasSystemFeature(FEATURE_BLUETOOTH_LE) } returns true

        assertThat(DeviceDetection(adapter, context).isUnsupported()).isFalse()
    }

    @Test
    fun `test isUnsupported() when bluetooth adapter is null`() {
        assertThat(DeviceDetection(null, context).isUnsupported()).isTrue()
    }

    @Test
    fun `test isUnsupported() when bluetooth low energy is not supported`() {
        every { manager.hasSystemFeature(FEATURE_BLUETOOTH_LE) } returns false

        assertThat(DeviceDetection(adapter, context).isUnsupported()).isTrue()
    }

    @Test
    fun `test isUnsupported() when bluetooth is enabled and bluetoothLeAdvertiser is null`() {
        every { manager.hasSystemFeature(FEATURE_BLUETOOTH_LE) } returns true

        every { adapter.isEnabled } returns true
        every { adapter.bluetoothLeAdvertiser } returns null

        assertThat(DeviceDetection(adapter, context).isUnsupported()).isTrue()
    }

    @Test
    fun `test isTablet() when device has a small screen width`() {
        every { context.smallestScreenWidth() } returns 300

        assertThat(DeviceDetection(adapter, context).isTablet()).isFalse()
    }

    @Test
    fun `test isTablet() when device has a large screen width`() {
        every { context.smallestScreenWidth() } returns 600

        assertThat(DeviceDetection(adapter, context).isTablet()).isTrue()
    }
}
