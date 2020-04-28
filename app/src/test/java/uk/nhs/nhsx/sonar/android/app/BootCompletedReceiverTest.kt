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
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

class BootCompletedReceiverTest {

    private val reminderManager = mockk<ReminderManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val intent = mockk<Intent>()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val applicationComponent = mockk<ApplicationComponent>(relaxed = true)
    private val sut = BootCompletedReceiver()

    @Before
    fun setUp() {
        every { context.appComponent } returns applicationComponent
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        mockkObject(BluetoothService)
        every { BluetoothService.start(context) } returns Unit

        sut.sonarIdProvider = sonarIdProvider
        sut.reminderManager = reminderManager
    }

    @Test
    fun onReceiveOtherIntentActionFinishes() {
        every { intent.action } returns "SOME_OTHER_ACTION"

        verifyAll {
            sonarIdProvider wasNot Called
            reminderManager wasNot Called
        }
    }

    @Test
    fun onReceiveNotifiesReminderManager() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        sut.onReceive(context, intent)

        verify(exactly = 1) { reminderManager.handleBootComplete() }
    }

    @Test
    fun onReceiveIfSonarIdIsPresentStartsBluetoothService() {
        every { sonarIdProvider.hasProperSonarId() } returns true

        sut.onReceive(context, intent)

        verify(exactly = 1) { BluetoothService.start(context) }
    }

    @Test
    fun onReceiveIfSonarIdIsAbsentDoesNotStartBluetoothService() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        sut.onReceive(context, intent)

        verify {
            BluetoothService wasNot Called
        }
    }
}
