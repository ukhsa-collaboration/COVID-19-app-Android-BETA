/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verifyAll
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import testsupport.TestIntent
import testsupport.mockContextWithMockedAppComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class BootCompletedReceiverTest {

    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val stateStorage = mockk<UserStateStorage>()
    private val reminders = mockk<Reminders>()
    private val context = mockContextWithMockedAppComponent()

    private val receiver = BootCompletedReceiver().also {
        it.sonarIdProvider = sonarIdProvider
        it.userStateStorage = stateStorage
        it.reminders = reminders
    }

    @Before
    fun setUp() {
        mockkObject(BluetoothService)
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.onReceive(context, intent)

        verifyAll {
            sonarIdProvider wasNot Called
            BluetoothService wasNot Called
        }
    }

    @Test
    fun `onReceive - with sonarId, with not expired red state`() {
        val until = DateTime.now().plusDays(1)

        every { sonarIdProvider.hasProperSonarId() } returns true
        every { stateStorage.get() } returns RedState(until, until, nonEmptySetOf(Symptom.COUGH))
        every { reminders.scheduleCheckInReminder(any()) } returns Unit
        every { BluetoothService.start(any()) } returns Unit

        receiver.onReceive(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService.start(context)
            reminders.scheduleCheckInReminder(until)
        }
    }

    @Test
    fun `onReceive - with sonarId, with expired red state`() {
        val until = DateTime.now().minusDays(1)

        every { sonarIdProvider.hasProperSonarId() } returns true
        every { stateStorage.get() } returns RedState(until, until, nonEmptySetOf(Symptom.COUGH))
        every { BluetoothService.start(any()) } returns Unit

        receiver.onReceive(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService.start(context)
            reminders wasNot Called
        }
    }

    @Test
    fun `onReceive - without sonarId`() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { stateStorage.get() } returns RedState(DateTime.now(), DateTime.now(), nonEmptySetOf(Symptom.COUGH))
        every { reminders.scheduleCheckInReminder(any()) } returns Unit

        receiver.onReceive(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService wasNot Called
        }
    }

    @Test
    fun `onReceive - without red state`() {
        every { stateStorage.get() } returns ExposedState(DateTime.now(), DateTime.now())
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { BluetoothService.start(any()) } returns Unit

        receiver.onReceive(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            reminders wasNot Called
        }
    }
}
