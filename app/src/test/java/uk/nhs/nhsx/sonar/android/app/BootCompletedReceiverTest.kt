/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

class BootCompletedReceiverTest {

    private val sonarIdProvider = mockk<SonarIdProvider>()

    private val receiver = BootCompletedReceiver().also {
        it.sonarIdProvider = sonarIdProvider
    }

    @Before
    fun setUp() {
        mockkObject(BluetoothService)
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.handle(mockk(), intent)

        verifyAll {
            sonarIdProvider wasNot Called
            BluetoothService wasNot Called
        }
    }

    @Test
    fun `onReceive - with boot intent action, and missing sonarId`() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        receiver.handle(mockk(), TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService wasNot Called
        }
    }

    @Test
    fun `onReceive - with boot intent action, and proper sonarId`() {
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { BluetoothService.start(any()) } returns Unit

        val context = mockk<Context>()
        receiver.handle(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService.start(context)
        }
    }

    class TestIntent(private val actionName: String) : Intent() {
        override fun getAction() = actionName
    }
}
