/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdProvider

@ExperimentalCoroutinesApi
class GattServerTest {
    val coroutineScope = TestCoroutineScope()
    val context = mockk<Context>()
    val bluetoothManager = mockk<BluetoothManager>()
    val bluetoothIdProvider = mockk<BluetoothIdProvider>()

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
    }

    @Test
    fun `does not crash when server fails to open`() {
        assertThatCode {
            val server = GattServer(context, bluetoothManager, bluetoothIdProvider)
            every { bluetoothManager.openGattServer(context, any()) } returns null
            server.start(coroutineScope)
        }.doesNotThrowAnyException()
    }
}
